package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.PaymentGatewayDtos.*;

import java.util.Map;

public interface PaymentGatewayPort {

    PaymentInitResult initiate(PaymentInitRequest request);

    /** Verifies the gateway signature and interprets the response code. */
    PaymentVerifyResult verifyCallback(Map<String, String> params);

    PaymentQueryResult queryTransaction(String transactionRef);

    /**
     * Requests a full refund for a completed transaction.
     *
     * @param request refund details (gateway transaction reference, original amount, actor and reason)
     * @return refund outcome; {@code success=false} with a message when the gateway rejected it
     */
    PaymentRefundResult refund(PaymentRefundRequest request);
}
