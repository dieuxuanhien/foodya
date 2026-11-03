package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Tracking point exposed to clients. Snapped/route fields are {@code null} when no planned route exists.
 */
public record OrderTrackingPointView(
        BigDecimal lat,
        BigDecimal lng,
        OffsetDateTime recordedAt,
        SnappedPositionView snapped,
        RouteProgressView route
) {
    public OrderTrackingPointView(BigDecimal lat, BigDecimal lng, OffsetDateTime recordedAt) {
        this(lat, lng, recordedAt, null, null);
    }

    public record SnappedPositionView(double lat, double lng, double progress) {}

    public record RouteProgressView(int remainingDistanceMeters, int remainingDurationSeconds,
                                    OffsetDateTime estimatedArrival) {}
}
