package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateMenuItemRequest(
        String categoryId,
        List<String> taxonomyCodes,
        String name,
        String description,
        BigDecimal price,
        Boolean isActive,
        Boolean isAvailable
) {
}
