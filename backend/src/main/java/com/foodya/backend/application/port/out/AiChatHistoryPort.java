package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.AiChatHistory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AiChatHistoryPort {

    AiChatHistory save(AiChatHistory chatHistory);

    List<AiChatHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
