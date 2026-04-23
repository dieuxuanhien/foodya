package com.foodya.backend.interfaces.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationLogResponse(
        UUID id,
        UUID receiverUserId,
        String receiverType,
        String eventType,
        String title,
        String message,
        String status,
        UUID orderId,
        OffsetDateTime sentAt,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {
}
