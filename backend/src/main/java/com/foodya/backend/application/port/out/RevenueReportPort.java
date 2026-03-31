package com.foodya.backend.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface RevenueReportPort {

    List<RevenueBucketProjection> fetchPlatformRevenueBuckets(OffsetDateTime fromInclusive, OffsetDateTime toExclusive);

    List<RevenueBucketProjection> fetchMerchantRevenueBuckets(UUID merchantUserId,
                                                              OffsetDateTime fromInclusive,
                                                              OffsetDateTime toExclusive);

    List<TopSellingItemProjection> fetchMerchantTopSellingItems(UUID merchantUserId,
                                                                OffsetDateTime fromInclusive,
                                                                OffsetDateTime toExclusive,
                                                                int limit);

    interface RevenueBucketProjection {
        LocalDate period();

        BigDecimal revenue();

        BigDecimal platformProfit();

        long orderCount();
    }

    interface TopSellingItemProjection {
        UUID menuItemId();

        String itemName();

        long quantitySold();

        BigDecimal revenue();
    }
}
