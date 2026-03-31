package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.OrderPort;
import com.foodya.backend.domain.persistence.Order;
import com.foodya.backend.infrastructure.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class OrderPersistenceAdapter implements OrderPort {

    private final OrderRepository repository;

    public OrderPersistenceAdapter(OrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Order> findByCustomerUserIdAndIdempotencyKey(UUID customerUserId, String idempotencyKey) {
        return repository.findByCustomerUserIdAndIdempotencyKey(customerUserId, idempotencyKey);
    }

    @Override
    public Order save(Order order) {
        return repository.save(order);
    }
}
