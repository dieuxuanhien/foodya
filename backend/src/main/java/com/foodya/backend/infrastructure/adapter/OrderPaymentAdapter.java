package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.OrderPaymentPort;
import com.foodya.backend.domain.entities.OrderPayment;
import com.foodya.backend.infrastructure.mapper.OrderPaymentMapper;
import com.foodya.backend.infrastructure.repository.OrderPaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderPaymentAdapter implements OrderPaymentPort {

    private final OrderPaymentRepository repository;
    private final OrderPaymentMapper mapper;

    public OrderPaymentAdapter(OrderPaymentRepository repository, OrderPaymentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<OrderPayment> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId).map(mapper::toDomain);
    }

    @Override
    @SuppressWarnings("null")
    public OrderPayment save(OrderPayment payment) {
        var saved = repository.save(mapper.toPersistence(Objects.requireNonNull(payment)));
        return mapper.toDomain(saved);
    }
}
