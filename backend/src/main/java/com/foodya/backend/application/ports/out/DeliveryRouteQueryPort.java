package com.foodya.backend.application.ports.out;

import java.util.Optional;

/**
 * Minimal read view over the planned delivery route, used for live-tracking enrichment.
 */
public interface DeliveryRouteQueryPort {

    record PlannedRoute(String encodedPolyline, int distanceMeters, int durationSeconds) {}

    Optional<PlannedRoute> findActiveRouteForOrder(java.util.UUID orderId);
}
