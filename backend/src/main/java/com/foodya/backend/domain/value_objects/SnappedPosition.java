package com.foodya.backend.domain.value_objects;

/**
 * Result of projecting a raw GPS point onto a planned route.
 *
 * @param progress                 fraction of the route already covered (0.0–1.0)
 * @param remainingDistanceMeters  distance from the snapped point to the route end, along the route
 * @param remainingDurationSeconds proportional remaining duration (route duration × remaining fraction)
 */
public record SnappedPosition(
        double lat,
        double lng,
        double progress,
        int remainingDistanceMeters,
        int remainingDurationSeconds
) {}
