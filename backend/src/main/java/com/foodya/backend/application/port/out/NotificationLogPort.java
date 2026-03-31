package com.foodya.backend.application.port.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.persistence.NotificationLog;

public interface NotificationLogPort {

    NotificationLog save(NotificationLog notificationLog);

    PaginatedResult<NotificationLog> list(int page, int size);
}
