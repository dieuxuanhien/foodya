package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.AdminMenuItemPort;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.entities.MenuItem;
import com.foodya.backend.infrastructure.mapper.MenuItemMapper;
import com.foodya.backend.infrastructure.repository.AdminMenuItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public PaginatedResult<MenuItem> search(UUID restaurantId, String taxonomyCode, String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MenuItem> result = repository.searchAdmin(restaurantId, taxonomyCode, keyword, pageable).map(mapper::toDomain);
        return new PaginatedResult<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public Optional<MenuItem> findById(UUID menuItemId) {
        return repository.findById(Objects.requireNonNull(menuItemId)).map(mapper::toDomain);
    }

    @Override
    @SuppressWarnings("null")
    public MenuItem save(MenuItem menuItem) {
        return mapper.toDomain(repository.save(mapper.toPersistence(Objects.requireNonNull(menuItem))));
    }

    @Override
    @SuppressWarnings("null")
    public void delete(MenuItem menuItem) {
        repository.delete(mapper.toPersistence(Objects.requireNonNull(menuItem)));
    }
}
