package com.foodya.backend.application.dto;

import java.math.BigDecimal;

public record CreateOrderFromCartRequest(
        String deliveryAddress,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        String customerNote
) {
}
