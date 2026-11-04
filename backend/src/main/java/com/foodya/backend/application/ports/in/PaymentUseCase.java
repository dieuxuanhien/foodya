package com.foodya.backend.application.ports.in;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface PaymentUseCase {

    /** Callback status that tells the controller to answer the gateway with IPN code 04. */
    String AMOUNT_MISMATCH = "AMOUNT_MISMATCH";

    record PaymentInitiation(UUID orderId, String paymentUrl, String transactionRef, BigDecimal amount) {}

    record PaymentCallbackResult(boolean verified, boolean success, UUID orderId,
                                 String transactionRef, String responseCode, String status) {}

    record PaymentStatusView(UUID orderId, String orderPaymentStatus, String transactionStatus,
                             String paymentMethod, String transactionRef) {}

    record PaymentRefundView(boolean refunded, String status, String message) {}

    /** Creates a transaction and returns the gateway redirect URL. */
    PaymentInitiation initiatePayment(UUID orderId, UUID customerUserId, String paymentMethod,
                                      String ipAddress, String returnUrl);

    /** Handles the gateway IPN callback (signature-verified, idempotent). */
    PaymentCallbackResult handleCallback(Map<String, String> params);

    PaymentStatusView getPaymentStatus(UUID orderId, UUID customerUserId);

    /** Refunds a paid online order in full. Idempotent once refunded. */
    PaymentRefundView refundOrder(UUID orderId, String actor, String ipAddress, String reason);

    /**
     * Marks online payments that were started but never confirmed within {@code age} as failed.
     *
     * @return number of transactions expired
     */
    int expireStalePayments(java.time.Duration age);

    /**
     * Refunds completed payments whose order is already cancelled or failed (refund failed earlier,
     * or the money arrived after the order closed).
     *
     * @return number of transactions refunded
     */
    int retryPendingRefunds();
}
