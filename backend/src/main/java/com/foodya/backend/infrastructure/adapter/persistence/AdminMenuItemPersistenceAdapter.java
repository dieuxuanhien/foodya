package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.ports.out.AdminMenuItemPort;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.infrastructure.repository.AdminMenuItemRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class AdminMenuItemPersistenceAdapter implements AdminMenuItemPort {

    private final AdminMenuItemRepository repository;

    public AdminMenuItemPersistenceAdapter(AdminMenuItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MenuItem> findById(UUID menuItemId) {
        return repository.findById(Objects.requireNonNull(menuItemId));
    }

    @Override
    public void delete(MenuItem menuItem) {
        repository.delete(Objects.requireNonNull(menuItem));
    }
}
