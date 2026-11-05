package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.DriverEarningsView;
import com.foodya.backend.application.ports.in.DriverEarningsUseCase;
import com.foodya.backend.application.ports.out.OrderManagementPort;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.value_objects.PaymentMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Driver earnings derived from completed orders. The driver keeps {@code deliveryFee - shippingFeeMargin};
 * the margin is snapshotted on every order at checkout (finance.shipping_margin_rate_percent), so changing the
 * rate later never rewrites past earnings. Tips and bonuses are not modelled.
 */
public class DriverEarningsService implements DriverEarningsUseCase {

    private final OrderManagementPort orderManagementPort;

    public DriverEarningsService(OrderManagementPort orderManagementPort) {
        this.orderManagementPort = orderManagementPort;
    }

    @Override
    public DriverEarningsView driverEarnings(UUID driverUserId, OffsetDateTime from, OffsetDateTime to) {
        List<Order> orders = orderManagementPort.findCompletedByDriverUserId(driverUserId, from, to);
        List<DriverEarningsView.Entry> entries = orders.stream().map(DriverEarningsService::toEntry).toList();

        BigDecimal total = entries.stream()
                .map(DriverEarningsView.Entry::driverEarning)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cod = orders.stream()
                .filter(o -> o.getPaymentMethod() == PaymentMethod.COD && o.getTotalAmount() != null)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DriverEarningsView(from, to, total, cod, entries.size(), entries);
    }

    private static DriverEarningsView.Entry toEntry(Order order) {
        BigDecimal fee = order.getDeliveryFee() == null ? BigDecimal.ZERO : order.getDeliveryFee();
        BigDecimal margin = order.getShippingFeeMarginAmount() == null ? BigDecimal.ZERO : order.getShippingFeeMarginAmount();
        BigDecimal earning = fee.subtract(margin).max(BigDecimal.ZERO);
        return new DriverEarningsView.Entry(order.getId(), order.getOrderCode(), order.getCompletedAt(),
                fee, margin, earning, order.getPaymentMethod() == null ? null : order.getPaymentMethod().name());
    }
}
