package com.foodya.backend.interfaces.rest.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AiChatResponse(
        UUID chatId,
        String prompt,
        String responseSummary,
        List<AiRecommendationItemResponse> recommendations,
        OffsetDateTime createdAt
) {
}
