package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueReportView(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal revenue,
        BigDecimal platformProfit,
        long orderCount,
        BigDecimal avgOrderValue,
        List<RevenueBucketView> series,
        List<TopSellingItemView> topSellingItems
) {
}
