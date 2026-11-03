package com.foodya.backend.domain.value_objects;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Everything a fee modifier may need. Immutable — modifiers return a new breakdown rather than mutating.
 *
 * @param routeDistanceKm      actual Goong route distance (not straight-line)
 * @param weather              current conditions at the restaurant's H3 cell, or null when unavailable
 * @param orderTime            local time at checkout, used for surge windows
 * @param demandSupplyRatio    eligible orders per available driver near the restaurant (0 when unknown)
 */
public record DeliveryFeeContext(
        BigDecimal routeDistanceKm,
        WeatherConditions weather,
        LocalDateTime orderTime,
        double demandSupplyRatio,
        double restaurantLat,
        double restaurantLng
) {
    public DeliveryFeeContext {
        if (routeDistanceKm == null || routeDistanceKm.signum() < 0) {
            throw new IllegalArgumentException("routeDistanceKm must be >= 0");
        }
    }
}
