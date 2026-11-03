package com.foodya.backend.domain.entities;

import com.foodya.backend.domain.value_objects.PaymentGateway;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentTransactionStatus;

import java.time.Instant;
import java.util.UUID;

/** Gateway transaction lifecycle: PENDING → PROCESSING → COMPLETED / FAILED → REFUNDED. */
public class PaymentTransaction {

    private UUID id;
    private UUID orderId;
    private PaymentMethod paymentMethod;
    private PaymentGateway gateway;
    private String gatewayTxnRef;
    private String gatewayTxnNo;
    private long amount;
    private String currency;
    private PaymentTransactionStatus status;
    private String gatewayResponse;
    private String returnUrl;
    private String paymentUrl;
    private String ipAddress;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public PaymentTransaction() {}

    public static PaymentTransaction create(UUID orderId, PaymentMethod paymentMethod, PaymentGateway gateway,
                                            String gatewayTxnRef, long amount, String returnUrl, String ipAddress) {
        PaymentTransaction t = new PaymentTransaction();
        t.id = UUID.randomUUID();
        t.orderId = orderId;
        t.paymentMethod = paymentMethod;
        t.gateway = gateway;
        t.gatewayTxnRef = gatewayTxnRef;
        t.amount = amount;
        t.currency = "VND";
        t.status = PaymentTransactionStatus.PENDING;
        t.returnUrl = returnUrl;
        t.ipAddress = ipAddress;
        t.createdAt = Instant.now();
        t.updatedAt = t.createdAt;
        return t;
    }

    public void markProcessing(String paymentUrl) {
        requireStatus(PaymentTransactionStatus.PENDING, "mark processing");
        this.status = PaymentTransactionStatus.PROCESSING;
        this.paymentUrl = paymentUrl;
        this.updatedAt = Instant.now();
    }

    public void complete(String gatewayTxnNo, String rawResponse) {
        if (status == PaymentTransactionStatus.COMPLETED) {
            return; // idempotent: gateways retry IPNs
        }
        this.status = PaymentTransactionStatus.COMPLETED;
        this.gatewayTxnNo = gatewayTxnNo;
        this.gatewayResponse = rawResponse;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void fail(String rawResponse) {
        if (status == PaymentTransactionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot fail a completed payment transaction");
        }
        this.status = PaymentTransactionStatus.FAILED;
        this.gatewayResponse = rawResponse;
        this.updatedAt = Instant.now();
    }

    public void refund() {
        requireStatus(PaymentTransactionStatus.COMPLETED, "refund");
        this.status = PaymentTransactionStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    private void requireStatus(PaymentTransactionStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("Cannot " + action + " transaction in status " + status);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public PaymentGateway getGateway() { return gateway; }
    public void setGateway(PaymentGateway gateway) { this.gateway = gateway; }
    public String getGatewayTxnRef() { return gatewayTxnRef; }
    public void setGatewayTxnRef(String gatewayTxnRef) { this.gatewayTxnRef = gatewayTxnRef; }
    public String getGatewayTxnNo() { return gatewayTxnNo; }
    public void setGatewayTxnNo(String gatewayTxnNo) { this.gatewayTxnNo = gatewayTxnNo; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PaymentTransactionStatus getStatus() { return status; }
    public void setStatus(PaymentTransactionStatus status) { this.status = status; }
    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
