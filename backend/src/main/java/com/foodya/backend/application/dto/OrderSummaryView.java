package com.foodya.backend.application.dto;

import com.foodya.backend.domain.model.OrderStatus;
import com.foodya.backend.domain.model.PaymentStatus;

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
