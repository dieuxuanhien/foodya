package com.foodya.backend.application.usecases;

import com.foodya.backend.application.ports.in.PaymentUseCase;
import com.foodya.backend.application.ports.out.DriverOnlineSessionPort;
import com.foodya.backend.application.ports.out.OrderBatchPort;
import com.foodya.backend.application.ports.out.OrderEventPublisherPort;
import com.foodya.backend.application.ports.out.OrderManagementPort;
import com.foodya.backend.application.ports.out.OrderPaymentPort;
import com.foodya.backend.application.ports.out.RestaurantPort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.domain.entities.DriverOnlineSession;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.value_objects.DriverSessionStatus;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.PaymentMethod;
import com.foodya.backend.domain.value_objects.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderClosureServiceTest {

    private OrderManagementPort orderManagementPort;
    private OrderBatchPort orderBatchPort;
    private DriverOnlineSessionPort driverOnlineSessionPort;
    private PaymentUseCase paymentUseCase;
    private SystemParameterPort systemParameterPort;
    private OrderClosureService service;

    @BeforeEach
    void setUp() {
        orderManagementPort = mock(OrderManagementPort.class);
        orderBatchPort = mock(OrderBatchPort.class);
        driverOnlineSessionPort = mock(DriverOnlineSessionPort.class);
        paymentUseCase = mock(PaymentUseCase.class);
        systemParameterPort = mock(SystemParameterPort.class);
        when(systemParameterPort.findById(anyString())).thenReturn(Optional.empty());
        when(orderManagementPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new OrderClosureService(orderManagementPort, orderBatchPort, driverOnlineSessionPort,
                mock(OrderPaymentPort.class), mock(RestaurantPort.class), mock(OrderEventPublisherPort.class),
                paymentUseCase, systemParameterPort);
    }

    private Order order(OrderStatus status, PaymentStatus paymentStatus) {
        Order o = Order.builder()
                .orderCode("ORD-1")
                .customerUserId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .status(status)
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(paymentStatus)
                .placedAt(OffsetDateTime.now().minusHours(1))
                .build();
        o.setId(UUID.randomUUID());
        return o;
    }

    private DriverOnlineSession busySession(UUID driverId) {
        DriverOnlineSession s = new DriverOnlineSession();
        s.setId(UUID.randomUUID());
        s.goOnline(driverId);
        s.startDelivery();
        return s;
    }

    @Test
    void onClosedRefundsAPaidOrderAndFreesTheDriver() {
        UUID driverId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        Order o = order(OrderStatus.CANCELLED, PaymentStatus.PAID);
        DriverOnlineSession session = busySession(driverId);
        when(driverOnlineSessionPort.findActiveSession(driverId)).thenReturn(Optional.of(session));
        when(paymentUseCase.refundOrder(eq(o.getId()), anyString(), anyString(), anyString()))
                .thenReturn(new PaymentUseCase.PaymentRefundView(true, "REFUNDED", "refunded"));

        boolean refunded = service.onClosed(o, batchId, driverId, "customer", "changed my mind");

        assertThat(refunded).isTrue();
        verify(orderBatchPort).failBatchItem(batchId, o.getId());
        assertThat(session.getStatus()).isEqualTo(DriverSessionStatus.ONLINE);
        assertThat(session.getActiveOrderCount()).isZero();
    }

    @Test
    void onClosedDoesNotTouchPaymentForUnpaidOrders() {
        Order o = order(OrderStatus.CANCELLED, PaymentStatus.UNPAID);

        boolean refunded = service.onClosed(o, null, null, "merchant", "closed");

        assertThat(refunded).isFalse();
        verify(paymentUseCase, never()).refundOrder(any(), anyString(), anyString(), anyString());
    }

    @Test
    void aFailingRefundDoesNotBreakTheClosure() {
        Order o = order(OrderStatus.CANCELLED, PaymentStatus.PAID);
        when(paymentUseCase.refundOrder(any(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("gateway down"));

        boolean refunded = service.onClosed(o, null, null, "system", "timeout");

        assertThat(refunded).isFalse(); // the retry sweep will pick it up
    }

    @Test
    void timedOutPendingOrdersAreCancelledAndRefunded() {
        Order stale = order(OrderStatus.PENDING, PaymentStatus.PAID);
        when(orderManagementPort.findStaleByStatus(eq(OrderStatus.PENDING), any())).thenReturn(List.of(stale));
        when(orderManagementPort.findStaleByStatus(eq(OrderStatus.ACCEPTED), any())).thenReturn(List.of());
        when(orderManagementPort.findById(stale.getId())).thenReturn(Optional.of(stale));
        when(paymentUseCase.refundOrder(eq(stale.getId()), anyString(), anyString(), anyString()))
                .thenReturn(new PaymentUseCase.PaymentRefundView(true, "REFUNDED", "refunded"));

        int cancelled = service.cancelTimedOutOrders();

        assertThat(cancelled).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(paymentUseCase).refundOrder(eq(stale.getId()), anyString(), anyString(), anyString());
    }

    @Test
    void aZeroTimeoutDisablesTheSweep() {
        var zero = parameter("0");
        when(systemParameterPort.findById("order.merchant_accept_timeout_minutes")).thenReturn(Optional.of(zero));
        when(systemParameterPort.findById("order.assignment_timeout_minutes")).thenReturn(Optional.of(zero));

        int cancelled = service.cancelTimedOutOrders();

        assertThat(cancelled).isZero();
        verify(orderManagementPort, never()).findStaleByStatus(any(), any());
    }

    @Test
    void staleDriverIsExpiredAndTheirUnpickedOrdersReturnToThePool() {
        UUID driverId = UUID.randomUUID();
        DriverOnlineSession session = busySession(driverId);
        Order held = order(OrderStatus.ASSIGNED, PaymentStatus.UNPAID);
        held.setDriverUserId(driverId);
        when(driverOnlineSessionPort.findLiveSessionsWithHeartbeatBefore(any(Instant.class)))
                .thenReturn(List.of(session));
        when(orderManagementPort.findOpenByDriverUserId(driverId)).thenReturn(List.of(held));

        int expired = service.expireStaleDriverSessions();

        assertThat(expired).isEqualTo(1);
        assertThat(held.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(held.getDriverUserId()).isNull();
        assertThat(session.getStatus()).isEqualTo(DriverSessionStatus.OFFLINE);
    }

    private static com.foodya.backend.domain.entities.SystemParameter parameter(String value) {
        com.foodya.backend.domain.entities.SystemParameter p = mock(com.foodya.backend.domain.entities.SystemParameter.class);
        when(p.getValue()).thenReturn(value);
        return p;
    }
}