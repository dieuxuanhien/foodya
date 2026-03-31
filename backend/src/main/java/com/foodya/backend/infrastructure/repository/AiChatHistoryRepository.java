package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.entities.AiChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AiChatHistoryRepository extends JpaRepository<AiChatHistory, UUID> {

    List<AiChatHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
