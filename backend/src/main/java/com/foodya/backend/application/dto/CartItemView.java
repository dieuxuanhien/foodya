package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Application layer cart item view with menu item details.
 */
public record CartItemView(
        UUID menuItemId,
        String menuItemName,
        int quantity,
        BigDecimal unitPriceSnapshot,
        BigDecimal lineTotal,
        String note
) {
}
