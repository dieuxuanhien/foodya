package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.NotificationLogData;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationLogPort {

    NotificationLogData save(NotificationLogData notificationLog);

    PaginatedResult<NotificationLogData> list(int page, int size);

    PaginatedResult<NotificationLogData> listByReceiver(UUID receiverUserId, int page, int size);

    Optional<NotificationLogData> markAsRead(UUID receiverUserId, UUID notificationId, OffsetDateTime readAt);
}
