package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.application.port.out.AdminRestaurantPort;
import com.foodya.backend.domain.model.OrderStatus;
import com.foodya.backend.domain.model.RestaurantStatus;
import com.foodya.backend.domain.persistence.Restaurant;
import com.foodya.backend.infrastructure.repository.AdminRestaurantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Component
public class AdminRestaurantPersistenceAdapter implements AdminRestaurantPort {

    private final AdminRestaurantRepository repository;

    public AdminRestaurantPersistenceAdapter(AdminRestaurantRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaginatedResult<Restaurant> search(String keyword, RestaurantStatus status, int page, int size) {
        String normalized = keyword == null ? "" : keyword.trim();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Restaurant> result;
        if (status == null) {
            result = repository.findByNameContainingIgnoreCase(normalized, pageable);
        } else {
            result = repository.findByStatusAndNameContainingIgnoreCase(status, normalized, pageable);
        }

        return new PaginatedResult<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public Optional<Restaurant> findById(UUID restaurantId) {
        return repository.findById(restaurantId);
    }

    @Override
    public Restaurant save(Restaurant restaurant) {
        return repository.save(restaurant);
    }

    @Override
    public void delete(Restaurant restaurant) {
        repository.delete(restaurant);
    }

    @Override
    public boolean hasOrdersInStatuses(UUID restaurantId, Collection<OrderStatus> statuses) {
        return repository.hasOrdersInStatuses(restaurantId, statuses);
    }
}
