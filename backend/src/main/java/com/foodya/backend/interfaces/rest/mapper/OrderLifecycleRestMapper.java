package com.foodya.backend.interfaces.rest.mapper;

import com.foodya.backend.application.dto.OrderDetailView;
import com.foodya.backend.application.dto.OrderSummaryView;
import com.foodya.backend.application.dto.OrderTrackingPointView;
import com.foodya.backend.interfaces.rest.dto.OrderDetailResponse;
import com.foodya.backend.interfaces.rest.dto.OrderSummaryResponse;
import com.foodya.backend.interfaces.rest.dto.OrderTrackingPointResponse;

public final class OrderLifecycleRestMapper {

    private OrderLifecycleRestMapper() {
    }

    public static OrderSummaryResponse toSummary(OrderSummaryView view) {
        return new OrderSummaryResponse(
                view.orderId(),
                view.orderCode(),
                view.status().name(),
                view.paymentStatus().name(),
                view.totalAmount()
        );
    }

    public static OrderDetailResponse toDetail(OrderDetailView view) {
        return new OrderDetailResponse(
                view.orderId(),
                view.orderCode(),
                view.restaurantId(),
                view.customerUserId(),
                view.status().name(),
                view.paymentMethod().name(),
                view.paymentStatus().name(),
                view.subtotalAmount(),
                view.deliveryFee(),
                view.totalAmount(),
                view.deliveryAddress()
        );
    }

    public static OrderTrackingPointResponse toTrackingPoint(OrderTrackingPointView view) {
        return new OrderTrackingPointResponse(view.lat(), view.lng(), view.recordedAt());
    }
}
