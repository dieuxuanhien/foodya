package com.foodya.backend.domain.value_objects;

import java.math.BigDecimal;

/**
 * Request value object for the driver matching algorithm.
 * Contains the geographic coordinates needed to find the best available driver.
 */
public record DriverMatchRequest(
        double restaurantLat,
        double restaurantLng,
        double customerLat,
        double customerLng,
        BigDecimal orderValue
) {
}
