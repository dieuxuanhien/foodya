package com.foodya.backend.application.dto;

import java.util.UUID;

public record AiCatalogVectorHit(
        UUID menuItemId,
        UUID restaurantId,
        String chunkText,
        double similarity
) {
}
