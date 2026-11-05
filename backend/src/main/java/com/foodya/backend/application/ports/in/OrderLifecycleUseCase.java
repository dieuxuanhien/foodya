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

    /** Merchant rejects an order it cannot fulfil (PENDING/ACCEPTED/ASSIGNED/PREPARING → CANCELLED). */
    OrderDetailView merchantReject(UUID merchantUserId, UUID orderId, String reason);

    /** Orders the driver currently holds (ASSIGNED, PREPARING or DELIVERING). Never another driver's orders. */
    List<OrderSummaryView> deliveryAssignments(UUID driverUserId);

    OrderDetailView deliveryAccept(UUID driverUserId, UUID orderId);

    /** Driver declines an assignment; the order returns to ACCEPTED for reassignment. */
    OrderDetailView deliveryReject(UUID driverUserId, UUID orderId);

    OrderDetailView deliveryUpdateStatus(UUID driverUserId, UUID orderId, OrderStatus targetStatus);

    OrderTrackingPointView addTrackingPoint(UUID driverUserId, UUID orderId,
                                            BigDecimal lat, BigDecimal lng, OffsetDateTime recordedAt);

    List<OrderTrackingPointView> customerTrackingPoints(UUID customerUserId, UUID orderId);
}
