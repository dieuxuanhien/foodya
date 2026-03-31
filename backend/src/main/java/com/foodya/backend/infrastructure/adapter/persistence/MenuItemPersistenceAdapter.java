package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.MenuItemPort;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.persistence.MenuItem;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MenuItemPersistenceAdapter implements MenuItemPort {

    private final MenuItemRepository repository;

    public MenuItemPersistenceAdapter(MenuItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MenuItem> findByRestaurantIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNull(UUID restaurantId) {
        return repository.findByRestaurantIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNull(restaurantId);
    }

    @Override
    public List<MenuItem> findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(String keyword) {
        return repository.findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(keyword);
    }

    @Override
    public PaginatedResult<MenuItem> findByRestaurantIdAndActiveTrueAndDeletedAtIsNull(UUID restaurantId, int page, int size) {
        Page<MenuItem> result = repository.findByRestaurantIdAndActiveTrueAndDeletedAtIsNull(restaurantId, PageRequest.of(page, size));
        return new PaginatedResult<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public Optional<MenuItem> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public MenuItem save(MenuItem menuItem) {
        return repository.save(menuItem);
    }
}
