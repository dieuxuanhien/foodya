package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.entities.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

	Page<NotificationLog> findByReceiverUserId(UUID receiverUserId, Pageable pageable);

	Optional<NotificationLog> findByIdAndReceiverUserId(UUID id, UUID receiverUserId);
}
