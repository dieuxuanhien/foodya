package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.OrderManagementPort;
import com.foodya.backend.domain.model.OrderStatus;
import com.foodya.backend.domain.persistence.Order;
import com.foodya.backend.infrastructure.repository.OrderManagementRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderManagementPersistenceAdapter implements OrderManagementPort {

    private final OrderManagementRepository repository;

    public OrderManagementPersistenceAdapter(OrderManagementRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return repository.findById(orderId);
    }

    @Override
    public List<Order> findByCustomerUserIdOrderByPlacedAtDesc(UUID customerUserId) {
        return repository.findByCustomerUserIdOrderByPlacedAtDesc(customerUserId);
    }

    @Override
    public List<Order> findByRestaurantIdOrderByPlacedAtDesc(UUID restaurantId) {
        return repository.findByRestaurantIdOrderByPlacedAtDesc(restaurantId);
    }

    @Override
    public List<Order> findByStatusInOrderByPlacedAtAsc(Collection<OrderStatus> statuses) {
        return repository.findByStatusInOrderByPlacedAtAsc(statuses);
    }

    @Override
    public Order save(Order order) {
        return repository.save(order);
    }
}
