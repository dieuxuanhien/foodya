package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.OrderPort;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.infrastructure.mapper.OrderMapper;
import com.foodya.backend.infrastructure.persistence.models.OrderPersistenceModel;
import com.foodya.backend.infrastructure.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter: OrderPort Implementation
 * Converts Order domain entities ↔ OrderPersistenceModel via mapper.
 */
@Component
public class OrderAdapter implements OrderPort {

    private final OrderRepository repository;
    private final OrderMapper mapper;

    public OrderAdapter(OrderRepository repository, OrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Order> findByCustomerUserIdAndIdempotencyKey(UUID customerUserId, String idempotencyKey) {
        return repository.findByCustomerUserIdAndIdempotencyKey(customerUserId, idempotencyKey)
                .map(mapper::toDomain);
    }

    @Override
    @SuppressWarnings("null")
    public Order save(Order order) {
        OrderPersistenceModel model = mapper.toPersistence(Objects.requireNonNull(order));
        OrderPersistenceModel saved = repository.save(model);
        return mapper.toDomain(saved);
    }
}
