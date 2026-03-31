package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;

public record MenuItemResponse(
        String id,
        String restaurantId,
        String categoryId,
        String name,
        String description,
        BigDecimal price,
        boolean active,
        boolean available
) {
}
