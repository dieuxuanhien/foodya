package com.foodya.backend.application.dto;

import com.foodya.backend.domain.model.NotificationReceiverType;
import com.foodya.backend.domain.model.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationLogView(
        UUID id,
        UUID receiverUserId,
        NotificationReceiverType receiverType,
        String eventType,
        String title,
        String message,
        NotificationStatus status,
        UUID orderId,
        OffsetDateTime sentAt,
        OffsetDateTime createdAt
) {
}
