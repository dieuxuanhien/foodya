package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;

public record OrderCostReviewResponse(
        BigDecimal subtotalAmount,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        BigDecimal commissionAmount,
        BigDecimal shippingFeeMarginAmount,
        BigDecimal platformProfitAmount,
        String currencyCode
) {
}
