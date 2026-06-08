package com.foodya.backend.interfaces.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceTokenResponse(
        UUID id,
        UUID userId,
        String platform,
        String deviceId,
        String appVersion,
        boolean enabled,
        OffsetDateTime lastSeenAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
