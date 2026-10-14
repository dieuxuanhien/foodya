package com.foodya.backend.application.usecases;

import com.foodya.backend.application.event.OrderNotificationEvent;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.ports.in.OrderBatchingUseCase;
import com.foodya.backend.application.ports.out.*;
import com.foodya.backend.domain.entities.*;
import com.foodya.backend.domain.value_objects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core orchestration service for order batching using VRPPD-TW.
 * Framework-free — no Spring annotations.
 */
public class OrderBatchingService implements OrderBatchingUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderBatchingService.class);

    private static final long DEFAULT_BATCH_COOLDOWN_SECONDS = 10;
    private static final long DEFAULT_MAX_WAIT_SECONDS = 120;
    private static final long DEFAULT_PICKUP_SERVICE_TIME = 300;   // 5 min
    private static final long DEFAULT_DELIVERY_SERVICE_TIME = 180; // 3 min
    private static final long DEFAULT_MAX_DELIVERY_TIME = 45 * 60; // 45 min
    private static final long DEFAULT_MAX_ROUTE_TIME = 90 * 60;    // 90 min
    private static final long DEFAULT_SOLVER_TIME_LIMIT = 3000;    // 3s
    private static final double DEFAULT_SEARCH_RADIUS_KM = 10.0;
    private static final int DEFAULT_MAX_BATCH_SIZE = 4;
    private static final long DEFAULT_MAX_DETOUR_MINUTES = 15;

    // Guards against overlapping batch cycles within a single JVM. @Scheduled
    // uses fixedDelay (serialized) so this is defense-in-depth; cross-instance
    // deployments still require a distributed lock.
    private final java.util.concurrent.locks.ReentrantLock cycleLock =
            new java.util.concurrent.locks.ReentrantLock();

    private final OrderManagementPort orderManagementPort;
    private final DriverOnlineSessionPort driverOnlineSessionPort;
    private final DistanceMatrixPort distanceMatrixPort;
    private final VrpSolverPort vrpSolverPort;
    private final OrderBatchPort orderBatchPort;
    private final GeoPort geoPort;
    private final OrderEventPublisherPort orderEventPublisherPort;
    private final SystemParameterPort systemParameterPort;
    private final DriverMatchingPort driverMatchingPort;
    private final RestaurantPort restaurantPort;

    public OrderBatchingService(OrderManagementPort orderManagementPort,
                                DriverOnlineSessionPort driverOnlineSessionPort,
                                DistanceMatrixPort distanceMatrixPort,
                                VrpSolverPort vrpSolverPort,
                                OrderBatchPort orderBatchPort,
                                GeoPort geoPort,
                                OrderEventPublisherPort orderEventPublisherPort,
                                SystemParameterPort systemParameterPort,
                                DriverMatchingPort driverMatchingPort,
                                RestaurantPort restaurantPort) {
        this.orderManagementPort = orderManagementPort;
        this.driverOnlineSessionPort = driverOnlineSessionPort;
        this.distanceMatrixPort = distanceMatrixPort;
        this.vrpSolverPort = vrpSolverPort;
        this.orderBatchPort = orderBatchPort;
        this.geoPort = geoPort;
        this.orderEventPublisherPort = orderEventPublisherPort;
        this.systemParameterPort = systemParameterPort;
        this.driverMatchingPort = driverMatchingPort;
        this.restaurantPort = restaurantPort;
    }

    @Override
    public BatchCycleResult triggerBatchCycle() {
        if (!isBatchingEnabled()) {
            log.debug("Batching disabled, skipping cycle");
            return BatchCycleResult.empty();
        }
        if (!cycleLock.tryLock()) {
            log.warn("Batch cycle already running, skipping this tick");
            return BatchCycleResult.empty();
        }
        try {
            return runBatchCycle();
        } finally {
            cycleLock.unlock();
        }
    }

    private BatchCycleResult runBatchCycle() {
        // 1. Collect eligible orders
        List<Order> allAccepted = orderManagementPort
                .findEligibleForBatching();

        if (allAccepted.isEmpty()) {
            return BatchCycleResult.empty();
        }

        long cooldownSeconds = getConfigLong("batching.cooldown_seconds", DEFAULT_BATCH_COOLDOWN_SECONDS);
        long maxWaitSeconds = getConfigLong("batching.max_wait_for_batch_seconds", DEFAULT_MAX_WAIT_SECONDS);
        Instant now = Instant.now();

        // Separate urgent orders (waited too long) from batchable orders
        List<Order> urgentOrders = new ArrayList<>();
        List<Order> eligibleOrders = new ArrayList<>();

        for (Order order : allAccepted) {
            if (order.getPlacedAt() == null) continue;
            long waitedSeconds = Duration.between(order.getPlacedAt().toInstant(), now).getSeconds();
            if (waitedSeconds > maxWaitSeconds) {
                urgentOrders.add(order);
            } else if (waitedSeconds >= cooldownSeconds) {
                eligibleOrders.add(order);
            }
            // Orders within cooldown period are skipped — wait for next cycle
        }

        int singleAssigned = 0;

        // Urgent orders: immediate single assignment
        for (Order order : urgentOrders) {
            if (trySingleAssign(order)) {
                singleAssigned++;
            }
        }

        // If only 1 eligible order, single assign
        if (eligibleOrders.size() <= 1) {
            for (Order order : eligibleOrders) {
                if (trySingleAssign(order)) {
                    singleAssigned++;
                }
            }
            return new BatchCycleResult(0, 0, singleAssigned, 0, 0);
        }

        // 2. Collect available drivers — expand a k-ring around every eligible
        //    restaurant so drivers near (not just inside) the restaurant cell are found.
        double searchRadius = getConfigDouble("batching.search_radius_km", DEFAULT_SEARCH_RADIUS_KM);
        Set<String> searchCells = collectSearchCells(eligibleOrders, searchRadius);

        List<DriverOnlineSession> drivers = driverOnlineSessionPort
                .findOnlineDriversInH3Cells(searchCells)
                .stream()
                .filter(DriverOnlineSession::canAcceptOrder)
                .toList();

        if (drivers.isEmpty()) {
            log.warn("No available drivers for {} eligible orders", eligibleOrders.size());
            return BatchCycleResult.noDrivers();
        }

        // Cap the problem size so the solver stays bounded: at most
        // max_batch_size orders per driver this cycle. Overflow is deferred to the
        // next cycle (or single-assigned once it becomes urgent).
        int maxBatchSize = (int) getConfigLong("batching.max_batch_size", DEFAULT_MAX_BATCH_SIZE);
        int maxOrdersThisCycle = Math.max(maxBatchSize, maxBatchSize * drivers.size());
        List<Order> overflowOrders = new ArrayList<>();
        if (eligibleOrders.size() > maxOrdersThisCycle) {
            overflowOrders.addAll(eligibleOrders.subList(maxOrdersThisCycle, eligibleOrders.size()));
            eligibleOrders = new ArrayList<>(eligibleOrders.subList(0, maxOrdersThisCycle));
            log.info("Capping batch cycle to {} orders; {} deferred to next cycle",
                    maxOrdersThisCycle, overflowOrders.size());
        }

        // 3. Build location list and distance matrix
        List<LatLng> locations = buildLocationList(drivers, eligibleOrders);
        DistanceMatrixResult matrix;
        try {
            matrix = distanceMatrixPort.getFullMatrix(locations, "bike");
        } catch (Exception ex) {
            log.error("Distance matrix fetch failed, falling back to single assignment", ex);
            for (Order order : eligibleOrders) {
                if (trySingleAssign(order)) singleAssigned++;
            }
            return new BatchCycleResult(0, 0, singleAssigned, 0, 0);
        }

        // 4. Build VRP problem
        VrpProblem problem = buildVrpProblem(matrix, eligibleOrders, drivers, locations);

        // 5. Solve
        Optional<VrpSolution> solution;
        try {
            solution = vrpSolverPort.solve(problem);
        } catch (Exception ex) {
            log.error("VRP solver failed, falling back to single assignment", ex);
            for (Order order : eligibleOrders) {
                if (trySingleAssign(order)) singleAssigned++;
            }
            return new BatchCycleResult(0, 0, singleAssigned, 0, 0);
        }

        if (solution.isEmpty()) {
            log.warn("VRP solver returned no solution, falling back to single assignment");
            for (Order order : eligibleOrders) {
                if (trySingleAssign(order)) singleAssigned++;
            }
            return new BatchCycleResult(0, 0, singleAssigned, 0, 0);
        }

        // 6. Persist batches and notify
        VrpSolution sol = solution.get();
        int batchesCreated = 0;
        int ordersAssigned = 0;
        Set<UUID> assignedOrderIds = new HashSet<>();

        for (VrpRoute route : sol.routes()) {
            if (route.stops().isEmpty()) continue;

            // Separate pickup and delivery stops, preserving solver route order
            List<VrpRouteStop> pickups = route.stops().stream()
                    .filter(s -> s.type() == VrpRouteStop.StopType.PICKUP)
                    .toList();
            List<VrpRouteStop> deliveries = route.stops().stream()
                    .filter(s -> s.type() == VrpRouteStop.StopType.DELIVERY)
                    .toList();

            if (pickups.isEmpty()) continue;

            // Precompute position-by-order maps once per route
            Map<UUID, Integer> pickupIndexByOrder = new HashMap<>();
            for (int i = 0; i < pickups.size(); i++) {
                pickupIndexByOrder.put(pickups.get(i).orderId(), i);
            }
            Map<UUID, VrpRouteStop> deliveryByOrder = new HashMap<>();
            Map<UUID, Integer> deliveryIndexByOrder = new HashMap<>();
            for (int i = 0; i < deliveries.size(); i++) {
                deliveryByOrder.put(deliveries.get(i).orderId(), deliveries.get(i));
                deliveryIndexByOrder.put(deliveries.get(i).orderId(), i);
            }

            // Build JSON sequences
            String pickupJson = buildSequenceJson(pickups);
            String deliveryJson = buildSequenceJson(deliveries);
            String solverMeta = String.format(
                    "{\"solver_time_ms\":%d,\"objective_value\":%d,\"num_orders\":%d}",
                    sol.solverTimeMs(), sol.objectiveValue(), pickups.size());

            OrderBatch batch = OrderBatch.create(
                    route.driverUserId(), route.sessionId(),
                    pickupJson, deliveryJson,
                    (int) route.routeDistanceMeters(), (int) route.routeDurationSeconds(),
                    solverMeta);
            batch = orderBatchPort.save(batch);

            // Create batch items and assign orders
            Instant batchCreatedAt = batch.getCreatedAt();
            int batchOrderCount = 0;
            for (VrpRouteStop pickup : pickups) {
                UUID orderId = pickup.orderId();
                VrpRouteStop delivery = deliveryByOrder.get(orderId);
                Integer pickupIdx = pickupIndexByOrder.get(orderId);
                Integer deliveryIdx = deliveryIndexByOrder.get(orderId);

                // Skip orders the solver left without a paired delivery stop
                if (delivery == null || pickupIdx == null || deliveryIdx == null) {
                    log.warn("Batch route for driver {} has pickup without delivery for order {}; skipping",
                            route.driverUserId(), orderId);
                    continue;
                }

                Instant estPickup = batchCreatedAt.plusSeconds(pickup.estimatedArrivalSeconds());
                Instant estDelivery = batchCreatedAt.plusSeconds(delivery.estimatedArrivalSeconds());

                BatchOrderItem item = BatchOrderItem.create(
                        batch.getId(), orderId,
                        pickupIdx, deliveryIdx,
                        estPickup, estDelivery);
                orderBatchPort.saveBatchItem(item);

                // Update order: assign driver and batch
                Order order = orderManagementPort.findById(orderId).orElse(null);
                if (order != null && order.getStatus() == OrderStatus.ACCEPTED) {
                    order.assignDriver(route.driverUserId());
                    order.setBatchId(batch.getId());
                    order.setBatchPickupIndex(pickupIdx);
                    order.setBatchDeliveryIndex(deliveryIdx);
                    order.setEstimatedDeliveryAt(OffsetDateTime.ofInstant(estDelivery, java.time.ZoneOffset.UTC));
                    orderManagementPort.save(order);
                    assignedOrderIds.add(orderId);
                    ordersAssigned++;
                    batchOrderCount++;

                    publishOrderEvent(order, "ORDER_ASSIGNED",
                            "Order " + order.getOrderCode() + " assigned to batch");
                }
            }

            // If no order in this route was actually assigned, drop the empty batch
            if (batchOrderCount == 0) {
                log.warn("Discarding batch {} — no orders could be assigned", batch.getId());
                batch.cancel();
                orderBatchPort.save(batch);
                continue;
            }

            // Update driver session
            final int assignedInBatch = batchOrderCount;
            driverOnlineSessionPort.findActiveSession(route.driverUserId())
                    .ifPresent(session -> {
                        for (int i = 0; i < assignedInBatch; i++) {
                            session.startDelivery();
                        }
                        driverOnlineSessionPort.save(session);
                    });

            batchesCreated++;
            log.info("Created batch {} with {} orders for driver {}",
                    batch.getId(), batchOrderCount, route.driverUserId());
        }

        // 7. Fallback: orders not assigned by solver → single assign.
        // Orders the solver dropped or that never made it into the batch are
        // single-assigned so they are not stuck at ACCEPTED.
        int droppedOrders = 0;
        for (Order order : eligibleOrders) {
            if (!assignedOrderIds.contains(order.getId())) {
                droppedOrders++;
                if (trySingleAssign(order)) {
                    singleAssigned++;
                }
            }
        }

        // Overflow orders were never considered this cycle: leave them ACCEPTED so
        // the next cycle batches them (they become urgent and single-assign if the
        // queue keeps growing).
        if (!overflowOrders.isEmpty()) {
            log.debug("{} orders deferred to next batch cycle", overflowOrders.size());
        }

        return new BatchCycleResult(batchesCreated, ordersAssigned, singleAssigned,
                droppedOrders, sol.solverTimeMs());
    }

    @Override
    public OrderBatch getActiveBatchForDriver(UUID driverUserId) {
        return orderBatchPort.findActiveBatchForDriver(driverUserId)
                .orElseThrow(() -> new NotFoundException("No active batch found for driver"));
    }

    @Override
    public OrderBatch getBatchDetail(UUID batchId) {
        return orderBatchPort.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found"));
    }

    @Override
    public List<BatchOrderItem> getBatchItems(UUID batchId) {
        return orderBatchPort.findBatchItemsByBatchId(batchId);
    }

    @Override
    public BatchOrderItem markPickupComplete(UUID batchId, UUID orderId, UUID driverUserId) {
        OrderBatch batch = getBatchDetail(batchId);
        if (!batch.getDriverUserId().equals(driverUserId)) {
            throw new ForbiddenException("Batch does not belong to this driver");
        }

        // Activate batch on first pickup
        if (batch.getStatus() == BatchStatus.PENDING) {
            batch.activate();
            orderBatchPort.save(batch);
        }

        BatchOrderItem item = orderBatchPort.findBatchItemByBatchIdAndOrderId(batchId, orderId)
                .orElseThrow(() -> new NotFoundException("Order not found in batch"));
        item.markPickedUp();
        orderBatchPort.saveBatchItem(item);

        // Advance the order state machine so it can later be completed. A batched
        // order is ASSIGNED after the solver; picking up moves it to DELIVERING.
        // Orders already PREPARING/DELIVERING (legacy path) are left untouched.
        orderManagementPort.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.ASSIGNED) {
                order.deliveryStart();
                orderManagementPort.save(order);
                publishOrderEvent(order, "ORDER_DELIVERING",
                        "Order " + order.getOrderCode() + " picked up and out for delivery");
            }
        });

        return item;
    }

    @Override
    public BatchOrderItem markDeliveryComplete(UUID batchId, UUID orderId, UUID driverUserId) {
        OrderBatch batch = getBatchDetail(batchId);
        if (!batch.getDriverUserId().equals(driverUserId)) {
            throw new ForbiddenException("Batch does not belong to this driver");
        }

        BatchOrderItem item = orderBatchPort.findBatchItemByBatchIdAndOrderId(batchId, orderId)
                .orElseThrow(() -> new NotFoundException("Order not found in batch"));
        item.markDelivered();
        orderBatchPort.saveBatchItem(item);

        // Transition order to SUCCESS. Accept DELIVERING (normal batch flow) or a
        // still-ASSIGNED order (driver skipped the pickup step) so a delivery is
        // never silently lost.
        orderManagementPort.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.DELIVERING) {
                order.deliveryFinish(OrderStatus.SUCCESS);
            } else if (order.getStatus() == OrderStatus.ASSIGNED) {
                order.deliveryStart();
                order.deliveryFinish(OrderStatus.SUCCESS);
            } else {
                log.warn("Order {} in status {} cannot be completed via batch delivery",
                        order.getOrderCode(), order.getStatus());
                return;
            }
            orderManagementPort.save(order);
            publishOrderEvent(order, "ORDER_SUCCESS",
                    "Order " + order.getOrderCode() + " delivered");
        });

        // Update driver session
        driverOnlineSessionPort.findActiveSession(driverUserId)
                .ifPresent(session -> {
                    session.completeDelivery();
                    driverOnlineSessionPort.save(session);
                });

        // Check if all items in batch are done
        List<BatchOrderItem> allItems = orderBatchPort.findBatchItemsByBatchId(batchId);
        boolean allDone = allItems.stream().allMatch(i ->
                i.getItemStatus() == BatchItemStatus.DELIVERED
                        || i.getItemStatus() == BatchItemStatus.FAILED);
        if (allDone) {
            boolean anyFailed = allItems.stream()
                    .anyMatch(i -> i.getItemStatus() == BatchItemStatus.FAILED);
            if (anyFailed) {
                batch.markPartial();
            } else {
                batch.complete();
            }
            orderBatchPort.save(batch);
        }

        return item;
    }

    @Override
    public CustomerBatchInfo getCustomerBatchInfo(UUID orderId, UUID customerUserId) {
        Order order = orderManagementPort.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!order.getCustomerUserId().equals(customerUserId)) {
            throw new ForbiddenException("Order does not belong to customer");
        }
        if (order.getBatchId() == null) {
            throw new NotFoundException("Order is not part of a batch");
        }

        OrderBatch batch = getBatchDetail(order.getBatchId());
        List<BatchOrderItem> items = orderBatchPort.findBatchItemsByBatchId(batch.getId());

        BatchOrderItem myItem = items.stream()
                .filter(i -> i.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Order not found in batch"));

        int totalOrders = items.size();
        int myPosition = myItem.getDeliveryIndex() + 1;
        Instant estimatedAt = myItem.getEstimatedDeliveryAt();
        int minutesRemaining = estimatedAt != null
                ? (int) Math.max(0, Duration.between(Instant.now(), estimatedAt).toMinutes())
                : 0;

        return new CustomerBatchInfo(batch.getId(), totalOrders, myPosition,
                estimatedAt, minutesRemaining);
    }

    @Override
    public List<OrderBatch> getAllBatches() {
        return orderBatchPort.findAllOrderByCreatedAtDesc();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isBatchingEnabled() {
        return systemParameterPort.findById("batching.enabled")
                .map(p -> "true".equalsIgnoreCase(p.getValue()))
                .orElse(true);
    }

    private long getConfigLong(String key, long defaultValue) {
        return systemParameterPort.findById(key)
                .map(p -> {
                    try { return Long.parseLong(p.getValue()); }
                    catch (NumberFormatException e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }

    private double getConfigDouble(String key, double defaultValue) {
        return systemParameterPort.findById(key)
                .map(p -> {
                    try { return Double.parseDouble(p.getValue()); }
                    catch (NumberFormatException e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }

    private boolean trySingleAssign(Order order) {
        try {
            Restaurant restaurant = restaurantPort.findById(order.getRestaurantId()).orElse(null);
            if (restaurant == null || restaurant.getLatitude() == null) return false;

            DriverMatchRequest req = new DriverMatchRequest(
                    restaurant.getLatitude().doubleValue(),
                    restaurant.getLongitude().doubleValue(),
                    order.getDeliveryLatitude() != null ? order.getDeliveryLatitude().doubleValue() : 0.0,
                    order.getDeliveryLongitude() != null ? order.getDeliveryLongitude().doubleValue() : 0.0,
                    order.getTotalAmount());

            Optional<DriverMatchResult> match = driverMatchingPort.findBestDriver(req);
            if (match.isEmpty()) return false;

            order.assignDriver(match.get().driverUserId());
            orderManagementPort.save(order);

            driverOnlineSessionPort.findActiveSession(match.get().driverUserId())
                    .ifPresent(session -> {
                        session.startDelivery();
                        driverOnlineSessionPort.save(session);
                    });

            publishOrderEvent(order, "ORDER_ASSIGNED",
                    "Driver assigned to order " + order.getOrderCode());
            return true;
        } catch (Exception ex) {
            log.error("Single assign failed for order {}", order.getOrderCode(), ex);
            return false;
        }
    }

    private Set<String> collectSearchCells(List<Order> orders, double radiusKm) {
        Set<String> cells = new HashSet<>();
        for (Order order : orders) {
            Restaurant restaurant = restaurantPort.findById(order.getRestaurantId()).orElse(null);
            if (restaurant != null && restaurant.getLatitude() != null) {
                cells.addAll(geoPort.h3KRingRes9(
                        restaurant.getLatitude().doubleValue(),
                        restaurant.getLongitude().doubleValue(),
                        radiusKm));
            }
        }
        return cells;
    }

    private List<LatLng> buildLocationList(List<DriverOnlineSession> drivers, List<Order> orders) {
        List<LatLng> locations = new ArrayList<>();

        // First: driver depot positions (indices 0..m-1)
        for (DriverOnlineSession driver : drivers) {
            locations.add(new LatLng(
                    driver.getCurrentLat() != null ? driver.getCurrentLat() : 0.0,
                    driver.getCurrentLng() != null ? driver.getCurrentLng() : 0.0));
        }

        // Then: for each order, pickup (restaurant) then delivery (customer)
        for (Order order : orders) {
            Restaurant restaurant = restaurantPort.findById(order.getRestaurantId()).orElse(null);
            double pickupLat = restaurant != null && restaurant.getLatitude() != null
                    ? restaurant.getLatitude().doubleValue() : 0.0;
            double pickupLng = restaurant != null && restaurant.getLongitude() != null
                    ? restaurant.getLongitude().doubleValue() : 0.0;
            locations.add(new LatLng(pickupLat, pickupLng));

            double deliveryLat = order.getDeliveryLatitude() != null
                    ? order.getDeliveryLatitude().doubleValue() : 0.0;
            double deliveryLng = order.getDeliveryLongitude() != null
                    ? order.getDeliveryLongitude().doubleValue() : 0.0;
            locations.add(new LatLng(deliveryLat, deliveryLng));
        }

        return locations;
    }

    private VrpProblem buildVrpProblem(DistanceMatrixResult matrix, List<Order> orders,
                                        List<DriverOnlineSession> drivers, List<LatLng> locations) {
        int numDrivers = drivers.size();
        long pickupServiceTime = getConfigLong("batching.pickup_service_time_seconds", DEFAULT_PICKUP_SERVICE_TIME);
        long deliveryServiceTime = getConfigLong("batching.delivery_service_time_seconds", DEFAULT_DELIVERY_SERVICE_TIME);
        long maxDeliveryTime = getConfigLong("batching.max_delivery_time_minutes", 45) * 60;
        long maxRouteTime = DEFAULT_MAX_ROUTE_TIME;
        long solverTimeLimit = getConfigLong("batching.solver_time_limit_ms", DEFAULT_SOLVER_TIME_LIMIT);

        List<VrpOrder> vrpOrders = new ArrayList<>();
        Map<Integer, long[]> timeWindows = new HashMap<>();
        Map<Integer, Long> serviceTimes = new HashMap<>();

        // Driver depot nodes: indices 0..numDrivers-1
        for (int i = 0; i < numDrivers; i++) {
            timeWindows.put(i, new long[]{0, maxRouteTime});
            serviceTimes.put(i, 0L);
        }

        // Order nodes: for order i, pickup = numDrivers + 2*i, delivery = numDrivers + 2*i + 1
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            int pickupIdx = numDrivers + 2 * i;
            int deliveryIdx = numDrivers + 2 * i + 1;

            long twEnd = maxDeliveryTime; // relative from route start

            vrpOrders.add(new VrpOrder(
                    order.getId(), pickupIdx, deliveryIdx,
                    0, twEnd,
                    pickupServiceTime, deliveryServiceTime));

            timeWindows.put(pickupIdx, new long[]{0, twEnd});
            timeWindows.put(deliveryIdx, new long[]{0, twEnd});
            serviceTimes.put(pickupIdx, pickupServiceTime);
            serviceTimes.put(deliveryIdx, deliveryServiceTime);
        }

        List<VrpDriver> vrpDrivers = new ArrayList<>();
        for (int i = 0; i < numDrivers; i++) {
            DriverOnlineSession d = drivers.get(i);
            vrpDrivers.add(new VrpDriver(
                    d.getDriverUserId(), d.getId(),
                    i,
                    d.getMaxConcurrentOrders() - d.getActiveOrderCount()));
        }

        return new VrpProblem(
                matrix.timeMatrix(), matrix.distanceMatrix(),
                vrpOrders, vrpDrivers,
                timeWindows, serviceTimes,
                maxRouteTime, solverTimeLimit);
    }

    private String buildSequenceJson(List<VrpRouteStop> stops) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < stops.size(); i++) {
            VrpRouteStop stop = stops.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format(
                    "{\"orderId\":\"%s\",\"lat\":%.7f,\"lng\":%.7f,\"estimatedArrivalSec\":%d,\"serviceTimeSec\":%d}",
                    stop.orderId(), stop.lat(), stop.lng(),
                    stop.estimatedArrivalSeconds(), stop.serviceDurationSeconds()));
        }
        sb.append("]");
        return sb.toString();
    }

    private void publishOrderEvent(Order order, String eventType, String message) {
        try {
            Restaurant restaurant = restaurantPort.findById(order.getRestaurantId()).orElse(null);
            UUID merchantUserId = restaurant != null ? restaurant.getOwnerUserId() : null;
            orderEventPublisherPort.publishOrderNotification(new OrderNotificationEvent(
                    order.getId(), order.getOrderCode(),
                    order.getCustomerUserId(), merchantUserId, order.getDriverUserId(),
                    order.getStatus(), eventType,
                    "Order update " + order.getOrderCode(), message));
        } catch (Exception ex) {
            log.warn("Failed to publish order event for {}: {}", order.getOrderCode(), ex.getMessage());
        }
    }
}
