package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record ActiveCartResponse(
        String cartId,
        String restaurantId,
        BigDecimal subtotal,
        int itemCount,
        List<CartItemResponse> items
) {
}
