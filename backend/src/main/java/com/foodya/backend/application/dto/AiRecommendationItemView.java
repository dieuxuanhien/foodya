package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AiRecommendationItemView(
        UUID menuItemId,
        String menuItemName,
        UUID restaurantId,
        String restaurantName,
        BigDecimal price,
        String reason
) {
}
