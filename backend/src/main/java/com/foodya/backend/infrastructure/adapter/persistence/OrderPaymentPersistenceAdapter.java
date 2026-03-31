package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.ports.out.OrderPaymentPort;
import com.foodya.backend.domain.entities.OrderPayment;
import com.foodya.backend.infrastructure.repository.OrderPaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class OrderPaymentPersistenceAdapter implements OrderPaymentPort {

    private final OrderPaymentRepository repository;

    public OrderPaymentPersistenceAdapter(OrderPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrderPayment save(OrderPayment payment) {
        return repository.save(Objects.requireNonNull(payment));
    }
}
