package com.foodya.backend.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateRestaurantRequest(
        @NotBlank String name,
        @NotBlank String cuisineType,
        String description,
        @NotBlank String addressLine,
        @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
        @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal maxDeliveryKm,
        @NotNull Boolean isOpen
) {
}
