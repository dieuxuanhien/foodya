package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.DriverSessionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Application-layer DTO representing the current state of a driver's online session.
 * Passed between the application use case and the REST controller.
 */
public record DriverSessionView(
        UUID sessionId,
        UUID driverUserId,
        DriverSessionStatus status,
        Instant startedAt,
        Instant endedAt,
        Instant lastHeartbeat,
        Double currentLat,
        Double currentLng,
        String h3IndexRes9,
        int activeOrderCount,
        int maxConcurrentOrders
) {

    /**
     * Convenience factory for an OFFLINE placeholder when the driver has no active session.
     */
    public static DriverSessionView offline(UUID driverUserId) {
        return new DriverSessionView(
                null,
                driverUserId,
                DriverSessionStatus.OFFLINE,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0
        );
    }
}
