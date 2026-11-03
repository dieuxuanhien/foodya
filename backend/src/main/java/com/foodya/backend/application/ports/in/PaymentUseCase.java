package com.foodya.backend.application.ports.in;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface PaymentUseCase {

    record PaymentInitiation(UUID orderId, String paymentUrl, String transactionRef, BigDecimal amount) {}

    record PaymentCallbackResult(boolean verified, boolean success, UUID orderId,
                                 String transactionRef, String responseCode, String status) {}

    record PaymentStatusView(UUID orderId, String orderPaymentStatus, String transactionStatus,
                             String paymentMethod, String transactionRef) {}

    /** Creates a transaction and returns the gateway redirect URL. */
    PaymentInitiation initiatePayment(UUID orderId, UUID customerUserId, String paymentMethod,
                                      String ipAddress, String returnUrl);

    /** Handles the gateway IPN callback (signature-verified, idempotent). */
    PaymentCallbackResult handleCallback(Map<String, String> params);

    PaymentStatusView getPaymentStatus(UUID orderId, UUID customerUserId);
}
