package com.foodya.backend.application.event;

import com.foodya.backend.domain.model.OrderStatus;

import java.util.UUID;

public record OrderNotificationEvent(
        UUID orderId,
        String orderCode,
        UUID customerUserId,
        UUID merchantUserId,
        OrderStatus status,
        String eventType,
        String title,
        String message
) {
}
