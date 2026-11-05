package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.value_objects.DriverMatchRequest;
import com.foodya.backend.domain.value_objects.DriverMatchResult;

import java.util.Optional;

/**
 * Outbound port for intelligent driver matching.
 * Finds the best available driver for an order using H3 k-ring expansion and scoring.
 */
public interface DriverMatchingPort {

    /**
     * Finds the best available driver for the given order request.
     * Uses H3 k-ring expansion from the restaurant location, then scores candidates
     * by proximity, capacity, and freshness.
     *
     * @param request the matching request with restaurant/customer coordinates
     * @return the best driver match, or empty if no driver is available
     */
    Optional<DriverMatchResult> findBestDriver(DriverMatchRequest request);
}
