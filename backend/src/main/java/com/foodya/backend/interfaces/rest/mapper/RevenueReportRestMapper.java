package com.foodya.backend.interfaces.rest.mapper;

import com.foodya.backend.application.dto.RevenueReportView;
import com.foodya.backend.interfaces.rest.dto.RevenueBucketResponse;
import com.foodya.backend.interfaces.rest.dto.RevenueReportResponse;
import com.foodya.backend.interfaces.rest.dto.TopSellingItemResponse;

public final class RevenueReportRestMapper {

    private RevenueReportRestMapper() {
    }

    public static RevenueReportResponse toResponse(RevenueReportView view) {
        return new RevenueReportResponse(
                view.fromDate(),
                view.toDate(),
                view.revenue(),
                view.platformProfit(),
                view.orderCount(),
                view.avgOrderValue(),
                view.series().stream()
                        .map(bucket -> new RevenueBucketResponse(
                                bucket.period(),
                                bucket.revenue(),
                                bucket.platformProfit(),
                                bucket.orderCount(),
                                bucket.avgOrderValue()
                        ))
                        .toList(),
                view.topSellingItems().stream()
                        .map(item -> new TopSellingItemResponse(
                                item.menuItemId(),
                                item.itemName(),
                                item.quantitySold(),
                                item.revenue()
                        ))
                        .toList()
        );
    }
}
