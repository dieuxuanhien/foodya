package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record MenuItemResponse(
        String id,
        String restaurantId,
        String categoryId,
        String name,
        String description,
        String imageUrl,
        List<String> images,
        BigDecimal price,
        boolean active,
        boolean available
) {
}
