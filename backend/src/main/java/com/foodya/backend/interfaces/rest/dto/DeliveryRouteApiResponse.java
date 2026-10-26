package com.foodya.backend.interfaces.rest.dto;

import java.time.Instant;
import java.util.List;

public record DeliveryRouteApiResponse(
        String encodedPolyline,
        int distanceMeters,
        int durationSeconds,
        List<Waypoint> waypoints,
        Instant calculatedAt
) {
    public record Waypoint(double lat, double lng, String type) {}
}
