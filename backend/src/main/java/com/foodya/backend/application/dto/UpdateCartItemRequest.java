package com.foodya.backend.application.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(1) int quantity,
        String note
) {
}
