package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CancelOrderApiRequest(
        @Size(max = 255, message = "must be <= 255 characters")
        @Pattern(regexp = "^$|.*\\S.*", message = "must not be blank when provided")
        String reason
) {
}
