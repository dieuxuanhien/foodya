package com.foodya.backend.domain.entities;

import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    private static final List<OrderStatus> CANCELLABLE_BY_CUSTOMER = List.of(
            OrderStatus.PENDING,
            OrderStatus.ACCEPTED,
            OrderStatus.ASSIGNED
    );

    @Id
    private UUID id;

    @Column(name = "order_code", nullable = false, length = 30, unique = true)
    private String orderCode;

    @Column(name = "customer_user_id", nullable = false)
    private UUID customerUserId;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status;

    @Column(name = "delivery_address", nullable = false, columnDefinition = "text")
    private String deliveryAddress;

    @Column(name = "delivery_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal deliveryLongitude;

    @Column(name = "customer_note", columnDefinition = "text")
    private String customerNote;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 16)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 16)
    private PaymentStatus paymentStatus;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "shipping_fee_margin_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFeeMarginAmount;

    @Column(name = "platform_profit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal platformProfitAmount;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "placed_at", nullable = false)
    private OffsetDateTime placedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = OrderStatus.PENDING;
        }
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.COD;
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.UNPAID;
        }
        if (placedAt == null) {
            placedAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public UUID getCustomerUserId() {
        return customerUserId;
    }

    public void setCustomerUserId(UUID customerUserId) {
        this.customerUserId = customerUserId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void cancelByCustomer(String reason) {
        if (!CANCELLABLE_BY_CUSTOMER.contains(status)) {
            throw invalidTransition("customer cancel");
        }
        status = OrderStatus.CANCELLED;
        if (reason != null && !reason.isBlank()) {
            cancelReason = reason.trim();
        }
    }

    public void merchantAccept() {
        requireStatus(OrderStatus.PENDING, "merchant accept");
        status = OrderStatus.ACCEPTED;
    }

    public void merchantMarkPreparing() {
        requireStatus(OrderStatus.ASSIGNED, "merchant mark preparing");
        status = OrderStatus.PREPARING;
    }

    public void deliveryAcceptAssignment() {
        requireStatus(OrderStatus.ACCEPTED, "delivery accept assignment");
        status = OrderStatus.ASSIGNED;
    }

    public void deliveryStart() {
        requireStatus(OrderStatus.ASSIGNED, "delivery start");
        status = OrderStatus.DELIVERING;
    }

    public void deliveryFinish(OrderStatus terminalStatus) {
        requireStatus(OrderStatus.DELIVERING, "delivery finish");
        if (terminalStatus != OrderStatus.SUCCESS && terminalStatus != OrderStatus.FAILED) {
            throw invalidTransition("delivery finish");
        }
        status = terminalStatus;
        if (terminalStatus == OrderStatus.SUCCESS) {
            completedAt = OffsetDateTime.now();
        }
    }

    public void adminTransitionTo(OrderStatus targetStatus) {
        if (!isValidAdminTransition(status, targetStatus)) {
            throw invalidTransition("admin transition");
        }
        status = targetStatus;
        if (targetStatus == OrderStatus.SUCCESS) {
            completedAt = OffsetDateTime.now();
        }
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public BigDecimal getDeliveryLatitude() {
        return deliveryLatitude;
    }

    public void setDeliveryLatitude(BigDecimal deliveryLatitude) {
        this.deliveryLatitude = deliveryLatitude;
    }

    public BigDecimal getDeliveryLongitude() {
        return deliveryLongitude;
    }

    public void setDeliveryLongitude(BigDecimal deliveryLongitude) {
        this.deliveryLongitude = deliveryLongitude;
    }

    public String getCustomerNote() {
        return customerNote;
    }

    public void setCustomerNote(String customerNote) {
        this.customerNote = customerNote;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public BigDecimal getShippingFeeMarginAmount() {
        return shippingFeeMarginAmount;
    }

    public void setShippingFeeMarginAmount(BigDecimal shippingFeeMarginAmount) {
        this.shippingFeeMarginAmount = shippingFeeMarginAmount;
    }

    public BigDecimal getPlatformProfitAmount() {
        return platformProfitAmount;
    }

    public void setPlatformProfitAmount(BigDecimal platformProfitAmount) {
        this.platformProfitAmount = platformProfitAmount;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public OffsetDateTime getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(OffsetDateTime placedAt) {
        this.placedAt = placedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    private void requireStatus(OrderStatus expected, String action) {
        if (status != expected) {
            throw invalidTransition(action);
        }
    }

    private IllegalStateException invalidTransition(String action) {
        return new IllegalStateException("invalid transition for " + action + " from status " + status);
    }

    private static boolean isValidAdminTransition(OrderStatus current, OrderStatus target) {
        if (current == target) {
            return true;
        }

        return switch (current) {
            case PENDING -> target == OrderStatus.ACCEPTED || target == OrderStatus.CANCELLED || target == OrderStatus.FAILED;
            case ACCEPTED -> target == OrderStatus.ASSIGNED || target == OrderStatus.CANCELLED || target == OrderStatus.FAILED;
            case ASSIGNED -> target == OrderStatus.PREPARING
                    || target == OrderStatus.DELIVERING
                    || target == OrderStatus.CANCELLED
                    || target == OrderStatus.FAILED;
            case PREPARING -> target == OrderStatus.DELIVERING || target == OrderStatus.CANCELLED || target == OrderStatus.FAILED;
            case DELIVERING -> target == OrderStatus.SUCCESS || target == OrderStatus.FAILED;
            case SUCCESS, CANCELLED, FAILED -> false;
        };
    }
}
