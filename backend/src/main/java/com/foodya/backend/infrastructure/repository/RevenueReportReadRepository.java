package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.OrderPersistenceModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface RevenueReportReadRepository extends Repository<OrderPersistenceModel, UUID> {

    @Query(value = """
            SELECT
                CAST(o.placed_at AS DATE) AS period,
                SUM(o.total_amount) AS revenue,
                SUM(o.platform_profit_amount) AS platformProfit,
                COUNT(*) AS orderCount
            FROM orders o
            WHERE o.status = 'SUCCESS'
              AND o.placed_at >= :fromInclusive
              AND o.placed_at < :toExclusive
            GROUP BY CAST(o.placed_at AS DATE)
            ORDER BY CAST(o.placed_at AS DATE)
            """, nativeQuery = true)
    List<RevenueBucketRow> findPlatformRevenueBuckets(@Param("fromInclusive") OffsetDateTime fromInclusive,
                                                      @Param("toExclusive") OffsetDateTime toExclusive);

    @Query(value = """
            SELECT
                CAST(o.placed_at AS DATE) AS period,
                SUM(o.total_amount) AS revenue,
                SUM(o.platform_profit_amount) AS platformProfit,
                COUNT(*) AS orderCount
            FROM orders o
            JOIN restaurants r ON r.id = o.restaurant_id
            WHERE o.status = 'SUCCESS'
              AND r.owner_user_id = :merchantUserId
              AND o.placed_at >= :fromInclusive
              AND o.placed_at < :toExclusive
            GROUP BY CAST(o.placed_at AS DATE)
            ORDER BY CAST(o.placed_at AS DATE)
            """, nativeQuery = true)
    List<RevenueBucketRow> findMerchantRevenueBuckets(@Param("merchantUserId") UUID merchantUserId,
                                                      @Param("fromInclusive") OffsetDateTime fromInclusive,
                                                      @Param("toExclusive") OffsetDateTime toExclusive);

    @Query(value = """
            SELECT
              CAST(oi.menu_item_id AS VARCHAR) AS menuItemId,
                oi.menu_item_name_snapshot AS itemName,
                SUM(oi.quantity) AS quantitySold,
                SUM(oi.line_total) AS revenue
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            JOIN restaurants r ON r.id = o.restaurant_id
            WHERE o.status = 'SUCCESS'
              AND r.owner_user_id = :merchantUserId
              AND o.placed_at >= :fromInclusive
              AND o.placed_at < :toExclusive
            GROUP BY oi.menu_item_id, oi.menu_item_name_snapshot
            ORDER BY SUM(oi.quantity) DESC, SUM(oi.line_total) DESC
            """, nativeQuery = true)
    List<TopSellingItemRow> findMerchantTopSellingItems(@Param("merchantUserId") UUID merchantUserId,
                                                        @Param("fromInclusive") OffsetDateTime fromInclusive,
                                                        @Param("toExclusive") OffsetDateTime toExclusive);

    interface RevenueBucketRow {
        LocalDate getPeriod();

        BigDecimal getRevenue();

        BigDecimal getPlatformProfit();

        long getOrderCount();
    }

    interface TopSellingItemRow {
      String getMenuItemId();

        String getItemName();

        long getQuantitySold();

        BigDecimal getRevenue();
    }
}
