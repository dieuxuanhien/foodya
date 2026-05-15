package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID orderId,
        String orderCode,
        String customerName,
        String restaurantName,
        String status,
        String paymentStatus,
        BigDecimal totalAmount
) {
}
