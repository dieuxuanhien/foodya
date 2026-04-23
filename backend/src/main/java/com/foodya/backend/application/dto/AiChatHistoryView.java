package com.foodya.backend.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AiChatHistoryView(
        UUID chatId,
        String prompt,
        String responseSummary,
        OffsetDateTime createdAt
) {
}
