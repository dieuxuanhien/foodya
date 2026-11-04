package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.PaymentTransaction;
import com.foodya.backend.domain.value_objects.PaymentTransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionPort {

    PaymentTransaction save(PaymentTransaction transaction);

    Optional<PaymentTransaction> findById(UUID id);

    Optional<PaymentTransaction> findByGatewayTxnRef(String gatewayTxnRef);

    Optional<PaymentTransaction> findLatestByOrderId(UUID orderId);

    /** The most recent transaction for the order that reached the given status. */
    Optional<PaymentTransaction> findLatestByOrderIdAndStatus(UUID orderId, PaymentTransactionStatus status);

    /** Transactions created before {@code before} still sitting in the given status (expiry sweep). */
    List<PaymentTransaction> findByStatusCreatedBefore(PaymentTransactionStatus status, Instant before);

    /** Completed transactions whose order was cancelled or failed and still holds the customer's money. */
    List<PaymentTransaction> findCompletedForClosedOrders();
}
