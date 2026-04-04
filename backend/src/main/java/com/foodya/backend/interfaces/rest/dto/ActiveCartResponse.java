package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Enhanced active cart response with restaurant details.
 * Includes restaurant name to avoid additional API call.
 */
public record ActiveCartResponse(
        String cartId,
        String restaurantId,
        String restaurantName,
        BigDecimal subtotal,
        int itemCount,
        List<CartItemResponse> items
) {
}
