package com.foodya.backend.interfaces.rest.dto;

import java.time.OffsetDateTime;

public record CategoryTaxonomyResponse(
        String code,
        String displayName,
        String description,
        String icon,
        int sortOrder,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}