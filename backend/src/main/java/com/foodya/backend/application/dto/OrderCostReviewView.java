package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderCostReviewView(
        BigDecimal subtotalAmount,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        BigDecimal commissionAmount,
        BigDecimal shippingFeeMarginAmount,
        BigDecimal platformProfitAmount,
        String currencyCode,
        List<FeeComponentView> feeBreakdown
) {
    public record FeeComponentView(String name, String label, BigDecimal amount) {}
}

