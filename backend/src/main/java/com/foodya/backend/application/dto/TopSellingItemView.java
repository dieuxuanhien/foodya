package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopSellingItemView(
        UUID menuItemId,
        String itemName,
        long quantitySold,
        BigDecimal revenue
) {
}
