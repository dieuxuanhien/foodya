package com.foodya.backend.infrastructure.persistence.models;

import com.foodya.backend.domain.value_objects.PayeeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "settlement_lines")
public class SettlementLinePersistenceModel {

    @Id
    private UUID id;

    @Column(name = "settlement_id", nullable = false)
    private UUID settlementId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_code", nullable = false, length = 30)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payee_type", nullable = false, length = 16)
    private PayeeType payeeType;

    @Column(name = "gross_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "platform_fee_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal platformFeeAmount;

    @Column(name = "cash_collected_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal cashCollectedAmount;

    @Column(name = "net_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSettlementId() { return settlementId; }
    public void setSettlementId(UUID settlementId) { this.settlementId = settlementId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public PayeeType getPayeeType() { return payeeType; }
    public void setPayeeType(PayeeType payeeType) { this.payeeType = payeeType; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getPlatformFeeAmount() { return platformFeeAmount; }
    public void setPlatformFeeAmount(BigDecimal platformFeeAmount) { this.platformFeeAmount = platformFeeAmount; }
    public BigDecimal getCashCollectedAmount() { return cashCollectedAmount; }
    public void setCashCollectedAmount(BigDecimal cashCollectedAmount) { this.cashCollectedAmount = cashCollectedAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
}