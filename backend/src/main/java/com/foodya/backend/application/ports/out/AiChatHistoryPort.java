package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.AiChatHistoryModel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AiChatHistoryPort {

    AiChatHistoryModel save(AiChatHistoryModel chatHistory);

    List<AiChatHistoryModel> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
