package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemView(
        UUID menuItemId,
        int quantity,
        BigDecimal unitPriceSnapshot,
        BigDecimal lineTotal,
        String note
) {
}
