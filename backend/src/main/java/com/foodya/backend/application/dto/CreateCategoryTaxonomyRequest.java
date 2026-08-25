package com.foodya.backend.application.dto;

public record CreateCategoryTaxonomyRequest(
        String code,
        String displayName,
        String description,
        String icon,
        Integer sortOrder,
        Boolean isActive
) {
}
