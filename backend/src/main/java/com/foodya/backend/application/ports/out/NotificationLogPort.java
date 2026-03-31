package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.dto.NotificationLogModel;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationLogPort {

    NotificationLogModel save(NotificationLogModel notificationLog);

    PaginatedResult<NotificationLogModel> list(int page, int size);

    PaginatedResult<NotificationLogModel> listByReceiver(UUID receiverUserId, int page, int size);

    Optional<NotificationLogModel> markAsRead(UUID receiverUserId, UUID notificationId, OffsetDateTime readAt);
}
