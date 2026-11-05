package com.foodya.backend.domain.value_objects;

import java.util.List;
import java.util.UUID;

/**
 * A single route (for one driver) within a VRP solution.
 */
public record VrpRoute(
        UUID driverUserId,
        UUID sessionId,
        List<VrpRouteStop> stops,
        long routeDistanceMeters,
        long routeDurationSeconds
) {}
