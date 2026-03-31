package com.foodya.backend.interfaces.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AiChatHistoryResponse(
        UUID chatId,
        String prompt,
        String responseSummary,
        OffsetDateTime createdAt
) {
}
