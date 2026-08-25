package com.foodya.backend.application.dto;

public record UpdateCategoryTaxonomyRequest(
        String displayName,
        String description,
        String icon,
        Integer sortOrder,
        Boolean isActive
) {
}
