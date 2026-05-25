package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceTokenUnregisterApiRequest(
        @NotBlank @Size(max = 4096) String token
) {
}
