package com.foodya.backend.interfaces.rest.mapper;

import com.foodya.backend.application.dto.OrderCreatedView;
import com.foodya.backend.interfaces.rest.dto.OrderCreatedResponse;

public final class OrderRestMapper {

    private OrderRestMapper() {
    }

    public static OrderCreatedResponse toResponse(OrderCreatedView view) {
        return new OrderCreatedResponse(
                view.orderId().toString(),
                view.orderCode(),
                view.status(),
                view.paymentMethod(),
                view.paymentStatus(),
                view.subtotalAmount(),
                view.deliveryFee(),
                view.totalAmount(),
                view.commissionAmount(),
                view.shippingFeeMarginAmount(),
                view.platformProfitAmount(),
                view.currencyCode()
        );
    }
}
