package com.foodya.backend.domain.value_objects;

import java.util.UUID;

/**
 * A single stop (pickup or delivery) within a VRP route.
 */
public record VrpRouteStop(
        UUID orderId,
        StopType type,
        int nodeIndex,
        double lat,
        double lng,
        long estimatedArrivalSeconds,
        long serviceDurationSeconds
) {
    public enum StopType { PICKUP, DELIVERY }
}
