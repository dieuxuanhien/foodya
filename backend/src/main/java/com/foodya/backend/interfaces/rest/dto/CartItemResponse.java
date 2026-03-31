package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        String menuItemId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String note
) {
}
