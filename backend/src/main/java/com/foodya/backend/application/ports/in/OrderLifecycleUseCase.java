package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.OrderDetailView;
import com.foodya.backend.application.dto.OrderSummaryView;
import com.foodya.backend.application.dto.OrderTrackingPointView;
import com.foodya.backend.domain.value_objects.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderLifecycleUseCase {

    List<OrderSummaryView> customerOrders(UUID customerUserId);

    OrderDetailView customerOrder(UUID customerUserId, UUID orderId);

    OrderDetailView cancelOrder(UUID customerUserId, UUID orderId, String cancelReason);

    List<OrderSummaryView> merchantOrders(UUID merchantUserId, UUID restaurantId);

    OrderDetailView merchantUpdateStatus(UUID merchantUserId, UUID orderId, OrderStatus targetStatus);

    List<OrderSummaryView> deliveryAssignments();

    OrderDetailView deliveryAccept(UUID orderId);

    OrderDetailView deliveryUpdateStatus(UUID orderId, OrderStatus targetStatus);

    OrderTrackingPointView addTrackingPoint(UUID orderId, BigDecimal lat, BigDecimal lng, OffsetDateTime recordedAt);

    List<OrderTrackingPointView> customerTrackingPoints(UUID customerUserId, UUID orderId);
}
