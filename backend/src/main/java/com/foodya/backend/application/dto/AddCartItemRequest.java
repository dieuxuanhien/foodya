package com.foodya.backend.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCartItemRequest(
        @NotBlank String menuItemId,
        @Min(1) int quantity,
        @Size(max = 255)
        String note
) {
}
