package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.NotificationLogPersistenceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLogPersistenceModel, UUID> {

	Page<NotificationLogPersistenceModel> findByReceiverUserId(UUID receiverUserId, Pageable pageable);

	Optional<NotificationLogPersistenceModel> findByIdAndReceiverUserId(UUID id, UUID receiverUserId);
}
