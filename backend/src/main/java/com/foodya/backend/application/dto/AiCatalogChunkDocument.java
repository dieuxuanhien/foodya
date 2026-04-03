package com.foodya.backend.application.dto;

import java.util.UUID;
import java.util.List;

public record AiCatalogChunkDocument(
        UUID menuItemId,
        UUID restaurantId,
        String chunkText,
        String metadataJson,
        List<Double> embedding
) {
}
