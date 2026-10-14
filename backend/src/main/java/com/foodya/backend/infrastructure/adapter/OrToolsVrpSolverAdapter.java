package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.VrpSolverPort;
import com.foodya.backend.domain.value_objects.*;
import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Adapter implementing VrpSolverPort using Google OR-Tools RoutingModel.
 * Solves the VRPPD-TW (Vehicle Routing Problem with Pickup and Delivery and Time Windows).
 */
@Component
public class OrToolsVrpSolverAdapter implements VrpSolverPort {

    private static final Logger log = LoggerFactory.getLogger(OrToolsVrpSolverAdapter.class);

    static {
        try {
            Loader.loadNativeLibraries();
        } catch (Exception ex) {
            LoggerFactory.getLogger(OrToolsVrpSolverAdapter.class)
                    .error("Failed to load OR-Tools native libraries", ex);
        }
    }

    @Override
    public Optional<VrpSolution> solve(VrpProblem problem) {
        long startMs = System.currentTimeMillis();

        int numNodes = problem.timeMatrix().length;
        int numVehicles = problem.drivers().size();

        if (numNodes == 0 || numVehicles == 0) {
            return Optional.empty();
        }

        int[] depots = problem.drivers().stream()
                .mapToInt(VrpDriver::depotNodeIndex).toArray();

        // RoutingIndexManager: maps between solver internal indices and node indices
        RoutingIndexManager manager = new RoutingIndexManager(
                numNodes, numVehicles, depots, depots);

        RoutingModel routing = new RoutingModel(manager);

        // 1. Transit callback (time-based arc costs)
        final int transitCbIndex = routing.registerTransitCallback((long fromIndex, long toIndex) -> {
            int fromNode = manager.indexToNode((int) fromIndex);
            int toNode = manager.indexToNode((int) toIndex);
            long transitTime = problem.timeMatrix()[fromNode][toNode];
            // Add service time at the 'from' node
            Long serviceTime = problem.serviceTimes().getOrDefault(fromNode, 0L);
            return transitTime + serviceTime;
        });
        routing.setArcCostEvaluatorOfAllVehicles(transitCbIndex);

        // 2. Time dimension
        routing.addDimension(transitCbIndex,
                1800,                    // max slack (wait time): 30 min
                problem.maxRouteTime(),  // max total route time
                false,                   // don't force start cumul to zero
                "Time");
        RoutingDimension timeDim = routing.getMutableDimension("Time");

        // 3. Pickup & Delivery constraints
        for (VrpOrder order : problem.orders()) {
            long pickupIdx = manager.nodeToIndex(order.pickupNodeIndex());
            long deliveryIdx = manager.nodeToIndex(order.deliveryNodeIndex());

            routing.addPickupAndDelivery(pickupIdx, deliveryIdx);

            // Same vehicle constraint
            routing.solver().addConstraint(
                    routing.solver().makeEquality(
                            routing.vehicleVar(pickupIdx),
                            routing.vehicleVar(deliveryIdx)));

            // Precedence: pickup before delivery
            routing.solver().addConstraint(
                    routing.solver().makeLessOrEqual(
                            timeDim.cumulVar(pickupIdx),
                            timeDim.cumulVar(deliveryIdx)));
        }

        // 4. Time windows
        for (var entry : problem.timeWindows().entrySet()) {
            long index = manager.nodeToIndex(entry.getKey());
            long[] tw = entry.getValue();
            timeDim.cumulVar(index).setRange(tw[0], tw[1]);
        }

        // 5. Capacity dimension
        int[] demands = new int[numNodes];
        for (VrpOrder order : problem.orders()) {
            demands[order.pickupNodeIndex()] = 1;
            demands[order.deliveryNodeIndex()] = -1;
        }
        final int demandCbIndex = routing.registerUnaryTransitCallback(
                (long fromIndex) -> demands[manager.indexToNode((int) fromIndex)]);
        long[] capacities = problem.drivers().stream()
                .mapToLong(VrpDriver::capacity).toArray();
        routing.addDimensionWithVehicleCapacity(
                demandCbIndex, 0, capacities, true, "Capacity");

        // 6. Allow dropping nodes (penalty-based) for infeasible orders
        long penalty = 100_000L;
        for (VrpOrder order : problem.orders()) {
            routing.addDisjunction(
                    new long[]{
                            manager.nodeToIndex(order.pickupNodeIndex()),
                            manager.nodeToIndex(order.deliveryNodeIndex())
                    },
                    penalty);
        }

        // 7. Search parameters
        RoutingSearchParameters searchParams =
                main.defaultRoutingSearchParameters().toBuilder()
                        .setFirstSolutionStrategy(
                                FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                        .setLocalSearchMetaheuristic(
                                LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                        .setTimeLimit(com.google.protobuf.Duration.newBuilder()
                                .setSeconds(problem.solverTimeLimitMs() / 1000)
                                .setNanos((int) ((problem.solverTimeLimitMs() % 1000) * 1_000_000))
                                .build())
                        .build();

        // 8. Solve
        Assignment solution = routing.solveWithParameters(searchParams);
        long solverTimeMs = System.currentTimeMillis() - startMs;

        if (solution == null) {
            log.warn("OR-Tools solver returned no solution after {}ms", solverTimeMs);
            return Optional.empty();
        }

        // 9. Extract solution
        return Optional.of(extractSolution(routing, manager, solution, problem, solverTimeMs));
    }

    private VrpSolution extractSolution(RoutingModel routing, RoutingIndexManager manager,
                                          Assignment solution, VrpProblem problem, long solverTimeMs) {
        List<VrpRoute> routes = new ArrayList<>();
        long totalDistance = 0;
        long totalTime = 0;

        // Build orderId lookup by node index
        Map<Integer, UUID> pickupNodeToOrder = new HashMap<>();
        Map<Integer, UUID> deliveryNodeToOrder = new HashMap<>();
        for (VrpOrder order : problem.orders()) {
            pickupNodeToOrder.put(order.pickupNodeIndex(), order.orderId());
            deliveryNodeToOrder.put(order.deliveryNodeIndex(), order.orderId());
        }

        for (int v = 0; v < problem.drivers().size(); v++) {
            List<VrpRouteStop> stops = new ArrayList<>();
            long routeDistance = 0;
            long routeDuration = 0;

            long index = routing.start(v);
            while (!routing.isEnd(index)) {
                long nextIndex = solution.value(routing.nextVar(index));
                int nodeIdx = manager.indexToNode((int) index);

                // Skip depot nodes
                if (pickupNodeToOrder.containsKey(nodeIdx)) {
                    UUID orderId = pickupNodeToOrder.get(nodeIdx);
                    long arrivalTime = solution.value(
                            routing.getMutableDimension("Time").cumulVar(index));
                    VrpOrder vrpOrder = problem.orders().stream()
                            .filter(o -> o.orderId().equals(orderId))
                            .findFirst().orElse(null);
                    stops.add(new VrpRouteStop(
                            orderId, VrpRouteStop.StopType.PICKUP, nodeIdx,
                            0, 0, // lat/lng filled later by service
                            arrivalTime,
                            vrpOrder != null ? vrpOrder.pickupServiceTime() : 300));
                } else if (deliveryNodeToOrder.containsKey(nodeIdx)) {
                    UUID orderId = deliveryNodeToOrder.get(nodeIdx);
                    long arrivalTime = solution.value(
                            routing.getMutableDimension("Time").cumulVar(index));
                    VrpOrder vrpOrder = problem.orders().stream()
                            .filter(o -> o.orderId().equals(orderId))
                            .findFirst().orElse(null);
                    stops.add(new VrpRouteStop(
                            orderId, VrpRouteStop.StopType.DELIVERY, nodeIdx,
                            0, 0,
                            arrivalTime,
                            vrpOrder != null ? vrpOrder.deliveryServiceTime() : 180));
                }

                int fromNode = manager.indexToNode((int) index);
                int toNode = manager.indexToNode((int) nextIndex);
                routeDistance += problem.distanceMatrix()[fromNode][toNode];
                routeDuration += problem.timeMatrix()[fromNode][toNode];

                index = nextIndex;
            }

            if (!stops.isEmpty()) {
                VrpDriver driver = problem.drivers().get(v);
                routes.add(new VrpRoute(
                        driver.driverUserId(), driver.sessionId(),
                        stops, routeDistance, routeDuration));
                totalDistance += routeDistance;
                totalTime += routeDuration;
            }
        }

        long objectiveValue = solution.objectiveValue();
        log.info("OR-Tools solved: {} routes, {} total distance, {}ms",
                routes.size(), totalDistance, solverTimeMs);

        return new VrpSolution(routes, totalDistance, totalTime, objectiveValue, solverTimeMs);
    }
}
