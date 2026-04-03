package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.RevenueBucketView;
import com.foodya.backend.application.dto.RevenueReportView;
import com.foodya.backend.application.dto.TopSellingItemView;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.RevenueReportUseCase;
import com.foodya.backend.application.ports.out.RevenueReportPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RevenueReportService implements RevenueReportUseCase {

    private static final int DEFAULT_TOP_ITEMS = 5;
    private static final int MAX_TOP_ITEMS = 20;

    private final RevenueReportPort revenueReportPort;

    public RevenueReportService(RevenueReportPort revenueReportPort) {
        this.revenueReportPort = revenueReportPort;
    }

    @Transactional(readOnly = true)
    public RevenueReportView platformRevenueReport(LocalDate from, LocalDate to) {
        DateRange range = normalizeRange(from, to);
        List<RevenueBucketView> buckets = revenueReportPort
                .fetchPlatformRevenueBuckets(range.fromInclusive(), range.toExclusive())
                .stream()
                .map(this::toBucket)
                .toList();

        return buildReport(range.fromDate(), range.toDate(), buckets, List.of());
    }

    @Transactional(readOnly = true)
    public RevenueReportView merchantRevenueReport(UUID merchantUserId,
                                                   LocalDate from,
                                                   LocalDate to,
                                                   Integer topItemsLimit) {
        DateRange range = normalizeRange(from, to);
        int limit = normalizeTopItemsLimit(topItemsLimit);

        List<RevenueBucketView> buckets = revenueReportPort
                .fetchMerchantRevenueBuckets(merchantUserId, range.fromInclusive(), range.toExclusive())
                .stream()
                .map(this::toBucket)
                .toList();

        List<TopSellingItemView> topItems = revenueReportPort
                .fetchMerchantTopSellingItems(merchantUserId, range.fromInclusive(), range.toExclusive(), limit)
                .stream()
                .map(item -> new TopSellingItemView(item.menuItemId(), item.itemName(), item.quantitySold(), item.revenue()))
                .toList();

        return buildReport(range.fromDate(), range.toDate(), buckets, topItems);
    }

    private RevenueBucketView toBucket(RevenueReportPort.RevenueBucketProjection projection) {
        BigDecimal revenue = safeAmount(projection.revenue());
        BigDecimal platformProfit = safeAmount(projection.platformProfit());
        long orderCount = projection.orderCount();
        BigDecimal avg = orderCount == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);

        return new RevenueBucketView(projection.period(), revenue, platformProfit, orderCount, avg);
    }

    private RevenueReportView buildReport(LocalDate fromDate,
                                          LocalDate toDate,
                                          List<RevenueBucketView> buckets,
                                          List<TopSellingItemView> topItems) {
        BigDecimal revenue = buckets.stream()
                .map(RevenueBucketView::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal platformProfit = buckets.stream()
                .map(RevenueBucketView::platformProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long orderCount = buckets.stream()
                .mapToLong(RevenueBucketView::orderCount)
                .sum();
        BigDecimal avgOrderValue = orderCount == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);

        return new RevenueReportView(fromDate, toDate, revenue, platformProfit, orderCount, avgOrderValue, buckets, topItems);
    }

    private DateRange normalizeRange(LocalDate from, LocalDate to) {
        LocalDate toDate = to == null ? LocalDate.now(ZoneOffset.UTC) : to;
        LocalDate fromDate = from == null ? toDate.minusDays(29) : from;

        if (fromDate.isAfter(toDate)) {
            throw new ValidationException("invalid date range", Map.of("from", "must be less than or equal to to"));
        }

        OffsetDateTime fromInclusive = fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toExclusive = toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        return new DateRange(fromDate, toDate, fromInclusive, toExclusive);
    }

    private int normalizeTopItemsLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_TOP_ITEMS;
        }
        if (limit < 1 || limit > MAX_TOP_ITEMS) {
            throw new ValidationException("invalid topItems", Map.of("topItems", "must be between 1 and 20"));
        }
        return limit;
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private record DateRange(LocalDate fromDate,
                             LocalDate toDate,
                             OffsetDateTime fromInclusive,
                             OffsetDateTime toExclusive) {
    }
}
