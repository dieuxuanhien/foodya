package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.OrderItemPort;
import com.foodya.backend.domain.persistence.OrderItem;
import com.foodya.backend.infrastructure.repository.OrderItemRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderItemPersistenceAdapter implements OrderItemPort {

    private final OrderItemRepository repository;

    public OrderItemPersistenceAdapter(OrderItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OrderItem> saveAll(List<OrderItem> items) {
        return repository.saveAll(items);
    }
}
