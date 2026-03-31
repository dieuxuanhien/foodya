package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.OrderPaymentPort;
import com.foodya.backend.domain.persistence.OrderPayment;
import com.foodya.backend.infrastructure.repository.OrderPaymentRepository;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentPersistenceAdapter implements OrderPaymentPort {

    private final OrderPaymentRepository repository;

    public OrderPaymentPersistenceAdapter(OrderPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrderPayment save(OrderPayment payment) {
        return repository.save(payment);
    }
}
