package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.PaymentGatewayDtos.PaymentInitRequest;
import com.foodya.backend.application.dto.PaymentGatewayDtos.PaymentInitResult;
import com.foodya.backend.application.dto.PaymentGatewayDtos.PaymentRefundRequest;
import com.foodya.backend.application.dto.PaymentGatewayDtos.PaymentVerifyResult;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.NotFoundException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Framework-free VNPAY payment orchestration.
 *
 * <p>Idempotency: VNPAY retries IPNs, and the customer may reload the return URL. A transaction that is
 * already COMPLETED short-circuits before any state is touched, and {@code syncOrderPayment} only
 * transitions UNPAID → PAID once.
 *
 * <p>A payment can also arrive after the order was cancelled or failed. In that case the money is not
 * applied to the order (the customer was already told it is closed); it is refunded instead.
 */
public class PaymentService implements PaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String VNPAY_SUCCESS = "00";
    /** Status reported to the controller so it can answer the gateway with IPN code 04. */
    public static final String AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    private static final DateTimeFormatter VNPAY_TXN_DATE =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final OrderManagementPort orderManagementPort;
    private final OrderPaymentPort orderPaymentPort;
    private final PaymentTransactionPort paymentTransactionPort;
    private final PaymentGatewayPort paymentGatewayPort;

    public PaymentService(OrderManagementPort orderManagementPort,
                          OrderPaymentPort orderPaymentPort,
                          PaymentTransactionPort paymentTransactionPort,
                          PaymentGatewayPort paymentGatewayPort) {
        this.orderManagementPort = orderManagementPort;
        this.orderPaymentPort = orderPaymentPort;
        this.paymentTransactionPort = paymentTransactionPort;
        this.paymentGatewayPort = paymentGatewayPort;
    }

    @Override
    public PaymentInitiation initiatePayment(UUID orderId, UUID customerUserId, String paymentMethod,
                                             String ipAddress, String returnUrl) {
        Order order = requireOwnedOrder(orderId, customerUserId);
        PaymentMethod method = parseMethod(paymentMethod);
        if (!method.isOnlineGateway()) {
            throw new ValidationException("COD orders are paid on delivery",
                    Map.of("paymentMethod", "must be one of VNPAY, VNPAY_QR, VNPAY_ATM, VNPAY_CARD"));
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ValidationException("order is already paid", Map.of("paymentStatus", "PAID"));
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new ValidationException("order can no longer be paid online",
                    Map.of("status", order.getStatus().name()));
        }

        PaymentInitResult result = paymentGatewayPort.initiate(new PaymentInitRequest(
                order.getId(), order.getOrderCode(), order.getTotalAmount(),
                "Thanh toan don hang " + order.getOrderCode(), returnUrl, ipAddress, method.name()));

        PaymentTransaction txn = PaymentTransaction.create(order.getId(), method.toPersistedMethod(),
                PaymentGateway.VNPAY, result.transactionRef(),
                order.getTotalAmount().longValue(), returnUrl, ipAddress);
        txn.setTransactionDate(result.transactionDate());
        txn.markProcessing(result.paymentUrl());
        txn = paymentTransactionPort.save(txn);

        return new PaymentInitiation(order.getId(), txn.getPaymentUrl(), txn.getGatewayTxnRef(),
                order.getTotalAmount());
    }

    @Override
    public PaymentCallbackResult handleCallback(Map<String, String> params) {
        PaymentVerifyResult verify = paymentGatewayPort.verifyCallback(params);
        String txnRef = verify.transactionRef();
        PaymentTransaction txn = paymentTransactionPort.findByGatewayTxnRef(txnRef).orElse(null);
        if (!verify.verified()) {
            return new PaymentCallbackResult(false, false, null, txnRef, verify.responseCode(),
                    txn == null ? null : txn.getStatus().name());
        }
        if (txn == null) {
            log.warn("VNPAY callback for unknown txnRef {}", txnRef);
            return new PaymentCallbackResult(true, false, null, txnRef, verify.responseCode(), null);
        }
        if (txn.getStatus() == PaymentTransactionStatus.COMPLETED
                || txn.getStatus() == PaymentTransactionStatus.REFUNDED) {
            return new PaymentCallbackResult(true, true, txn.getOrderId(), txnRef,
                    verify.responseCode(), txn.getStatus().name());
        }
        if (!verifySuccessResponse(verify.responseCode())) {
            txn.fail("responseCode=" + verify.responseCode());
            paymentTransactionPort.save(txn);
            return new PaymentCallbackResult(true, false, txn.getOrderId(), txnRef,
                    verify.responseCode(), txn.getStatus().name());
        }

        BigDecimal expectedAmount = BigDecimal.valueOf(txn.getAmount());
        if (verify.amount() != null && verify.amount().compareTo(expectedAmount) != 0) {
            log.error("VNPAY amount mismatch for txnRef {}: expected {} but gateway reported {}",
                    txnRef, expectedAmount, verify.amount());
            return new PaymentCallbackResult(true, false, txn.getOrderId(), txnRef,
                    verify.responseCode(), AMOUNT_MISMATCH);
        }

        txn.complete(verify.transactionNo(), "responseCode=" + verify.responseCode());
        paymentTransactionPort.save(txn);

        Order order = orderManagementPort.findById(txn.getOrderId()).orElse(null);
        if (order != null && isClosed(order)) {
            // Money arrived for an order that is already cancelled/failed: refund instead of crediting it.
            log.warn("Payment for closed order {} (status {}) — issuing refund", order.getOrderCode(), order.getStatus());
            refundTransaction(txn, "system", "127.0.0.1", "Order closed before payment confirmed");
            return new PaymentCallbackResult(true, false, txn.getOrderId(), txnRef,
                    verify.responseCode(), txn.getStatus().name());
        }
        syncOrderPayment(txn.getOrderId(), verify.transactionNo() != null ? verify.transactionNo() : txnRef);
        return new PaymentCallbackResult(true, true, txn.getOrderId(), txnRef,
                verify.responseCode(), txn.getStatus().name());
    }

    @Override
    public PaymentStatusView getPaymentStatus(UUID orderId, UUID customerUserId) {
        Order order = requireOwnedOrder(orderId, customerUserId);
        PaymentTransaction txn = paymentTransactionPort.findLatestByOrderId(orderId).orElse(null);
        return new PaymentStatusView(orderId, order.getPaymentStatus().name(),
                txn == null ? null : txn.getStatus().name(),
                order.getPaymentMethod().name(),
                txn == null ? null : txn.getGatewayTxnRef());
    }

    @Override
    public PaymentRefundView refundOrder(UUID orderId, String actor, String ipAddress, String reason) {
        PaymentTransaction txn = paymentTransactionPort
                .findLatestByOrderIdAndStatus(orderId, PaymentTransactionStatus.COMPLETED)
                .orElse(null);
        if (txn == null) {
            PaymentTransaction latest = paymentTransactionPort.findLatestByOrderId(orderId).orElse(null);
            if (latest != null && latest.getStatus() == PaymentTransactionStatus.REFUNDED) {
                return new PaymentRefundView(true, latest.getStatus().name(), "already refunded");
            }
            return new PaymentRefundView(false, latest == null ? null : latest.getStatus().name(),
                    "no completed payment to refund");
        }
        return refundTransaction(txn, actor, ipAddress, reason);
    }

    @Override
    public int expireStalePayments(java.time.Duration age) {
        Instant cutoff = Instant.now().minus(age);
        int expired = 0;
        for (PaymentTransactionStatus status :
                new PaymentTransactionStatus[]{PaymentTransactionStatus.PENDING, PaymentTransactionStatus.PROCESSING}) {
            for (PaymentTransaction txn : paymentTransactionPort.findByStatusCreatedBefore(status, cutoff)) {
                try {
                    txn.markExpired("expired after " + age.toMinutes() + " minutes without confirmation");
                    paymentTransactionPort.save(txn);
                    expired++;
                } catch (RuntimeException ex) {
                    log.warn("Could not expire payment transaction {}", txn.getId(), ex);
                }
            }
        }
        return expired;
    }

    @Override
    public int retryPendingRefunds() {
        int refunded = 0;
        for (PaymentTransaction txn : paymentTransactionPort.findCompletedForClosedOrders()) {
            try {
                if (refundTransaction(txn, "system", "127.0.0.1", "Order closed").refunded()) {
                    refunded++;
                }
            } catch (RuntimeException ex) {
                log.warn("Refund retry failed for transaction {}", txn.getId(), ex);
            }
        }
        return refunded;
    }

    private PaymentRefundView refundTransaction(PaymentTransaction txn, String actor, String ipAddress, String reason) {
        if (txn.getStatus() == PaymentTransactionStatus.REFUNDED) {
            return new PaymentRefundView(true, txn.getStatus().name(), "already refunded");
        }
        String transactionDate = txn.getTransactionDate() != null
                ? txn.getTransactionDate()
                : VNPAY_TXN_DATE.format(txn.getCreatedAt());

        var result = paymentGatewayPort.refund(new PaymentRefundRequest(
                txn.getGatewayTxnRef(), txn.getAmount(), transactionDate, txn.getGatewayTxnNo(),
                actor, ipAddress, reason));
        if (!result.success()) {
            return new PaymentRefundView(false, txn.getStatus().name(),
                    "gateway refund failed: " + result.responseCode() + " " + result.message());
        }

        txn.completeRefund(result.gatewayTransactionNo(), "refund:" + result.responseCode());
        paymentTransactionPort.save(txn);

        orderManagementPort.findById(txn.getOrderId()).ifPresent(order -> {
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.markRefunded();
                orderManagementPort.save(order);
            }
            orderPaymentPort.findByOrderId(order.getId()).ifPresent(payment -> {
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                orderPaymentPort.save(payment);
            });
        });
        return new PaymentRefundView(true, txn.getStatus().name(), "refunded");
    }

    private static boolean isClosed(Order order) {
        return order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.FAILED;
    }

    private void syncOrderPayment(UUID orderId, String gatewayTxnNo) {
        Order order = orderManagementPort.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (order.getPaymentStatus() == PaymentStatus.UNPAID) {
            order.setPaymentMethod(PaymentMethod.VNPAY);
            order.markOnlinePaid();
            orderManagementPort.save(order);
        }

        OrderPayment payment = orderPaymentPort.findByOrderId(orderId).orElseGet(OrderPayment::new);
        payment.setOrderId(orderId);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setAmount(order.getTotalAmount());
        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
            payment.markPaid(gatewayTxnNo);
            orderPaymentPort.save(payment);
        }
    }

    private static boolean verifySuccessResponse(String responseCode) {
        return VNPAY_SUCCESS.equals(responseCode);
    }

    private Order requireOwnedOrder(UUID orderId, UUID customerUserId) {
        Order order = orderManagementPort.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!order.getCustomerUserId().equals(customerUserId)) {
            throw new ForbiddenException("order does not belong to customer");
        }
        return order;
    }

    private static PaymentMethod parseMethod(String raw) {
        try {
            return PaymentMethod.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("unsupported payment method",
                    Map.of("paymentMethod", "must be one of COD, VNPAY, VNPAY_QR, VNPAY_ATM, VNPAY_CARD"));
        }
    }

    static OffsetDateTime now() {
        return OffsetDateTime.now();
    }
}