package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.DevicePlatform;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceTokenView(
        UUID id,
        UUID userId,
        DevicePlatform platform,
        String deviceId,
        String appVersion,
        boolean enabled,
        OffsetDateTime lastSeenAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
