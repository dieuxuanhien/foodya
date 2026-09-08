package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.OrderDetailView;
import com.foodya.backend.application.dto.OrderSummaryView;
import com.foodya.backend.application.dto.OrderTrackingPointView;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.value_objects.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderLifecycleUseCase {

    PaginatedResult<OrderSummaryView> customerOrders(UUID customerUserId, Integer page, Integer size);

    OrderDetailView customerOrder(UUID customerUserId, UUID orderId);

    OrderDetailView cancelOrder(UUID customerUserId, UUID orderId, String cancelReason);

    PaginatedResult<OrderSummaryView> merchantOrders(UUID merchantUserId, UUID restaurantId, Integer page, Integer size);

    OrderDetailView merchantOrder(UUID merchantUserId, UUID orderId);

    OrderDetailView merchantUpdateStatus(UUID merchantUserId, UUID orderId, OrderStatus targetStatus);

    List<OrderSummaryView> deliveryAssignments();

    OrderDetailView deliveryAccept(UUID orderId);

    OrderDetailView deliveryUpdateStatus(UUID orderId, OrderStatus targetStatus);

    OrderTrackingPointView addTrackingPoint(UUID orderId, BigDecimal lat, BigDecimal lng, OffsetDateTime recordedAt);

    List<OrderTrackingPointView> customerTrackingPoints(UUID customerUserId, UUID orderId);
}
