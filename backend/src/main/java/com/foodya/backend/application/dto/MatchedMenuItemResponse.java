package com.foodya.backend.application.dto;

import java.math.BigDecimal;

public record MatchedMenuItemResponse(
        String id,
        String name,
        BigDecimal price
) {
    public static MatchedMenuItemResponse from(MenuItemModel item) {
        return new MatchedMenuItemResponse(item.getId().toString(), item.getName(), item.getPrice());
    }
}
