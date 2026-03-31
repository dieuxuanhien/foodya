package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.ports.out.AdminOrderPort;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.infrastructure.repository.AdminOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class AdminOrderPersistenceAdapter implements AdminOrderPort {

    private final AdminOrderRepository repository;

    public AdminOrderPersistenceAdapter(AdminOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaginatedResult<Order> search(OrderStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt"));
        Page<Order> result = status == null
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);

        return new PaginatedResult<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return repository.findById(Objects.requireNonNull(orderId));
    }

    @Override
    public Order save(Order order) {
        return repository.save(Objects.requireNonNull(order));
    }

    @Override
    public void delete(Order order) {
        repository.delete(Objects.requireNonNull(order));
    }
}
