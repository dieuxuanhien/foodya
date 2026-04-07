package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record MatchedMenuItemView(
        String id,
        String name,
        BigDecimal price,
        List<String> images
) {
    public static MatchedMenuItemView from(MenuItemData item) {
        return new MatchedMenuItemView(
                item.getId().toString(),
                item.getName(),
                item.getPrice(),
                item.getImageUrl() == null || item.getImageUrl().isBlank() ? List.of() : List.of(item.getImageUrl())
        );
    }
}
