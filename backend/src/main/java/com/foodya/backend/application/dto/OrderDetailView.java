package com.foodya.backend.application.dto;

import com.foodya.backend.domain.model.OrderStatus;
import com.foodya.backend.domain.model.PaymentMethod;
import com.foodya.backend.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderDetailView(
        UUID orderId,
        String orderCode,
        UUID restaurantId,
        UUID customerUserId,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal subtotalAmount,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        String deliveryAddress
) {
}
