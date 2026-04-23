package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.AdminMenuItemPort;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.infrastructure.mapper.MenuItemMapper;
import com.foodya.backend.infrastructure.repository.AdminMenuItemRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class AdminMenuItemAdapter implements AdminMenuItemPort {

    private final AdminMenuItemRepository repository;
    private final MenuItemMapper mapper;

    public AdminMenuItemAdapter(AdminMenuItemRepository repository, MenuItemMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<MenuItem> findById(UUID menuItemId) {
        return repository.findById(Objects.requireNonNull(menuItemId)).map(mapper::toDomain);
    }

    @Override
    @SuppressWarnings("null")
    public void delete(MenuItem menuItem) {
        repository.delete(mapper.toPersistence(Objects.requireNonNull(menuItem)));
    }
}
