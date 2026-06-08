package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.AiChatHistoryData;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AiChatHistoryPort {

    AiChatHistoryData save(AiChatHistoryData chatHistory);

    List<AiChatHistoryData> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long deleteByUserId(UUID userId);

    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
