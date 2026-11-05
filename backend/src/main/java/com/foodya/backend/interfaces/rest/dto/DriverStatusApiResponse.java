package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.value_objects.DriverSessionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * API response for driver status/lifecycle endpoints.
 */
public record DriverStatusApiResponse(
        UUID sessionId,
        DriverSessionStatus status,
        Instant startedAt,
        Instant endedAt,
        Instant lastHeartbeat,
        Double currentLat,
        Double currentLng,
        String h3Cell,
        int activeOrderCount,
        int maxConcurrentOrders,
        String message
) {
}
