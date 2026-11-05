package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.PaymentGatewayDtos.*;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.PaymentUseCase;
import com.foodya.backend.application.ports.out.OrderManagementPort;
import com.foodya.backend.application.ports.out.OrderPaymentPort;
import com.foodya.backend.application.ports.out.PaymentGatewayPort;
import com.foodya.backend.application.ports.out.PaymentTransactionPort;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.domain.entities.OrderPayment;
import com.foodya.backend.domain.entities.PaymentTransaction;
import com.foodya.backend.domain.value_objects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private OrderManagementPort orderManagementPort;
    private OrderPaymentPort orderPaymentPort;
    private PaymentTransactionPort transactionPort;
    private PaymentGatewayPort gatewayPort;
    private PaymentService service;

    private Order order;
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderManagementPort = mock(OrderManagementPort.class);
        orderPaymentPort = mock(OrderPaymentPort.class);
        transactionPort = mock(PaymentTransactionPort.class);
        gatewayPort = mock(PaymentGatewayPort.class);
        service = new PaymentService(orderManagementPort, orderPaymentPort, transactionPort, gatewayPort);

        order = Order.builder()
                .orderCode("ORD-1")
                .customerUserId(customerId)
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.UNPAID)
                .totalAmount(new BigDecimal("65000"))
                .build();
        order.setId(UUID.randomUUID());
        when(orderManagementPort.findById(order.getId())).thenReturn(Optional.of(order));
        when(transactionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderPaymentPort.findByOrderId(any())).thenReturn(Optional.empty());
        when(gatewayPort.initiate(any())).thenReturn(
                new PaymentInitResult("https://pay?x=1", "ORD-1-abcd1234", null));
    }

    @Test
    void initiateCreatesProcessingTransactionForOwnedOrder() {
        PaymentUseCase.PaymentInitiation result = service.initiatePayment(
                order.getId(), customerId, "VNPAY_QR", "1.2.3.4", null);

        assertThat(result.paymentUrl()).isEqualTo("https://pay?x=1");
        verify(transactionPort).save(argThat(t ->
                t.getStatus() == PaymentTransactionStatus.PROCESSING
                        && t.getPaymentMethod() == PaymentMethod.VNPAY
                        && t.getGatewayTxnRef().equals("ORD-1-abcd1234")));
    }

    @Test
    void initiateRejectsCodAndForeignOrders() {
        assertThatThrownBy(() -> service.initiatePayment(order.getId(), customerId, "COD", "1.2.3.4", null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.initiatePayment(order.getId(), UUID.randomUUID(), "VNPAY", "1.2.3.4", null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void successfulCallbackMarksTransactionAndOrderPaid() {
        PaymentTransaction txn = PaymentTransaction.create(order.getId(), PaymentMethod.VNPAY,
                PaymentGateway.VNPAY, "ORD-1-abcd1234", 65000, null, null);
        when(transactionPort.findByGatewayTxnRef("ORD-1-abcd1234")).thenReturn(Optional.of(txn));
        when(gatewayPort.verifyCallback(any())).thenReturn(new PaymentVerifyResult(
                true, true, "ORD-1-abcd1234", "123456", "00", new BigDecimal("65000"), "NCB"));

        PaymentUseCase.PaymentCallbackResult result = service.handleCallback(new HashMap<>(Map.of("vnp_ResponseCode", "00")));

        assertThat(result.success()).isTrue();
        assertThat(txn.getStatus()).isEqualTo(PaymentTransactionStatus.COMPLETED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.VNPAY);
        verify(orderPaymentPort).save(argThat(p -> p.getPaymentStatus() == PaymentStatus.PAID));
    }

    @Test
    void duplicateCallbackIsIdempotent() {
        PaymentTransaction txn = PaymentTransaction.create(order.getId(), PaymentMethod.VNPAY,
                PaymentGateway.VNPAY, "REF-1", 65000, null, null);
        txn.complete("123", "ok");
        when(transactionPort.findByGatewayTxnRef("REF-1")).thenReturn(Optional.of(txn));
        when(gatewayPort.verifyCallback(any())).thenReturn(new PaymentVerifyResult(
                true, true, "REF-1", "123", "00", new BigDecimal("65000"), "NCB"));

        service.handleCallback(new HashMap<>());

        verify(transactionPort, never()).save(any());
        verify(orderManagementPort, never()).save(any());
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void failedResponseCodeMarksTransactionFailed() {
        PaymentTransaction txn = PaymentTransaction.create(order.getId(), PaymentMethod.VNPAY,
                PaymentGateway.VNPAY, "REF-2", 65000, null, null);
        when(transactionPort.findByGatewayTxnRef("REF-2")).thenReturn(Optional.of(txn));
        when(gatewayPort.verifyCallback(any())).thenReturn(new PaymentVerifyResult(
                true, false, "REF-2", null, "24", new BigDecimal("65000"), null));

        PaymentUseCase.PaymentCallbackResult result = service.handleCallback(new HashMap<>());

        assertThat(result.success()).isFalse();
        assertThat(txn.getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void badSignatureIsRejectedWithoutTouchingState() {
        when(gatewayPort.verifyCallback(any())).thenReturn(new PaymentVerifyResult(
                false, false, "REF-3", null, "00", null, null));

        PaymentUseCase.PaymentCallbackResult result = service.handleCallback(new HashMap<>());

        assertThat(result.verified()).isFalse();
        verify(transactionPort, never()).save(any());
        verify(orderManagementPort, never()).save(any());
    }

    @Test
    void amountMismatchIsRejectedAndReportedToTheGateway() {
        PaymentTransaction txn = PaymentTransaction.create(order.getId(), PaymentMethod.VNPAY,
                PaymentGateway.VNPAY, "REF-AMT", 65000, null, null);
        when(transactionPort.findByGatewayTxnRef("REF-AMT")).thenReturn(Optional.of(txn));
        // gateway reports a smaller amount than the order total
        when(gatewayPort.verifyCallback(any())).thenReturn(new PaymentVerifyResult(
                true, true, "REF-AMT", "123", "00", new BigDecimal("1000"), "NCB"));

        PaymentUseCase.PaymentCallbackResult result = service.handleCallback(new HashMap<>());

        assertThat(result.success()).isFalse();
        assertThat(result.status()).isEqualTo(PaymentUseCase.AMOUNT_MISMATCH);
        assertThat(txn.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING); // untouched
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void paymentArrivingAfterCancellationIsRefundedNotCredited() {
        Order cancelled = Order.builder()
                .orderCode("ORD-CANCELLED")
                .customerUserId(customerId)
                .status(OrderStatus.CANCELLED)
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(PaymentStatus.UNPAID)
                .totalAmount(new BigDecimal("65000"))
                .build();
        cancelled.setId(UUID.randomUUID());
        when(orderManagementPort.findById(cancelled.getId())).thenReturn(Optional.of(cancelled));

        PaymentTransaction txn = PaymentTransaction.create(cancelled.getId(), PaymentMethod.VNPAY,
                PaymentGateway.VNPAY, "REF-LATE", 65000, null, null);
        txn.setTransactionDate("20261104120000");
        when(transactionPort.findByGatewayTxnRef("REF-LATE")).thenReturn(Optional.of(txn));
        when(gatewayPort.verifyCallback(any())).thenReturn(new PaymentVerifyResult(
                true, true, "REF-LATE", "123", "00", new BigDecimal("65000"), "NCB"));
        when(gatewayPort.refund(any())).thenReturn(new PaymentRefundResult(true, "00", "ok", "999"));

        PaymentUseCase.PaymentCallbackResult result = service.handleCallback(new HashMap<>());

        assertThat(result.success()).isFalse();
        assertThat(txn.getStatus()).isEqualTo(PaymentTransactionStatus.REFUNDED);
        assertThat(cancelled.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        verify(gatewayPort).refund(argThat(r -> r.transactionRef().equals("REF-LATE")
                && r.amount() == 65000L && "20261104120000".equals(r.transactionDate())));
    }

    @Test
    void refundOrderMarksTransactionAndOrderRefunded() {
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.CANCELLED);

        PaymentTransaction txn = PaymentTransaction.create(order.getId(), PaymentMethod.VNPAY,
                PaymentGateway.VNPAY, "REF-OK", 65000, null, null);
        txn.complete("123", "ok");
        when(transactionPort.findLatestByOrderIdAndStatus(order.getId(), PaymentTransactionStatus.COMPLETED))
                .thenReturn(Optional.of(txn));
        when(gatewayPort.refund(any())).thenReturn(new PaymentRefundResult(true, "00", "ok", "999"));

        PaymentUseCase.PaymentRefundView view = service.refundOrder(order.getId(), "admin", "1.2.3.4", "test");

        assertThat(view.refunded()).isTrue();
        assertThat(txn.getStatus()).isEqualTo(PaymentTransactionStatus.REFUNDED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void refundIsIdempotentWhenAlreadyRefunded() {
        PaymentTransaction txn = PaymentTransaction.create(order.getId(), PaymentMethod.VNPAY,
                PaymentGateway.VNPAY, "REF-DONE", 65000, null, null);
        txn.complete("123", "ok");
        txn.refund();
        when(transactionPort.findLatestByOrderIdAndStatus(order.getId(), PaymentTransactionStatus.COMPLETED))
                .thenReturn(Optional.empty());
        when(transactionPort.findLatestByOrderId(order.getId())).thenReturn(Optional.of(txn));

        PaymentUseCase.PaymentRefundView view = service.refundOrder(order.getId(), "admin", "1.2.3.4", "again");

        assertThat(view.refunded()).isTrue();
        verify(gatewayPort, never()).refund(any());
    }

    @Test
    void gatewayRefundFailureLeavesTransactionCompletedForRetry() {
        order.setPaymentStatus(PaymentStatus.PAID);
        PaymentTransaction txn = PaymentTransaction.create(order.getId(), PaymentMethod.VNPAY,
                PaymentGateway.VNPAY, "REF-FAIL", 65000, null, null);
        txn.complete("123", "ok");
        when(transactionPort.findLatestByOrderIdAndStatus(order.getId(), PaymentTransactionStatus.COMPLETED))
                .thenReturn(Optional.of(txn));
        when(gatewayPort.refund(any())).thenReturn(new PaymentRefundResult(false, "91", "no such transaction", null));

        PaymentUseCase.PaymentRefundView view = service.refundOrder(order.getId(), "admin", "1.2.3.4", "test");

        assertThat(view.refunded()).isFalse();
        assertThat(txn.getStatus()).isEqualTo(PaymentTransactionStatus.COMPLETED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void expireStalePaymentsFailsUnconfirmedTransactions() {
        PaymentTransaction stale = PaymentTransaction.create(order.getId(), PaymentMethod.VNPAY,
                PaymentGateway.VNPAY, "REF-STALE", 65000, null, null);
        stale.markProcessing("https://pay");
        when(transactionPort.findByStatusCreatedBefore(eq(PaymentTransactionStatus.PROCESSING),
                org.mockito.ArgumentMatchers.<java.time.Instant>any()))
                .thenReturn(java.util.List.of(stale));

        int expired = service.expireStalePayments(java.time.Duration.ofMinutes(30));

        assertThat(expired).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);
    }
}
