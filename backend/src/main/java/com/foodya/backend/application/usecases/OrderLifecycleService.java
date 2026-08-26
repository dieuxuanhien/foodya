package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.OrderDetailView;
import com.foodya.backend.application.dto.OrderSummaryView;
import com.foodya.backend.application.dto.OrderTrackingPointView;
import com.foodya.backend.application.dto.UserAccountData;
import com.foodya.backend.application.event.OrderNotificationEvent;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.OrderLifecycleUseCase;
import com.foodya.backend.application.ports.out.DeliveryTrackingPointPort;
import com.foodya.backend.application.ports.out.OrderEventPublisherPort;
import com.foodya.backend.application.ports.out.OrderManagementPort;
import com.foodya.backend.application.ports.out.OrderPaymentPort;
import com.foodya.backend.application.ports.out.OrderTrackingUpdatePublisherPort;
import com.foodya.backend.application.ports.out.RestaurantPort;
import com.foodya.backend.application.ports.out.UserAccountPort;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.entities.DeliveryTrackingPoint;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.OrderPayment;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OrderLifecycleService implements OrderLifecycleUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderLifecycleService.class);

    private final OrderManagementPort orderManagementPort;
    private final RestaurantPort restaurantPort;
    private final DeliveryTrackingPointPort deliveryTrackingPointPort;
    private final OrderEventPublisherPort orderEventPublisherPort;
    private final UserAccountPort userAccountPort;
    private final OrderPaymentPort orderPaymentPort;
    private final OrderTrackingUpdatePublisherPort orderTrackingUpdatePublisherPort;

    public OrderLifecycleService(OrderManagementPort orderManagementPort,
                                 RestaurantPort restaurantPort,
                                 DeliveryTrackingPointPort deliveryTrackingPointPort,
                                 OrderEventPublisherPort orderEventPublisherPort,
                                 UserAccountPort userAccountPort,
                                 OrderPaymentPort orderPaymentPort,
                                 OrderTrackingUpdatePublisherPort orderTrackingUpdatePublisherPort) {
        this.orderManagementPort = orderManagementPort;
        this.restaurantPort = restaurantPort;
        this.deliveryTrackingPointPort = deliveryTrackingPointPort;
        this.orderEventPublisherPort = orderEventPublisherPort;
        this.userAccountPort = userAccountPort;
        this.orderPaymentPort = orderPaymentPort;
        this.orderTrackingUpdatePublisherPort = orderTrackingUpdatePublisherPort;
    }

    public List<OrderSummaryView> customerOrders(UUID customerUserId) {
        return orderManagementPort.findByCustomerUserIdOrderByPlacedAtDesc(customerUserId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public OrderDetailView customerOrder(UUID customerUserId, UUID orderId) {
        Order order = requireOrder(orderId);
        if (!order.getCustomerUserId().equals(customerUserId)) {
            throw new ForbiddenException("order does not belong to customer");
        }
        return toDetail(order);
    }

    public OrderDetailView cancelOrder(UUID customerUserId, UUID orderId, String cancelReason) {
        Order order = requireOrder(orderId);
        if (!order.getCustomerUserId().equals(customerUserId)) {
            throw new ForbiddenException("order does not belong to customer");
        }
        try {
            order.cancelByCustomer(cancelReason);
        } catch (IllegalStateException ex) {
            throw new ValidationException("order is not cancellable", Map.of("status", "must be PENDING, ACCEPTED, or ASSIGNED"));
        }
        Order saved = orderManagementPort.save(order);
        publishOrderEvent(saved, "ORDER_CANCELLED", "Order " + saved.getOrderCode() + " cancelled");
        return toDetail(saved);
    }

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

    public OrderDetailView merchantOrder(UUID merchantUserId, UUID orderId) {
        Order order = requireOrder(orderId);
        assertMerchantOwnsOrder(merchantUserId, order.getRestaurantId());
        return toDetail(order);
    }

    public OrderDetailView merchantUpdateStatus(UUID merchantUserId, UUID orderId, OrderStatus targetStatus) {
        Order order = requireOrder(orderId);
        assertMerchantOwnsOrder(merchantUserId, order.getRestaurantId());

        if (targetStatus == OrderStatus.ACCEPTED) {
            try {
                order.merchantAccept();
            } catch (IllegalStateException ex) {
                throw new ValidationException("invalid merchant status transition",
                        Map.of("status", "only PENDING->ACCEPTED or ASSIGNED->PREPARING is allowed"));
            }
            Order saved = orderManagementPort.save(order);
            publishOrderEvent(saved, "ORDER_ACCEPTED", "Order " + saved.getOrderCode() + " accepted");
            return toDetail(saved);
        }
        if (targetStatus == OrderStatus.PREPARING) {
            try {
                order.merchantMarkPreparing();
            } catch (IllegalStateException ex) {
                throw new ValidationException("invalid merchant status transition",
                        Map.of("status", "only PENDING->ACCEPTED or ASSIGNED->PREPARING is allowed"));
            }
            Order saved = orderManagementPort.save(order);
            publishOrderEvent(saved, "ORDER_PREPARING", "Order " + saved.getOrderCode() + " is being prepared");
            return toDetail(saved);
        }

        throw new ValidationException("invalid merchant status transition",
                Map.of("status", "only PENDING->ACCEPTED or ASSIGNED->PREPARING is allowed"));
    }

    public List<OrderSummaryView> deliveryAssignments() {
        return orderManagementPort.findByStatusInOrderByPlacedAtAsc(List.of(OrderStatus.ACCEPTED, OrderStatus.ASSIGNED))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public OrderDetailView deliveryAccept(UUID orderId) {
        Order order = requireOrder(orderId);
        try {
            order.deliveryAcceptAssignment();
        } catch (IllegalStateException ex) {
            throw new ValidationException("order is not assignable", Map.of("status", "must be ACCEPTED"));
        }
        Order saved = orderManagementPort.save(order);
        publishOrderEvent(saved, "ORDER_ASSIGNED", "Order " + saved.getOrderCode() + " assigned to delivery");
        return toDetail(saved);
    }

    public OrderDetailView deliveryUpdateStatus(UUID orderId, OrderStatus targetStatus) {
        Order order = requireOrder(orderId);
        if (targetStatus == OrderStatus.DELIVERING) {
            try {
                order.deliveryStart();
            } catch (IllegalStateException ex) {
                throw new ValidationException("invalid delivery status transition",
                        Map.of("status", "allowed: ASSIGNED->DELIVERING and DELIVERING->SUCCESS|FAILED"));
            }
            Order saved = orderManagementPort.save(order);
            publishOrderEvent(saved, "ORDER_DELIVERING", "Order " + saved.getOrderCode() + " is delivering");
            return toDetail(saved);
        }

        if (targetStatus == OrderStatus.SUCCESS || targetStatus == OrderStatus.FAILED) {
            try {
                order.deliveryFinish(targetStatus);
                syncCodPaymentState(order, targetStatus);
            } catch (IllegalStateException ex) {
                throw new ValidationException("invalid delivery status transition",
                        Map.of("status", "allowed: ASSIGNED->DELIVERING and DELIVERING->SUCCESS|FAILED"));
            }
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
        OrderTrackingPointView view = new OrderTrackingPointView(saved.getLat(), saved.getLng(), saved.getRecordedAt());
        try {
            orderTrackingUpdatePublisherPort.publishTrackingPoint(order.getCustomerUserId(), orderId, view);
        } catch (RuntimeException ex) {
            log.warn("Failed to publish tracking update for order {}", orderId, ex);
        }
        return view;
    }

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
        orderEventPublisherPort.publishOrderNotification(new OrderNotificationEvent(
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

    private void syncCodPaymentState(Order order, OrderStatus targetStatus) {
        if (targetStatus == OrderStatus.SUCCESS) {
            order.syncCodPaymentStatus(PaymentStatus.PAID);
        } else if (targetStatus == OrderStatus.FAILED) {
            order.syncCodPaymentStatus(PaymentStatus.FAILED);
        } else {
            return;
        }

        OrderPayment payment = orderPaymentPort.findByOrderId(order.getId())
                .orElseGet(OrderPayment::new);
        payment.setOrderId(order.getId());
        payment.setPaymentMethod(order.getPaymentMethod());
        payment.setPaymentStatus(order.getPaymentStatus());
        payment.setAmount(order.getTotalAmount());
        payment.setPaidAt(order.getPaymentStatus() == PaymentStatus.PAID
                ? (order.getCompletedAt() == null ? OffsetDateTime.now() : order.getCompletedAt())
                : null);
        orderPaymentPort.save(payment);
    }

    private OrderSummaryView toSummary(Order order) {
        String restaurantName = restaurantPort.findById(order.getRestaurantId())
                .map(Restaurant::getName)
                .orElse("Unknown Restaurant");
        String customerName = userAccountPort.findById(order.getCustomerUserId())
                .map(com.foodya.backend.domain.entities.UserAccount::getFullName)
                .orElse("Unknown Customer");

        return new OrderSummaryView(
                order.getId(),
                order.getOrderCode(),
                customerName,
                restaurantName,
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount()
        );
    }

    private OrderDetailView toDetail(Order order) {
        String restaurantName = restaurantPort.findById(order.getRestaurantId())
                .map(Restaurant::getName)
                .orElse("Unknown Restaurant");
        String customerName = userAccountPort.findById(order.getCustomerUserId())
                .map(com.foodya.backend.domain.entities.UserAccount::getFullName)
                .orElse("Unknown Customer");

        return new OrderDetailView(
                order.getId(),
                order.getOrderCode(),
                order.getRestaurantId(),
                restaurantName,
                order.getCustomerUserId(),
                customerName,
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getSubtotalAmount(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getDeliveryAddress(),
                order.getDeliveryLatitude(),
                order.getDeliveryLongitude()
        );
    }
}
