package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Earnings for one driver over a period. Each delivery credits the driver with the delivery fee minus the
 * platform's shipping margin recorded on the order, so historical orders keep the rate they were created with.
 *
 * @param codCashCollected total order value the driver collected in cash on COD orders in the period
 */
public record DriverEarningsView(
        OffsetDateTime from,
        OffsetDateTime to,
        BigDecimal totalEarnings,
        BigDecimal codCashCollected,
        int deliveryCount,
        List<Entry> entries
) {
    public record Entry(
            UUID orderId,
            String orderCode,
            OffsetDateTime completedAt,
            BigDecimal deliveryFee,
            BigDecimal platformMargin,
            BigDecimal driverEarning,
            String paymentMethod
    ) {}
}
