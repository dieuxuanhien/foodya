package com.foodya.backend.application.dto;


import java.math.BigDecimal;

public record UpdateMenuItemRequest(
        String categoryId,
        String name,
        String description,
        BigDecimal price,
        Boolean isActive,
        Boolean isAvailable
) {
}
