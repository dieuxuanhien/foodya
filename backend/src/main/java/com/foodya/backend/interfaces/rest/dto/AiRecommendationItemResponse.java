package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AiRecommendationItemResponse(
        UUID menuItemId,
        String menuItemName,
        UUID restaurantId,
        String restaurantName,
        BigDecimal price,
        String reason
) {
}
