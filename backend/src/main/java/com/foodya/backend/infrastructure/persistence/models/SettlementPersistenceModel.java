package com.foodya.backend.infrastructure.persistence.models;

import com.foodya.backend.domain.value_objects.PayeeType;
import com.foodya.backend.domain.value_objects.SettlementDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlements")
public class SettlementPersistenceModel {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "payee_type", nullable = false, length = 16)
    private PayeeType payeeType;

    @Column(name = "payee_user_id", nullable = false)
    private UUID payeeUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 16)
    private SettlementDirection direction;

    @Column(name = "order_count", nullable = false)
    private int orderCount;

    @Column(name = "gross_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "platform_fee_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal platformFeeAmount;

    @Column(name = "cash_collected_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal cashCollectedAmount;

    @Column(name = "net_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Column(name = "settled_by", nullable = false, length = 128)
    private String settledBy;

    @Column(name = "settled_at", nullable = false)
    private OffsetDateTime settledAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PayeeType getPayeeType() { return payeeType; }
    public void setPayeeType(PayeeType payeeType) { this.payeeType = payeeType; }
    public UUID getPayeeUserId() { return payeeUserId; }
    public void setPayeeUserId(UUID payeeUserId) { this.payeeUserId = payeeUserId; }
    public SettlementDirection getDirection() { return direction; }
    public void setDirection(SettlementDirection direction) { this.direction = direction; }
    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getPlatformFeeAmount() { return platformFeeAmount; }
    public void setPlatformFeeAmount(BigDecimal platformFeeAmount) { this.platformFeeAmount = platformFeeAmount; }
    public BigDecimal getCashCollectedAmount() { return cashCollectedAmount; }
    public void setCashCollectedAmount(BigDecimal cashCollectedAmount) { this.cashCollectedAmount = cashCollectedAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getSettledBy() { return settledBy; }
    public void setSettledBy(String settledBy) { this.settledBy = settledBy; }
    public OffsetDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(OffsetDateTime settledAt) { this.settledAt = settledAt; }
}