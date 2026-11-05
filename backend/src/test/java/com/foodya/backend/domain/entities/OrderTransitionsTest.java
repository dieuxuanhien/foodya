package com.foodya.backend.domain.entities;

import com.foodya.backend.domain.value_objects.DriverSessionStatus;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Order state machine rules added to close the merchant/driver/cancel gaps. */
class OrderTransitionsTest {

    private static Order order(OrderStatus status) {
        Order o = Order.builder()
                .orderCode("ORD-1")
                .customerUserId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .status(status)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();
        o.setId(UUID.randomUUID());
        return o;
    }

    @Nested
    class MerchantReject {

        @Test
        void allowsRejectBeforePickupAndRecordsReason() {
            for (OrderStatus s : new OrderStatus[]{OrderStatus.PENDING, OrderStatus.ACCEPTED,
                    OrderStatus.ASSIGNED, OrderStatus.PREPARING}) {
                Order o = order(s);
                o.merchantReject("out of stock");
                assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                assertThat(o.getCancelReason()).isEqualTo("out of stock");
            }
        }

        @Test
        void blocksRejectOnceDelivering() {
            Order o = order(OrderStatus.DELIVERING);
            assertThatThrownBy(() -> o.merchantReject("too late")).isInstanceOf(IllegalStateException.class);
            assertThat(o.getStatus()).isEqualTo(OrderStatus.DELIVERING);
        }
    }

    @Nested
    class DriverPickup {

        @Test
        void preparingOrderCanBePickedUp_merchantMarkedItBeforeAssignment() {
            Order o = order(OrderStatus.PREPARING);
            o.deliveryStart();
            assertThat(o.getStatus()).isEqualTo(OrderStatus.DELIVERING);
        }

        @Test
        void assignedOrderCanBePickedUp_driverSkippedMerchantStep() {
            Order o = order(OrderStatus.ASSIGNED);
            o.deliveryStart();
            assertThat(o.getStatus()).isEqualTo(OrderStatus.DELIVERING);
        }

        @Test
        void cannotPickUpBeforeAssignment() {
            assertThatThrownBy(() -> order(OrderStatus.ACCEPTED).deliveryStart())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class DriverRelease {

        @Test
        void releaseClearsDriverBatchAndReturnsToPool() {
            Order o = order(OrderStatus.ASSIGNED);
            UUID driverId = UUID.randomUUID();
            o.setDriverUserId(driverId);
            o.setBatchId(UUID.randomUUID());
            o.setBatchPickupIndex(1);

            o.releaseDriver();

            assertThat(o.getDriverUserId()).isNull();
            assertThat(o.getBatchId()).isNull();
            assertThat(o.getBatchPickupIndex()).isNull();
            assertThat(o.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        }

        @Test
        void preparingOrderCanStillBeReleasedBeforePickup() {
            Order o = order(OrderStatus.PREPARING);
            o.setDriverUserId(UUID.randomUUID());
            o.releaseDriver();
            assertThat(o.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        }

        @Test
        void cannotReleaseAfterPickup() {
            Order o = order(OrderStatus.DELIVERING);
            o.setDriverUserId(UUID.randomUUID());
            assertThatThrownBy(o::releaseDriver).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class SystemCancel {

        @Test
        void allowedUpToPreparing() {
            for (OrderStatus s : new OrderStatus[]{OrderStatus.PENDING, OrderStatus.ACCEPTED,
                    OrderStatus.ASSIGNED, OrderStatus.PREPARING}) {
                Order o = order(s);
                o.cancelBySystem("timed out");
                assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            }
        }

        @Test
        void blockedWhileDelivering() {
            Order o = order(OrderStatus.DELIVERING);
            assertThatThrownBy(() -> o.cancelBySystem("oops")).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class RefundedPayment {

        @Test
        void markedOnlyWhenPaid() {
            Order o = order(OrderStatus.CANCELLED);
            o.setPaymentStatus(PaymentStatus.PAID);
            o.markRefunded();
            assertThat(o.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        void rejectsUnpaidOrder() {
            Order o = order(OrderStatus.CANCELLED);
            assertThatThrownBy(o::markRefunded).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class DriverSessionSlots {

        @Test
        void releaseSlotReturnsDriverToOnlineWhenLastOrderEnds() {
            DriverOnlineSession s = new DriverOnlineSession();
            s.goOnline(UUID.randomUUID());
            s.startDelivery();
            assertThat(s.getStatus()).isEqualTo(DriverSessionStatus.ON_DELIVERY);

            s.releaseSlot();
            assertThat(s.getStatus()).isEqualTo(DriverSessionStatus.ONLINE);
            assertThat(s.getActiveOrderCount()).isZero();
        }

        @Test
        void releaseSlotHonoursLastTripMode() {
            DriverOnlineSession s = new DriverOnlineSession();
            s.goOnline(UUID.randomUUID());
            s.startDelivery();
            s.requestOffline();
            assertThat(s.getStatus()).isEqualTo(DriverSessionStatus.GOING_OFFLINE);

            s.releaseSlot();
            assertThat(s.getStatus()).isEqualTo(DriverSessionStatus.OFFLINE);
            assertThat(s.getEndedAt()).isNotNull();
        }

        @Test
        void releaseSlotIsNoOpOnIdleSession() {
            DriverOnlineSession s = new DriverOnlineSession();
            s.goOnline(UUID.randomUUID());
            s.releaseSlot();
            assertThat(s.getStatus()).isEqualTo(DriverSessionStatus.ONLINE);
            assertThat(s.getActiveOrderCount()).isZero();
        }

        @Test
        void expireClosesTheSession() {
            DriverOnlineSession s = new DriverOnlineSession();
            s.goOnline(UUID.randomUUID());
            s.startDelivery();
            s.expire();
            assertThat(s.getStatus()).isEqualTo(DriverSessionStatus.OFFLINE);
            assertThat(s.getActiveOrderCount()).isZero();
        }
    }
}