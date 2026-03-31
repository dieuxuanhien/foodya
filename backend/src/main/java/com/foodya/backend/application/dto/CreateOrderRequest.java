package com.foodya.backend.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String restaurantId,
        @NotEmpty List<@Valid CreateOrderItemRequest> items,
        @NotBlank String deliveryAddress,
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal deliveryLatitude,
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal deliveryLongitude,
        String customerNote
) {
}
