package com.foodya.backend.domain.value_objects;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One order's contribution to a settlement. {@code netAmount} is signed: positive means the platform
 * owes the payee for this order, negative means the payee owes the platform (a driver holding COD cash).
 */
public record SettlementLine(
        UUID orderId,
        String orderCode,
        BigDecimal grossAmount,
        BigDecimal platformFeeAmount,
        BigDecimal cashCollectedAmount,
        BigDecimal netAmount
) {}