package com.foodya.backend.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderItemRequest(
        @NotBlank String menuItemId,
        @Min(1) int quantity
) {
}
