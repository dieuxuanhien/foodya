package com.foodya.backend.domain.entities;

import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DOMAIN ENTITY: Order
 * 
 * This is a PURE domain entity with NO framework dependencies or annotations.
 * - All business logic and state transitions are here
 * - No persistence concerns (JPA, database mapping)
 * - Fully unit-testable without any framework
 * - Framework-independent and reusable across technologies
 * 
 * Persistence mapping is handled by:
 * - OrderPersistenceModel (in infrastructure/persistence/models/)
 * - OrderMapper (in infrastructure/persistence/mappers/)
 * 
 * This separation follows Clean Architecture's inward dependency principle:
 * Domain → no outer layer (framework) dependencies.
 */
public class Order {

    private static final List<OrderStatus> CANCELLABLE_BY_CUSTOMER = List.of(
            OrderStatus.PENDING,
            OrderStatus.ACCEPTED,
            OrderStatus.ASSIGNED
    );

    private UUID id;

    private String orderCode;

    private UUID customerUserId;

    private String idempotencyKey;

    private UUID restaurantId;

    private OrderStatus status;

    private String deliveryAddress;

    private BigDecimal deliveryLatitude;

    private BigDecimal deliveryLongitude;

    private String customerNote;

    private BigDecimal subtotalAmount;

    private BigDecimal deliveryFee;

    private BigDecimal totalAmount;

    private PaymentMethod paymentMethod;

    private PaymentStatus paymentStatus;

    private BigDecimal commissionAmount;

    private BigDecimal shippingFeeMarginAmount;

    private BigDecimal platformProfitAmount;

    private String cancelReason;

    private OffsetDateTime placedAt;

    private OffsetDateTime completedAt;

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

    public void setId(UUID id) {
        this.id = id;
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
                    || target == OrderStatus.CANCELLED
                    || target == OrderStatus.FAILED;
            case PREPARING -> target == OrderStatus.DELIVERING || target == OrderStatus.CANCELLED || target == OrderStatus.FAILED;
            case DELIVERING -> target == OrderStatus.SUCCESS || target == OrderStatus.FAILED;
            case SUCCESS, CANCELLED, FAILED -> false;
        };
    }
}
