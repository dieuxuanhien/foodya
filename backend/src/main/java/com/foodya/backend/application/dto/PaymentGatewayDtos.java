package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** DTOs exchanged with the payment gateway port (kept framework-free). */
public final class PaymentGatewayDtos {

    private PaymentGatewayDtos() {}

    public record PaymentInitRequest(
            UUID orderId,
            String orderCode,
            BigDecimal amount,
            String orderInfo,
            String returnUrl,
            String ipAddress,
            String paymentMethod
    ) {}

    /** {@code transactionDate} is the gateway-side creation time (yyyyMMddHHmmss) a later refund must echo back. */
    public record PaymentInitResult(String paymentUrl, String transactionRef, String transactionDate) {}

    public record PaymentVerifyResult(boolean verified, boolean success, String transactionRef,
                                      String transactionNo, String responseCode, BigDecimal amount,
                                      String bankCode) {}

    public record PaymentQueryResult(String status, BigDecimal amount, String transactionNo) {}

    public record PaymentRefundRequest(
            String transactionRef,
            long amount,
            String transactionDate,
            String transactionNo,
            String createBy,
            String ipAddress,
            String reason
    ) {}

    public record PaymentRefundResult(boolean success, String responseCode, String message,
                                      String gatewayTransactionNo) {}
}
