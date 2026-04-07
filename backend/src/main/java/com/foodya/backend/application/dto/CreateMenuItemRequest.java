package com.foodya.backend.application.dto;


import java.math.BigDecimal;

public record CreateMenuItemRequest(
        String categoryId,
        String name,
        String description,
        BigDecimal price,
        Boolean isActive,
        Boolean isAvailable
) {
}
