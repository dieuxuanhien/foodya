package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.OrderManagementPort;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.infrastructure.mapper.OrderMapper;
import com.foodya.backend.infrastructure.persistence.models.OrderPersistenceModel;
import com.foodya.backend.infrastructure.repository.OrderManagementRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter: OrderManagementPort Implementation
 * 
 * Mediates between the application layer (OrderManagementPort) expecting
 * domain Order entities and the persistence layer (OrderManagementRepository)
 * working with OrderPersistenceModel.
 * 
 * Responsibility: Convert persistence models ↔ domain entities via OrderMapper.
 */
@Component
public class OrderManagementAdapter implements OrderManagementPort {

    private final OrderManagementRepository repository;
    private final OrderMapper mapper;

    public OrderManagementAdapter(OrderManagementRepository repository, OrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return repository.findById(Objects.requireNonNull(orderId))
                .map(mapper::toDomain);
    }

    @Override
    public List<Order> findByCustomerUserIdOrderByPlacedAtDesc(UUID customerUserId) {
        return repository.findByCustomerUserIdOrderByPlacedAtDesc(customerUserId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByRestaurantIdOrderByPlacedAtDesc(UUID restaurantId) {
        return repository.findByRestaurantIdOrderByPlacedAtDesc(restaurantId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByStatusInOrderByPlacedAtAsc(Collection<OrderStatus> statuses) {
        return repository.findByStatusInOrderByPlacedAtAsc(statuses)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @SuppressWarnings("null")
    public Order save(Order order) {
        OrderPersistenceModel model = mapper.toPersistence(Objects.requireNonNull(order));
        OrderPersistenceModel saved = repository.save(model);
        return mapper.toDomain(saved);
    }
}
