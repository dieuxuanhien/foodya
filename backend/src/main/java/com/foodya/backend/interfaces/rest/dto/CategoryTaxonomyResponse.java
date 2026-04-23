package com.foodya.backend.interfaces.rest.dto;

public record CategoryTaxonomyResponse(
        String code,
        String displayName,
        int sortOrder,
        boolean active
) {
}