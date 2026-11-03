package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderCostReviewResponse(
        BigDecimal subtotalAmount,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        BigDecimal commissionAmount,
        BigDecimal shippingFeeMarginAmount,
        BigDecimal platformProfitAmount,
        String currencyCode,
        List<FeeComponent> feeBreakdown
) {
    public record FeeComponent(String name, String label, BigDecimal amount) {}
}

