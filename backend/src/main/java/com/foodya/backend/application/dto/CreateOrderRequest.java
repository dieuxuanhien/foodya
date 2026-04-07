package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        String restaurantId,
        List<CreateOrderItemRequest> items,
        String deliveryAddress,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        String customerNote
) {
}
