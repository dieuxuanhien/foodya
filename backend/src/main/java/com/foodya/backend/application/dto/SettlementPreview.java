package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.PayeeType;
import com.foodya.backend.domain.value_objects.SettlementDirection;

import java.math.BigDecimal;
import java.util.UUID;

/** What one payee is owed right now, before the admin clicks Done. {@code netAmount} is always positive. */
public record SettlementPreview(
        PayeeType payeeType,
        UUID payeeUserId,
        String payeeName,
        SettlementDirection direction,
        int orderCount,
        BigDecimal grossAmount,
        BigDecimal platformFeeAmount,
        BigDecimal cashCollectedAmount,
        BigDecimal netAmount
) {}
