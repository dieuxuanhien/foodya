package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueBucketResponse(
        LocalDate period,
        BigDecimal revenue,
        BigDecimal platformProfit,
        long orderCount,
        BigDecimal avgOrderValue
) {
}
