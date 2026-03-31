package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal revenue,
        BigDecimal platformProfit,
        long orderCount,
        BigDecimal avgOrderValue,
        List<RevenueBucketResponse> series,
        List<TopSellingItemResponse> topSellingItems
) {
}
