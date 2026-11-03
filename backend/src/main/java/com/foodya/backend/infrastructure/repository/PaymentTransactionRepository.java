package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.PaymentTransactionPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionPersistenceModel, UUID> {

    Optional<PaymentTransactionPersistenceModel> findByGatewayTxnRef(String gatewayTxnRef);

    Optional<PaymentTransactionPersistenceModel> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
