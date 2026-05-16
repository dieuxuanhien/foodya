package com.foodya.backend.interfaces.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String actorId,
        String eventType,
        String entityType,
        String entityId,
        String oldValue,
        String newValue,
        String context,
        OffsetDateTime createdAt
) {
}
