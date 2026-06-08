package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.DevicePlatform;

public record RegisterDeviceTokenCommand(
        String token,
        DevicePlatform platform,
        String deviceId,
        String appVersion
) {
}
