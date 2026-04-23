package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.OrderItemPort;
import com.foodya.backend.domain.entities.OrderItem;
import com.foodya.backend.infrastructure.mapper.OrderItemMapper;
import com.foodya.backend.infrastructure.repository.OrderItemRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class OrderItemAdapter implements OrderItemPort {

    private final OrderItemRepository repository;
    private final OrderItemMapper mapper;

    public OrderItemAdapter(OrderItemRepository repository, OrderItemMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @SuppressWarnings("null")
    public List<OrderItem> saveAll(List<OrderItem> items) {
        return repository.saveAll(Objects.requireNonNull(items).stream().map(mapper::toPersistence).toList())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
