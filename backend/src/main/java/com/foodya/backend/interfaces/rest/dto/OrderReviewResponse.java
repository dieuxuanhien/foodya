package com.foodya.backend.interfaces.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderReviewResponse(
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
