package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.MenuItemPort;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.infrastructure.mapper.MenuItemMapper;
import com.foodya.backend.infrastructure.repository.MenuItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class MenuItemAdapter implements MenuItemPort {

    private final MenuItemRepository repository;
    private final MenuItemMapper mapper;

    public MenuItemAdapter(MenuItemRepository repository, MenuItemMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<MenuItem> findByRestaurantIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNull(UUID restaurantId) {
        return repository.findByRestaurantIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNull(restaurantId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<MenuItem> findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(String keyword) {
        return repository.findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(keyword)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PaginatedResult<MenuItem> findByRestaurantIdAndActiveTrueAndDeletedAtIsNull(UUID restaurantId, int page, int size) {
        Page<MenuItem> result = repository.findByRestaurantIdAndActiveTrueAndDeletedAtIsNull(restaurantId, PageRequest.of(page, size))
            .map(mapper::toDomain);
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
        return repository.findById(Objects.requireNonNull(id)).map(mapper::toDomain);
    }

    @Override
    @SuppressWarnings("null")
    public MenuItem save(MenuItem menuItem) {
        var saved = repository.save(mapper.toPersistence(Objects.requireNonNull(menuItem)));
        return mapper.toDomain(saved);
    }
}
