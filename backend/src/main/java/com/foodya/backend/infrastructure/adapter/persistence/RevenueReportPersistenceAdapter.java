package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.ports.out.RevenueReportPort;
import com.foodya.backend.infrastructure.repository.RevenueReportReadRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class RevenueReportPersistenceAdapter implements RevenueReportPort {

    private final RevenueReportReadRepository repository;

    public RevenueReportPersistenceAdapter(RevenueReportReadRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RevenueBucketProjection> fetchPlatformRevenueBuckets(OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
        return repository.findPlatformRevenueBuckets(fromInclusive, toExclusive)
                .stream()
                .map(row -> (RevenueBucketProjection) new RevenueBucketProjectionImpl(
                        row.getPeriod(),
                        row.getRevenue(),
                        row.getPlatformProfit(),
                        row.getOrderCount()
                ))
                .toList();
    }

    @Override
    public List<RevenueBucketProjection> fetchMerchantRevenueBuckets(UUID merchantUserId,
                                                                     OffsetDateTime fromInclusive,
                                                                     OffsetDateTime toExclusive) {
        return repository.findMerchantRevenueBuckets(merchantUserId, fromInclusive, toExclusive)
                .stream()
                .map(row -> (RevenueBucketProjection) new RevenueBucketProjectionImpl(
                        row.getPeriod(),
                        row.getRevenue(),
                        row.getPlatformProfit(),
                        row.getOrderCount()
                ))
                .toList();
    }

    @Override
    public List<TopSellingItemProjection> fetchMerchantTopSellingItems(UUID merchantUserId,
                                                                        OffsetDateTime fromInclusive,
                                                                        OffsetDateTime toExclusive,
                                                                        int limit) {
        return repository.findMerchantTopSellingItems(
                        merchantUserId,
                        fromInclusive,
                        toExclusive
                )
                .stream()
                .limit(limit)
                .map(row -> (TopSellingItemProjection) new TopSellingItemProjectionImpl(
                        UUID.fromString(row.getMenuItemId()),
                        row.getItemName(),
                        row.getQuantitySold(),
                        row.getRevenue()
                ))
                .toList();
    }

    private record RevenueBucketProjectionImpl(LocalDate period,
                                               BigDecimal revenue,
                                               BigDecimal platformProfit,
                                               long orderCount) implements RevenueBucketProjection {
    }

    private record TopSellingItemProjectionImpl(UUID menuItemId,
                                                String itemName,
                                                long quantitySold,
                                                BigDecimal revenue) implements TopSellingItemProjection {
    }
}
