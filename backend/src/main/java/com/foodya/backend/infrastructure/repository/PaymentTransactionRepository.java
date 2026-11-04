package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.value_objects.PaymentTransactionStatus;
import com.foodya.backend.infrastructure.persistence.models.PaymentTransactionPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionPersistenceModel, UUID> {

    Optional<PaymentTransactionPersistenceModel> findByGatewayTxnRef(String gatewayTxnRef);

    Optional<PaymentTransactionPersistenceModel> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<PaymentTransactionPersistenceModel> findByStatusAndCreatedAtBefore(
            PaymentTransactionStatus status, OffsetDateTime before);

    Optional<PaymentTransactionPersistenceModel> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
            UUID orderId, PaymentTransactionStatus status);

    @org.springframework.data.jpa.repository.Query(
            "SELECT t FROM PaymentTransactionPersistenceModel t WHERE t.status = com.foodya.backend.domain.value_objects.PaymentTransactionStatus.COMPLETED "
                    + "AND EXISTS (SELECT 1 FROM OrderPersistenceModel o WHERE o.id = t.orderId "
                    + "AND o.status IN (com.foodya.backend.domain.value_objects.OrderStatus.CANCELLED, com.foodya.backend.domain.value_objects.OrderStatus.FAILED))")
    List<PaymentTransactionPersistenceModel> findCompletedForClosedOrders();
}
