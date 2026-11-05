package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.PayeeType;
import com.foodya.backend.domain.value_objects.SettlementDirection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A recorded settlement in the history list (no lines). */
public record SettlementView(
        UUID settlementId,
        PayeeType payeeType,
        UUID payeeUserId,
        String payeeName,
        SettlementDirection direction,
        int orderCount,
        BigDecimal grossAmount,
        BigDecimal platformFeeAmount,
        BigDecimal cashCollectedAmount,
        BigDecimal netAmount,
        String note,
        String settledBy,
        OffsetDateTime settledAt
) {}
