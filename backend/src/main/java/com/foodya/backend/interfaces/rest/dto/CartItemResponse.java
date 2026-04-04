package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;

/**
 * Enhanced cart item response with menu item details to avoid N+1 queries.
 * Clients can display cart items without additional API calls.
 */
public record CartItemResponse(
        String menuItemId,
        String menuItemName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        String note
) {
}
