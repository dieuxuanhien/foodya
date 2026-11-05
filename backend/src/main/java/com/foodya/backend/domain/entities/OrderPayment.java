package com.foodya.backend.domain.entities;

import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class OrderPayment {

    private UUID id;

    private UUID orderId;

    private PaymentMethod paymentMethod;

    private PaymentStatus paymentStatus;

    private BigDecimal amount;

    private OffsetDateTime paidAt;

    private String externalRef;

    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void markPaid(String transactionId) {
        if (paymentStatus == PaymentStatus.FAILED) {
            throw new IllegalStateException("Cannot mark a failed payment as paid");
        }
        this.paymentStatus = PaymentStatus.PAID;
        this.externalRef = transactionId;
        this.paidAt = OffsetDateTime.now();
    }

    public void markFailed(String reason) {
        if (paymentStatus == PaymentStatus.PAID) {
            throw new IllegalStateException("Cannot mark a completed payment as failed");
        }
        this.paymentStatus = PaymentStatus.FAILED;
        if (reason != null && !reason.isBlank()) {
            this.externalRef = reason.trim();
        }
    }
}
