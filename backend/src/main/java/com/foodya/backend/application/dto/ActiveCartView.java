package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Application layer active cart view with restaurant details.
 */
public record ActiveCartView(
        UUID cartId,
        UUID restaurantId,
        String restaurantName,
        BigDecimal subtotal,
        int itemCount,
        List<CartItemView> items
) {
    public static ActiveCartView empty() {
        return new ActiveCartView(null, null, null, BigDecimal.ZERO, 0, List.of());
    }
}
