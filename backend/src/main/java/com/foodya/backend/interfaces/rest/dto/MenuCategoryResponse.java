package com.foodya.backend.interfaces.rest.dto;

public record MenuCategoryResponse(
        String id,
        String restaurantId,
        String name,
        int sortOrder,
        boolean active
) {
}
