package com.foodya.backend.domain.entities;

import com.foodya.backend.domain.policies.SettlementPolicy;
import com.foodya.backend.domain.value_objects.PayeeType;
import com.foodya.backend.domain.value_objects.SettlementDirection;
import com.foodya.backend.domain.value_objects.SettlementLine;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A recorded settlement: the admin marked everything a payee had outstanding as paid (or collected).
 * No money moves in the system; this is the audit record. {@code netAmount} is always positive and
 * {@code direction} says who paid whom.
 */
public class Settlement {

    private UUID id;
    private PayeeType payeeType;
    private UUID payeeUserId;
    private SettlementDirection direction;
    private int orderCount;
    private BigDecimal grossAmount;
    private BigDecimal platformFeeAmount;
    private BigDecimal cashCollectedAmount;
    private BigDecimal netAmount;
    private String note;
    private String settledBy;
    private OffsetDateTime settledAt;
    private List<SettlementLine> lines = List.of();

    public Settlement() {}

    public static Settlement create(PayeeType payeeType, UUID payeeUserId, List<SettlementLine> lines,
                                    String note, String settledBy) {
        SettlementPolicy.Totals totals = SettlementPolicy.totals(lines);
        Settlement s = new Settlement();
        s.id = UUID.randomUUID();
        s.payeeType = payeeType;
        s.payeeUserId = payeeUserId;
        s.direction = totals.direction();
        s.orderCount = totals.orderCount();
        s.grossAmount = totals.grossAmount();
        s.platformFeeAmount = totals.platformFeeAmount();
        s.cashCollectedAmount = totals.cashCollectedAmount();
        s.netAmount = totals.netAmount();
        s.note = note;
        s.settledBy = settledBy;
        s.settledAt = OffsetDateTime.now();
        s.lines = List.copyOf(lines);
        return s;
    }

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
    public List<SettlementLine> getLines() { return lines; }
    public void setLines(List<SettlementLine> lines) { this.lines = lines == null ? List.of() : lines; }
}