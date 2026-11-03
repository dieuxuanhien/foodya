package com.foodya.backend.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public final class PaymentApiResponses {

    private PaymentApiResponses() {}

    public record PaymentInitiationResponse(UUID orderId, String paymentUrl, String transactionRef,
                                            BigDecimal amount) {}

    public record PaymentStatusResponse(UUID orderId, String orderPaymentStatus, String transactionStatus,
                                        String paymentMethod, String transactionRef) {}
}
