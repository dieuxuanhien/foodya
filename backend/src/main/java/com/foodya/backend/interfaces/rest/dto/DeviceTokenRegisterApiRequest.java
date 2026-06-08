package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceTokenRegisterApiRequest(
        @NotBlank @Size(max = 4096) String token,
        @Size(max = 32) String platform,
        @Size(max = 128) String deviceId,
        @Size(max = 64) String appVersion
) {
}
