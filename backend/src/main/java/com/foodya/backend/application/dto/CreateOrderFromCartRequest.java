package com.foodya.backend.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateOrderFromCartRequest(
        @NotBlank String deliveryAddress,
        @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal deliveryLatitude,
        @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal deliveryLongitude,
        @Size(max = 500)
        String customerNote
) {
}
