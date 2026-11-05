package com.foodya.backend.application.usecases;

import com.foodya.backend.application.event.OrderNotificationEvent;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.PaymentUseCase;
import com.foodya.backend.application.ports.out.DriverOnlineSessionPort;
import com.foodya.backend.application.ports.out.OrderBatchPort;
import com.foodya.backend.application.ports.out.OrderEventPublisherPort;
import com.foodya.backend.application.ports.out.OrderManagementPort;
import com.foodya.backend.application.ports.out.OrderPaymentPort;
import com.foodya.backend.application.ports.out.RestaurantPort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.OrderPayment;
import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Follow-up work that must happen whenever an order leaves the normal flow or finishes: free the
 * driver's slot, close the batch item, settle COD cash, and return money for paid orders.
 *
 * <p>Every caller (customer cancel, merchant reject, driver release, admin override, timeouts) goes
 * through here so these side effects cannot be forgotten on one path. Refunds are delegated to the
 * payment layer, which is idempotent per order.
 */
public class OrderClosureService {

    private static final Logger log = LoggerFactory.getLogger(OrderClosureService.class);
    private static final String SYSTEM_IP = "127.0.0.1";

    private final OrderManagementPort orderManagementPort;
    private final OrderBatchPort orderBatchPort;
    private final DriverOnlineSessionPort driverOnlineSessionPort;
    private final OrderPaymentPort orderPaymentPort;
    private final RestaurantPort restaurantPort;
    private final OrderEventPublisherPort orderEventPublisherPort;
    private final PaymentUseCase paymentUseCase;
    private final SystemParameterPort systemParameterPort;

    public OrderClosureService(OrderManagementPort orderManagementPort,
                               OrderBatchPort orderBatchPort,
                               DriverOnlineSessionPort driverOnlineSessionPort,
                               OrderPaymentPort orderPaymentPort,
                               RestaurantPort restaurantPort,
                               OrderEventPublisherPort orderEventPublisherPort,
                               PaymentUseCase paymentUseCase,
                               SystemParameterPort systemParameterPort) {
        this.orderManagementPort = orderManagementPort;
        this.orderBatchPort = orderBatchPort;
        this.driverOnlineSessionPort = driverOnlineSessionPort;
        this.orderPaymentPort = orderPaymentPort;
        this.restaurantPort = restaurantPort;
        this.orderEventPublisherPort = orderEventPublisherPort;
        this.paymentUseCase = paymentUseCase;
        this.systemParameterPort = systemParameterPort;
    }

    /**
     * System-initiated cancellation (timeouts). Fails when delivery already started.
     *
     * @return true when a paid order was refunded
     */
    public boolean cancel(UUID orderId, String actor, String reason) {
        Order order = orderManagementPort.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        try {
            order.cancelBySystem(reason);
        } catch (IllegalStateException ex) {
            throw new ValidationException("order cannot be cancelled in its current state",
                    Map.of("status", order.getStatus().name()));
        }
        // Batch id and driver must be read before the cancel clears the order's assignment.
        UUID batchId = order.getBatchId();
        UUID driverUserId = order.getDriverUserId();
        Order saved = orderManagementPort.save(order);
        boolean refunded = onClosed(saved, batchId, driverUserId, actor, reason);
        notifyOrderEvent(saved, "ORDER_CANCELLED", "Order " + saved.getOrderCode() + " cancelled: " + reason);
        return refunded;
    }

    /**
     * Call after an order was saved as CANCELLED or FAILED.
     *
     * @return true when a paid order was refunded
     */
    public boolean onClosed(Order order, UUID batchId, UUID driverUserId, String actor, String reason) {
        releaseBatchItem(batchId, order.getId());
        releaseDriverSlot(driverUserId, order.getId());
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            return false;
        }
        try {
            return paymentUseCase.refundOrder(order.getId(), actor, SYSTEM_IP, reason).refunded();
        } catch (RuntimeException ex) {
            // The retry sweep picks it up: the transaction stays COMPLETED while the order is closed.
            log.warn("Refund for order {} failed, will be retried", order.getOrderCode(), ex);
            return false;
        }
    }

    /** Call after a driver was detached from an order that stays open (decline or ghost driver). */
    public void onDriverReleased(UUID driverUserId, UUID batchId, UUID orderId) {
        releaseBatchItem(batchId, orderId);
        releaseDriverSlot(driverUserId, orderId);
    }

    /** Call after a non-batch delivery completed: the driver can take another order. */
    public void onDelivered(UUID driverUserId, UUID orderId) {
        releaseDriverSlot(driverUserId, orderId);
    }

    /**
     * Mirrors the outcome of cash collection onto the order and its payment row. COD is paid when
     * the order succeeds and failed when it fails; online orders are not touched.
     */
    public void settleCodPayment(Order order, OrderStatus terminalStatus) {
        if (terminalStatus == OrderStatus.SUCCESS) {
            order.syncCodPaymentStatus(PaymentStatus.PAID);
        } else if (terminalStatus == OrderStatus.FAILED) {
            order.syncCodPaymentStatus(PaymentStatus.FAILED);
        } else {
            return;
        }

        OrderPayment payment = orderPaymentPort.findByOrderId(order.getId()).orElseGet(OrderPayment::new);
        payment.setOrderId(order.getId());
        payment.setPaymentMethod(order.getPaymentMethod());
        payment.setPaymentStatus(order.getPaymentStatus());
        payment.setAmount(order.getTotalAmount());
        payment.setPaidAt(order.getPaymentStatus() == PaymentStatus.PAID
                ? (order.getCompletedAt() == null ? OffsetDateTime.now() : order.getCompletedAt())
                : null);
        orderPaymentPort.save(payment);
    }

    public void notifyOrderEvent(Order order, String eventType, String message) {
        Restaurant restaurant = restaurantPort.findById(order.getRestaurantId()).orElse(null);
        orderEventPublisherPort.publishOrderNotification(new OrderNotificationEvent(
                order.getId(), order.getOrderCode(), order.getCustomerUserId(),
                restaurant == null ? null : restaurant.getOwnerUserId(), order.getDriverUserId(),
                order.getStatus(), eventType, "Order update " + order.getOrderCode(), message));
    }

    /**
     * Cancels orders the merchant never accepted, and orders accepted but never assigned a driver,
     * once they have waited longer than the configured limits.
     *
     * @return number of orders cancelled
     */
    public int cancelTimedOutOrders() {
        long merchantTimeout = minutes("order.merchant_accept_timeout_minutes", 10);
        long assignmentTimeout = minutes("order.assignment_timeout_minutes", 20);
        int cancelled = 0;
        cancelled += cancelStale(OrderStatus.PENDING, merchantTimeout, "merchant", "restaurant did not respond in time");
        cancelled += cancelStale(OrderStatus.ACCEPTED, assignmentTimeout, "system", "no driver was available in time");
        return cancelled;
    }

    private int cancelStale(OrderStatus status, long timeoutMinutes, String actor, String reason) {
        if (timeoutMinutes <= 0) {
            return 0;
        }
        java.time.OffsetDateTime cutoff = java.time.OffsetDateTime.now().minusMinutes(timeoutMinutes);
        int cancelled = 0;
        for (Order order : orderManagementPort.findStaleByStatus(status, cutoff)) {
            try {
                cancel(order.getId(), actor, reason);
                cancelled++;
            } catch (RuntimeException ex) {
                log.warn("Could not cancel timed-out order {}", order.getOrderCode(), ex);
            }
        }
        return cancelled;
    }

    /**
     * Closes sessions whose heartbeat stopped (driver killed the app) and releases the orders they
     * still hold, so abandoned work returns to the pool instead of being stuck with a ghost driver.
     *
     * @return number of sessions expired
     */
    public int expireStaleDriverSessions() {
        long timeoutSeconds = minutes("driver.heartbeat_timeout_minutes", 5) * 60;
        if (timeoutSeconds <= 0) {
            return 0;
        }
        java.time.Instant cutoff = java.time.Instant.now().minusSeconds(timeoutSeconds);
        int expired = 0;
        for (var session : driverOnlineSessionPort.findLiveSessionsWithHeartbeatBefore(cutoff)) {
            try {
                for (Order order : orderManagementPort.findOpenByDriverUserId(session.getDriverUserId())) {
                    try {
                        UUID batchId = order.getBatchId();
                        order.releaseDriver();
                        orderManagementPort.save(order);
                        releaseBatchItem(batchId, order.getId());
                        notifyOrderEvent(order, "ORDER_ASSIGNED",
                                "Order " + order.getOrderCode() + " returned to the pool (driver connection lost)");
                    } catch (IllegalStateException ex) {
                        log.warn("Could not release order {} from expired driver {}",
                                order.getOrderCode(), session.getDriverUserId());
                    }
                }
                session.expire();
                driverOnlineSessionPort.save(session);
                expired++;
            } catch (RuntimeException ex) {
                log.warn("Could not expire driver session {}", session.getId(), ex);
            }
        }
        return expired;
    }

    private long minutes(String key, long defaultValue) {
        return systemParameterPort.findById(key)
                .map(p -> {
                    try {
                        return Long.parseLong(p.getValue().trim());
                    } catch (NumberFormatException ex) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private void releaseBatchItem(UUID batchId, UUID orderId) {
        if (batchId == null) {
            return;
        }
        try {
            orderBatchPort.failBatchItem(batchId, orderId);
        } catch (RuntimeException ex) {
            log.warn("Could not close batch item for order {}", orderId, ex);
        }
    }

    private void releaseDriverSlot(UUID driverUserId, UUID orderId) {
        if (driverUserId == null) {
            return;
        }
        try {
            driverOnlineSessionPort.findActiveSession(driverUserId).ifPresent(session -> {
                session.releaseSlot();
                driverOnlineSessionPort.save(session);
            });
        } catch (RuntimeException ex) {
            log.warn("Could not release driver slot for order {}", orderId, ex);
        }
    }
}