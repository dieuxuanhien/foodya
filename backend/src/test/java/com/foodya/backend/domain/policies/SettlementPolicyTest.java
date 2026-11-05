package com.foodya.backend.domain.policies;

import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PayeeType;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import com.foodya.backend.domain.value_objects.SettlementDirection;
import com.foodya.backend.domain.value_objects.SettlementLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Money rules for payouts. Worked example used throughout:
 * a 100,000₫ food order with a 15,000₫ delivery fee and 15% commission.
 */
class SettlementPolicyTest {

    private static Order order(OrderStatus status, PaymentMethod method) {
        Order o = Order.builder()
                .orderCode("ORD-1")
                .customerUserId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .driverUserId(UUID.randomUUID())
                .status(status)
                .paymentMethod(method)
                .paymentStatus(PaymentStatus.PAID)
                .subtotalAmount(new BigDecimal("100000"))
                .deliveryFee(new BigDecimal("15000"))
                .totalAmount(new BigDecimal("115000"))
                .commissionAmount(new BigDecimal("15000"))
                .shippingFeeMarginAmount(BigDecimal.ZERO)
                .build();
        o.setId(UUID.randomUUID());
        return o;
    }

    @Test
    void merchantGetsSubtotalMinusCommission() {
        SettlementLine line = SettlementPolicy.lineFor(order(OrderStatus.SUCCESS, PaymentMethod.VNPAY), PayeeType.MERCHANT);

        assertThat(line.grossAmount()).isEqualByComparingTo("100000");
        assertThat(line.platformFeeAmount()).isEqualByComparingTo("15000");
        assertThat(line.cashCollectedAmount()).isEqualByComparingTo("0");
        assertThat(line.netAmount()).isEqualByComparingTo("85000");
    }

    @Test
    void driverOnAPaidOnlineOrderIsOwedTheirFullEarning() {
        SettlementLine line = SettlementPolicy.lineFor(order(OrderStatus.SUCCESS, PaymentMethod.VNPAY), PayeeType.DRIVER);

        assertThat(line.grossAmount()).isEqualByComparingTo("15000");
        assertThat(line.cashCollectedAmount()).isEqualByComparingTo("0");
        assertThat(line.netAmount()).isEqualByComparingTo("15000");
    }

    @Test
    void driverOnACodOrderOwesTheCashTheyHold() {
        SettlementLine line = SettlementPolicy.lineFor(order(OrderStatus.SUCCESS, PaymentMethod.COD), PayeeType.DRIVER);

        // earning 15,000 but holding 115,000 cash -> the driver owes the platform 100,000
        assertThat(line.cashCollectedAmount()).isEqualByComparingTo("115000");
        assertThat(line.netAmount()).isEqualByComparingTo("-100000");
    }

    @Test
    void aShippingMarginReducesTheDriverShare() {
        Order o = order(OrderStatus.SUCCESS, PaymentMethod.VNPAY);
        o.setShippingFeeMarginAmount(new BigDecimal("2000"));

        SettlementLine line = SettlementPolicy.lineFor(o, PayeeType.DRIVER);

        assertThat(line.platformFeeAmount()).isEqualByComparingTo("2000");
        assertThat(line.netAmount()).isEqualByComparingTo("13000");
    }

    @Test
    void totalsDecideTheDirection() {
        SettlementLine payout = SettlementPolicy.lineFor(order(OrderStatus.SUCCESS, PaymentMethod.VNPAY), PayeeType.DRIVER);
        SettlementPolicy.Totals paid = SettlementPolicy.totals(List.of(payout));
        assertThat(paid.direction()).isEqualTo(SettlementDirection.PAYOUT);
        assertThat(paid.netAmount()).isEqualByComparingTo("15000");

        SettlementLine cash = SettlementPolicy.lineFor(order(OrderStatus.SUCCESS, PaymentMethod.COD), PayeeType.DRIVER);
        SettlementPolicy.Totals collected = SettlementPolicy.totals(List.of(cash));
        assertThat(collected.direction()).isEqualTo(SettlementDirection.COLLECTION);
        assertThat(collected.netAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void cashAndEarningsInTheSameSettlementNetOff() {
        SettlementLine paidEarning = SettlementPolicy.lineFor(order(OrderStatus.SUCCESS, PaymentMethod.VNPAY), PayeeType.DRIVER);
        SettlementLine codOrder = SettlementPolicy.lineFor(order(OrderStatus.SUCCESS, PaymentMethod.COD), PayeeType.DRIVER);

        SettlementPolicy.Totals net = SettlementPolicy.totals(List.of(paidEarning, codOrder));

        // 15,000 earned online + 15,000 earned on COD - 115,000 cash held
        assertThat(net.signedNet()).isEqualByComparingTo("-85000");
        assertThat(net.direction()).isEqualTo(SettlementDirection.COLLECTION);
        assertThat(net.cashCollectedAmount()).isEqualByComparingTo("115000");
    }

    @Test
    void onlyDeliveredOrdersCountAndRefundedOrdersSkipTheMerchant() {
        PaymentStatus refunded = PaymentStatus.REFUNDED;
        Order cancelled = order(OrderStatus.CANCELLED, PaymentMethod.VNPAY);
        Order refundedOrder = order(OrderStatus.SUCCESS, PaymentMethod.VNPAY);
        refundedOrder.setPaymentStatus(refunded);
        Order delivered = order(OrderStatus.SUCCESS, PaymentMethod.VNPAY);

        assertThat(SettlementPolicy.isPayable(cancelled, PayeeType.MERCHANT)).isFalse();
        assertThat(SettlementPolicy.isPayable(refundedOrder, PayeeType.MERCHANT)).isFalse();
        assertThat(SettlementPolicy.isPayable(delivered, PayeeType.MERCHANT)).isTrue();
        // the driver still delivered the refunded order, so they are paid for it
        assertThat(SettlementPolicy.isPayable(refundedOrder, PayeeType.DRIVER)).isTrue();
    }

    @Test
    void ordersWithoutADriverNeverPayADriver() {
        Order noDriver = order(OrderStatus.SUCCESS, PaymentMethod.VNPAY);
        noDriver.setDriverUserId(null);

        assertThat(SettlementPolicy.isPayable(noDriver, PayeeType.DRIVER)).isFalse();
    }
}