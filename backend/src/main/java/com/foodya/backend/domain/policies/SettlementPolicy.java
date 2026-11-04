package com.foodya.backend.domain.policies;

import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PayeeType;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import com.foodya.backend.domain.value_objects.SettlementDirection;
import com.foodya.backend.domain.value_objects.SettlementLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Money rules for settling completed orders. All amounts come from values snapshotted on the order at
 * checkout, so changing a fee rate later never rewrites what was already earned.
 *
 * <ul>
 *   <li><b>Merchant</b>: gets {@code subtotal - commission}, whether the customer paid online or in cash.</li>
 *   <li><b>Driver</b>: earns {@code deliveryFee - shippingMargin}. On a COD order the driver also holds the
 *       whole order total in cash, so that cash is subtracted from what the platform owes them. The result
 *       is a payout when earnings exceed the cash held and a collection otherwise.</li>
 * </ul>
 */
public final class SettlementPolicy {

    private static final int SCALE = 2;

    private SettlementPolicy() {}

    /** Sum of a set of lines. {@code signedNet} keeps the sign; {@link #netAmount()} is always positive. */
    public record Totals(int orderCount, BigDecimal grossAmount, BigDecimal platformFeeAmount,
                         BigDecimal cashCollectedAmount, BigDecimal signedNet) {

        public SettlementDirection direction() {
            return signedNet.signum() >= 0 ? SettlementDirection.PAYOUT : SettlementDirection.COLLECTION;
        }

        public BigDecimal netAmount() {
            return signedNet.abs();
        }
    }

    /**
     * Whether the order counts towards the payee's balance. Only delivered orders count. A merchant is not
     * paid for an order whose customer was refunded; a driver is still paid, because the delivery happened.
     */
    public static boolean isPayable(Order order, PayeeType payeeType) {
        if (order == null || order.getStatus() != OrderStatus.SUCCESS) {
            return false;
        }
        return switch (payeeType) {
            case MERCHANT -> order.getPaymentStatus() != PaymentStatus.REFUNDED;
            case DRIVER -> order.getDriverUserId() != null;
        };
    }

    public static SettlementLine lineFor(Order order, PayeeType payeeType) {
        return switch (payeeType) {
            case MERCHANT -> merchantLine(order);
            case DRIVER -> driverLine(order);
        };
    }

    public static Totals totals(List<SettlementLine> lines) {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        for (SettlementLine line : lines) {
            gross = gross.add(line.grossAmount());
            fee = fee.add(line.platformFeeAmount());
            cash = cash.add(line.cashCollectedAmount());
            net = net.add(line.netAmount());
        }
        return new Totals(lines.size(), money(gross), money(fee), money(cash), money(net));
    }

    private static SettlementLine merchantLine(Order order) {
        BigDecimal gross = money(order.getSubtotalAmount());
        BigDecimal commission = money(order.getCommissionAmount());
        return new SettlementLine(order.getId(), order.getOrderCode(), gross, commission,
                money(null), gross.subtract(commission));
    }

    private static SettlementLine driverLine(Order order) {
        BigDecimal gross = money(order.getDeliveryFee());
        BigDecimal margin = money(order.getShippingFeeMarginAmount());
        BigDecimal earning = gross.subtract(margin);
        BigDecimal cash = order.getPaymentMethod() == PaymentMethod.COD ? money(order.getTotalAmount()) : money(null);
        return new SettlementLine(order.getId(), order.getOrderCode(), gross, margin, cash, earning.subtract(cash));
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(SCALE, RoundingMode.HALF_UP);
    }
}