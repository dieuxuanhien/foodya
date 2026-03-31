package com.foodya.backend.application.service;

import com.foodya.backend.application.dto.OrderDetailView;
import com.foodya.backend.application.dto.OrderSummaryView;
import com.foodya.backend.application.dto.OrderTrackingPointView;
import com.foodya.backend.application.event.OrderNotificationEvent;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.port.out.DeliveryTrackingPointPort;
import com.foodya.backend.application.port.out.OrderManagementPort;
import com.foodya.backend.application.port.out.RestaurantPort;
import com.foodya.backend.domain.model.OrderStatus;
import com.foodya.backend.domain.persistence.DeliveryTrackingPoint;
import com.foodya.backend.domain.persistence.Order;
import com.foodya.backend.domain.persistence.Restaurant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderLifecycleService {

    private static final List<OrderStatus> CANCELLABLE_STATUSES = List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.ASSIGNED);

    private final OrderManagementPort orderManagementPort;
    private final RestaurantPort restaurantPort;
    private final DeliveryTrackingPointPort deliveryTrackingPointPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    public OrderLifecycleService(OrderManagementPort orderManagementPort,
                                 RestaurantPort restaurantPort,
                                 DeliveryTrackingPointPort deliveryTrackingPointPort,
                                 ApplicationEventPublisher applicationEventPublisher) {
        this.orderManagementPort = orderManagementPort;
        this.restaurantPort = restaurantPort;
        this.deliveryTrackingPointPort = deliveryTrackingPointPort;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryView> customerOrders(UUID customerUserId) {
        return orderManagementPort.findByCustomerUserIdOrderByPlacedAtDesc(customerUserId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailView customerOrder(UUID customerUserId, UUID orderId) {
        Order order = requireOrder(orderId);
        if (!order.getCustomerUserId().equals(customerUserId)) {
            throw new ForbiddenException("order does not belong to customer");
        }
        return toDetail(order);
    }

    @Transactional
    public OrderDetailView cancelOrder(UUID customerUserId, UUID orderId, String cancelReason) {
        Order order = requireOrder(orderId);
        if (!order.getCustomerUserId().equals(customerUserId)) {
            throw new ForbiddenException("order does not belong to customer");
        }
        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new ValidationException("order is not cancellable", Map.of("status", "must be PENDING, ACCEPTED, or ASSIGNED"));
        }

        order.setStatus(OrderStatus.CANCELLED);
        if (cancelReason != null && !cancelReason.isBlank()) {
            order.setCustomerNote(cancelReason.trim());
        }
        Order saved = orderManagementPort.save(order);
        publishOrderEvent(saved, "ORDER_CANCELLED", "Order " + saved.getOrderCode() + " cancelled");
        return toDetail(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryView> merchantOrders(UUID merchantUserId, UUID restaurantId) {
        Restaurant restaurant = restaurantPort.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("restaurant not found"));
        if (!restaurant.getOwnerUserId().equals(merchantUserId)) {
            throw new ForbiddenException("restaurant does not belong to merchant");
        }

        return orderManagementPort.findByRestaurantIdOrderByPlacedAtDesc(restaurantId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public OrderDetailView merchantUpdateStatus(UUID merchantUserId, UUID orderId, OrderStatus targetStatus) {
        Order order = requireOrder(orderId);
        assertMerchantOwnsOrder(merchantUserId, order.getRestaurantId());

        OrderStatus current = order.getStatus();
        if (current == OrderStatus.PENDING && targetStatus == OrderStatus.ACCEPTED) {
            order.setStatus(targetStatus);
            Order saved = orderManagementPort.save(order);
            publishOrderEvent(saved, "ORDER_ACCEPTED", "Order " + saved.getOrderCode() + " accepted");
            return toDetail(saved);
        }
        if (current == OrderStatus.ASSIGNED && targetStatus == OrderStatus.PREPARING) {
            order.setStatus(targetStatus);
            Order saved = orderManagementPort.save(order);
            publishOrderEvent(saved, "ORDER_PREPARING", "Order " + saved.getOrderCode() + " is being prepared");
            return toDetail(saved);
        }

        throw new ValidationException("invalid merchant status transition",
                Map.of("status", "only PENDING->ACCEPTED or ASSIGNED->PREPARING is allowed"));
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryView> deliveryAssignments() {
        return orderManagementPort.findByStatusInOrderByPlacedAtAsc(List.of(OrderStatus.ACCEPTED, OrderStatus.ASSIGNED))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public OrderDetailView deliveryAccept(UUID orderId) {
        Order order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new ValidationException("order is not assignable", Map.of("status", "must be ACCEPTED"));
        }
        order.setStatus(OrderStatus.ASSIGNED);
        Order saved = orderManagementPort.save(order);
        publishOrderEvent(saved, "ORDER_ASSIGNED", "Order " + saved.getOrderCode() + " assigned to delivery");
        return toDetail(saved);
    }

    @Transactional
    public OrderDetailView deliveryUpdateStatus(UUID orderId, OrderStatus targetStatus) {
        Order order = requireOrder(orderId);
        OrderStatus current = order.getStatus();

        if (current == OrderStatus.ASSIGNED && targetStatus == OrderStatus.DELIVERING) {
            order.setStatus(targetStatus);
            Order saved = orderManagementPort.save(order);
            publishOrderEvent(saved, "ORDER_DELIVERING", "Order " + saved.getOrderCode() + " is delivering");
            return toDetail(saved);
        }

        if (current == OrderStatus.DELIVERING && (targetStatus == OrderStatus.SUCCESS || targetStatus == OrderStatus.FAILED)) {
            order.setStatus(targetStatus);
            Order saved = orderManagementPort.save(order);
            publishOrderEvent(
                    saved,
                    targetStatus == OrderStatus.SUCCESS ? "ORDER_SUCCESS" : "ORDER_FAILED",
                    "Order " + saved.getOrderCode() + " completed with status " + targetStatus.name()
            );
            return toDetail(saved);
        }

        throw new ValidationException("invalid delivery status transition",
                Map.of("status", "allowed: ASSIGNED->DELIVERING and DELIVERING->SUCCESS|FAILED"));
    }

    @Transactional
    public OrderTrackingPointView addTrackingPoint(UUID orderId, BigDecimal lat, BigDecimal lng, OffsetDateTime recordedAt) {
        Order order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.ASSIGNED && order.getStatus() != OrderStatus.DELIVERING) {
            throw new ValidationException("tracking point is not allowed in current state",
                    Map.of("status", "must be ASSIGNED or DELIVERING"));
        }

        DeliveryTrackingPoint point = new DeliveryTrackingPoint();
        point.setOrderId(orderId);
        point.setLat(lat);
        point.setLng(lng);
        point.setRecordedAt(recordedAt);

        DeliveryTrackingPoint saved = deliveryTrackingPointPort.save(point);
        return new OrderTrackingPointView(saved.getLat(), saved.getLng(), saved.getRecordedAt());
    }

    @Transactional(readOnly = true)
    public List<OrderTrackingPointView> customerTrackingPoints(UUID customerUserId, UUID orderId) {
        Order order = requireOrder(orderId);
        if (!order.getCustomerUserId().equals(customerUserId)) {
            throw new ForbiddenException("order does not belong to customer");
        }

        return deliveryTrackingPointPort.findByOrderIdOrderByRecordedAtAsc(orderId)
                .stream()
                .map(point -> new OrderTrackingPointView(point.getLat(), point.getLng(), point.getRecordedAt()))
                .toList();
    }

    private void assertMerchantOwnsOrder(UUID merchantUserId, UUID restaurantId) {
        Restaurant restaurant = restaurantPort.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("restaurant not found"));
        if (!restaurant.getOwnerUserId().equals(merchantUserId)) {
            throw new ForbiddenException("order does not belong to merchant");
        }
    }

    private Order requireOrder(UUID orderId) {
        return orderManagementPort.findById(orderId)
                .orElseThrow(() -> new NotFoundException("order not found"));
    }

        private void publishOrderEvent(Order order, String eventType, String message) {
        Restaurant restaurant = restaurantPort.findById(order.getRestaurantId())
            .orElseThrow(() -> new NotFoundException("restaurant not found"));
        applicationEventPublisher.publishEvent(new OrderNotificationEvent(
            order.getId(),
            order.getOrderCode(),
            order.getCustomerUserId(),
            restaurant.getOwnerUserId(),
            order.getStatus(),
            eventType,
            "Order update " + order.getOrderCode(),
            message
        ));
        }

    private OrderSummaryView toSummary(Order order) {
        return new OrderSummaryView(
                order.getId(),
                order.getOrderCode(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount()
        );
    }

    private OrderDetailView toDetail(Order order) {
        return new OrderDetailView(
                order.getId(),
                order.getOrderCode(),
                order.getRestaurantId(),
                order.getCustomerUserId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getSubtotalAmount(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getDeliveryAddress()
        );
    }
}
