package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.PaymentTransaction;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionPort {

    PaymentTransaction save(PaymentTransaction transaction);

    Optional<PaymentTransaction> findById(UUID id);

    Optional<PaymentTransaction> findByGatewayTxnRef(String gatewayTxnRef);

    Optional<PaymentTransaction> findLatestByOrderId(UUID orderId);
}
