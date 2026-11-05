package com.foodya.backend.domain.value_objects;

/**
 * Result of a distance matrix API call — both time and distance matrices.
 */
public record DistanceMatrixResult(
        long[][] timeMatrix,     // seconds between node pairs
        long[][] distanceMatrix  // meters between node pairs
) {}
