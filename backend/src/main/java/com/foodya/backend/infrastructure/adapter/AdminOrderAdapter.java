package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.ports.out.AdminOrderPort;
import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.entities.Order;
import com.foodya.backend.infrastructure.mapper.OrderMapper;
import com.foodya.backend.infrastructure.persistence.models.OrderPersistenceModel;
import com.foodya.backend.infrastructure.repository.AdminOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter: AdminOrderPort Implementation
 * Converts Order domain entities ↔ OrderPersistenceModel via mapper.
 */
@Component
public class AdminOrderAdapter implements AdminOrderPort {

    private final AdminOrderRepository repository;
    private final OrderMapper mapper;

    public AdminOrderAdapter(AdminOrderRepository repository, OrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public PaginatedResult<Order> search(OrderStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt"));
        Page<OrderPersistenceModel> result = status == null
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);

        return new PaginatedResult<>(
                result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return repository.findById(Objects.requireNonNull(orderId))
                .map(mapper::toDomain);
    }

    @Override
    @SuppressWarnings("null")
    public Order save(Order order) {
        OrderPersistenceModel model = mapper.toPersistence(Objects.requireNonNull(order));
        OrderPersistenceModel saved = repository.save(model);
        return mapper.toDomain(saved);
    }

    @Override
    @SuppressWarnings("null")
    public void delete(Order order) {
        OrderPersistenceModel model = mapper.toPersistence(Objects.requireNonNull(order));
        repository.delete(model);
    }
}
