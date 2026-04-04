package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.AiChatHistoryPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AiChatHistoryRepository extends JpaRepository<AiChatHistoryPersistenceModel, UUID> {

    List<AiChatHistoryPersistenceModel> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
