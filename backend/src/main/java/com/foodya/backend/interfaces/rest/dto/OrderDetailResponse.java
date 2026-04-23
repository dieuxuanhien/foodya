package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderDetailResponse(
        UUID orderId,
        String orderCode,
        UUID restaurantId,
        UUID customerUserId,
        String status,
        String paymentMethod,
        String paymentStatus,
        BigDecimal subtotalAmount,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        String deliveryAddress
) {
}
