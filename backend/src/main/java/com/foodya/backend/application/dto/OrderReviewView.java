package com.foodya.backend.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderReviewView(
        UUID reviewId,
        UUID orderId,
        UUID restaurantId,
        UUID customerUserId,
        int stars,
        String comment,
        String merchantResponse,
        OffsetDateTime respondedAt,
        OffsetDateTime createdAt
) {
}
