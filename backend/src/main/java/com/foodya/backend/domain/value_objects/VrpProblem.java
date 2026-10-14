package com.foodya.backend.domain.value_objects;

import java.util.List;
import java.util.Map;

/**
 * Input to the VRP solver: distance/time matrices, orders, drivers, constraints.
 */
public record VrpProblem(
        long[][] timeMatrix,
        long[][] distanceMatrix,
        List<VrpOrder> orders,
        List<VrpDriver> drivers,
        Map<Integer, long[]> timeWindows,
        Map<Integer, Long> serviceTimes,
        long maxRouteTime,
        long solverTimeLimitMs
) {}
