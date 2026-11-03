package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.PaymentGatewayDtos.*;

import java.util.Map;

public interface PaymentGatewayPort {

    PaymentInitResult initiate(PaymentInitRequest request);

    /** Verifies the gateway signature and interprets the response code. */
    PaymentVerifyResult verifyCallback(Map<String, String> params);

    PaymentQueryResult queryTransaction(String transactionRef);
}
