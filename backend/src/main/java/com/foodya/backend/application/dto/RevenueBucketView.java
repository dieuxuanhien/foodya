package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueBucketView(
        LocalDate period,
        BigDecimal revenue,
        BigDecimal platformProfit,
        long orderCount,
        BigDecimal avgOrderValue
) {
}
