package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.application.dto.UpdateMenuItemAvailabilityRequest;
import jakarta.validation.constraints.NotNull;

public record UpdateMenuItemAvailabilityApiRequest(
        @NotNull Boolean isAvailable
) {
    public UpdateMenuItemAvailabilityRequest toApplicationDto() {
        return new UpdateMenuItemAvailabilityRequest(isAvailable);
    }
}
