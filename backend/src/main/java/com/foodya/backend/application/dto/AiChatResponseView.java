package com.foodya.backend.application.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AiChatResponseView(
        UUID chatId,
        String prompt,
        String responseSummary,
        List<AiRecommendationItemView> recommendations,
        OffsetDateTime createdAt
) {
}
