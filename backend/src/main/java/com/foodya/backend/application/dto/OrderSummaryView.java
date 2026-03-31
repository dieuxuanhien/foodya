package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderSummaryView(
        UUID orderId,
        String orderCode,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount
) {
}
