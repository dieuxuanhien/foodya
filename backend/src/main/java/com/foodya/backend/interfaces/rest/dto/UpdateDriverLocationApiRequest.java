package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for the PUT /api/v1/delivery/status/location endpoint.
 * Driver sends their current GPS coordinates.
 */
public record UpdateDriverLocationApiRequest(
        @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double lat,
        @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double lng
) {
}
