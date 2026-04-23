package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.NotificationReceiverType;
import com.foodya.backend.domain.value_objects.NotificationStatus;

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
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {
}
