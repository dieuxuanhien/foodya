package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;

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
