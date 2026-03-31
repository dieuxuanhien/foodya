package com.foodya.backend.application.dto;

import com.foodya.backend.domain.persistence.MenuItem;

import java.math.BigDecimal;

public record MatchedMenuItemResponse(
        String id,
        String name,
        BigDecimal price
) {
    public static MatchedMenuItemResponse from(MenuItem item) {
        return new MatchedMenuItemResponse(item.getId().toString(), item.getName(), item.getPrice());
    }
}
