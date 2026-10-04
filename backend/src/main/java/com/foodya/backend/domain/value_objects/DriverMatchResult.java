package com.foodya.backend.domain.value_objects;

import java.util.UUID;

/**
 * Result value object returned by the driver matching algorithm.
 * Contains the matched driver and scoring details.
 */
public record DriverMatchResult(
        UUID driverUserId,
        UUID sessionId,
        double distanceKm,
        double score
) {
}
