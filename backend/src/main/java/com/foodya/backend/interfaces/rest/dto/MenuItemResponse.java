package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record MenuItemResponse(
        String id,
        String restaurantId,
        String categoryId,
        List<String> taxonomyCodes,
        String name,
        String description,
        String imageUrl,
        BigDecimal price,
        boolean active,
        boolean available
) {
}
