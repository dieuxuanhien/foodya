package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.NotificationLogView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.value_objects.UserRole;

import java.util.UUID;

public interface NotificationUseCase {

    NotificationLogView notifyUser(UUID receiverUserId, UserRole receiverRole, String eventType, String title, String message, UUID orderId);

    PaginatedResult<NotificationLogView> list(Integer page, Integer size);

    PaginatedResult<NotificationLogView> listForUser(UUID receiverUserId, Integer page, Integer size);

    NotificationLogView markAsRead(UUID receiverUserId, UUID notificationId);
}
