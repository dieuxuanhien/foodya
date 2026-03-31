package com.foodya.backend.application.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMenuItemAvailabilityRequest(
        @NotNull Boolean isAvailable
) {
}
