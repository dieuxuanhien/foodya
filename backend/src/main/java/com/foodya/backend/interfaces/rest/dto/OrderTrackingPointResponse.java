package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderTrackingPointResponse(
        BigDecimal lat,
        BigDecimal lng,
        OffsetDateTime recordedAt,
        SnappedPosition snapped,
        RouteProgress route
) {
    public record SnappedPosition(double lat, double lng, double progress) {}

    public record RouteProgress(int remainingDistanceMeters, int remainingDurationSeconds,
                                OffsetDateTime estimatedArrival) {}
}
