package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OrderStatusUpdateApiRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z_]+$", message = "must be uppercase enum format")
        String status
) {
}
