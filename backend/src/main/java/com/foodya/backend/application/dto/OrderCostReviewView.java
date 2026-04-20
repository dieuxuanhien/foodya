package com.foodya.backend.application.dto;

import java.math.BigDecimal;

public record OrderCostReviewView(
        BigDecimal subtotalAmount,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        BigDecimal commissionAmount,
        BigDecimal shippingFeeMarginAmount,
        BigDecimal platformProfitAmount,
        String currencyCode
) {
}
