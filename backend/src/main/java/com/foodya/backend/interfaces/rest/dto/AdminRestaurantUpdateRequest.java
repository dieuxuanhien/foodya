package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.value_objects.RestaurantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminRestaurantUpdateRequest(
        @NotNull UUID ownerUserId,
        @NotBlank String name,
        String description,
        @NotBlank String addressLine,
        @NotBlank String cuisineType,
        boolean isOpen,
        @NotNull RestaurantStatus status,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @NotNull @DecimalMin("0.0") BigDecimal maxDeliveryKm
) {
}
