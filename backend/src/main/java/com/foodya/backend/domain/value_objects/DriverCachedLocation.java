package com.foodya.backend.domain.value_objects;

import java.time.Instant;
import java.util.UUID;

/** Hot-path snapshot of a driver's last known position, as held in the shared location cache. */
public record DriverCachedLocation(
        UUID driverUserId,
        double lat,
        double lng,
        String h3Res9,
        Instant updatedAt
) {
    public boolean isStale(Instant now, long maxAgeSeconds) {
        return updatedAt == null || updatedAt.plusSeconds(maxAgeSeconds).isBefore(now);
    }
}
