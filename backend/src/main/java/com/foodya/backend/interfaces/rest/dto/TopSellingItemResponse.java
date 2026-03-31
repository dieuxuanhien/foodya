package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopSellingItemResponse(
        UUID menuItemId,
        String itemName,
        long quantitySold,
        BigDecimal revenue
) {
}
