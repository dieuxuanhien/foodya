package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.PaymentTransactionPort;
import com.foodya.backend.domain.entities.PaymentTransaction;
import com.foodya.backend.domain.value_objects.PaymentTransactionStatus;
import com.foodya.backend.infrastructure.mapper.PaymentTransactionMapper;
import com.foodya.backend.infrastructure.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentTransactionPersistenceAdapter implements PaymentTransactionPort {

    private final PaymentTransactionRepository repository;
    private final PaymentTransactionMapper mapper;

    public PaymentTransactionPersistenceAdapter(PaymentTransactionRepository repository,
                                                 PaymentTransactionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public PaymentTransaction save(PaymentTransaction transaction) {
        return mapper.toDomain(repository.save(mapper.toPersistence(transaction)));
    }

    @Override
    public Optional<PaymentTransaction> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentTransaction> findByGatewayTxnRef(String gatewayTxnRef) {
        return repository.findByGatewayTxnRef(gatewayTxnRef).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentTransaction> findLatestByOrderId(UUID orderId) {
        return repository.findFirstByOrderIdOrderByCreatedAtDesc(orderId).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentTransaction> findLatestByOrderIdAndStatus(UUID orderId, PaymentTransactionStatus status) {
        return repository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(orderId, status).map(mapper::toDomain);
    }

    @Override
    public List<PaymentTransaction> findCompletedForClosedOrders() {
        return repository.findCompletedForClosedOrders().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PaymentTransaction> findByStatusCreatedBefore(PaymentTransactionStatus status, Instant before) {
        return repository.findByStatusAndCreatedAtBefore(
                        status, OffsetDateTime.ofInstant(before, ZoneOffset.UTC))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
