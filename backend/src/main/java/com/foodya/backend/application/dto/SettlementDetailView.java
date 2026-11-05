package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A recorded settlement with every contributing order. */
public record SettlementDetailView(SettlementView settlement, List<Line> lines) {
    public record Line(UUID orderId, String orderCode, BigDecimal grossAmount,
                       BigDecimal platformFeeAmount, BigDecimal cashCollectedAmount, BigDecimal netAmount) {}
}
