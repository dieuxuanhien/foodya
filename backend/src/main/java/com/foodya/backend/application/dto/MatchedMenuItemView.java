package com.foodya.backend.application.dto;

import java.math.BigDecimal;

public record MatchedMenuItemView(
        String id,
        String name,
        BigDecimal price
) {
    public static MatchedMenuItemView from(MenuItemModel item) {
        return new MatchedMenuItemView(item.getId().toString(), item.getName(), item.getPrice());
    }
}
