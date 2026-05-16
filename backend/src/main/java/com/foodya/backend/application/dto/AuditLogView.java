package com.foodya.backend.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogView(
        UUID id,
        String actorUserId,
        String action,
        String targetType,
        String targetId,
        String oldValue,
        String newValue,
        OffsetDateTime createdAt
) {
}
