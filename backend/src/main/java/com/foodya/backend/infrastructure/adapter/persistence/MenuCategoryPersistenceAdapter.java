package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.ports.out.MenuCategoryPort;
import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.entities.MenuCategory;
import com.foodya.backend.infrastructure.repository.MenuCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class MenuCategoryPersistenceAdapter implements MenuCategoryPort {

    private final MenuCategoryRepository repository;

    public MenuCategoryPersistenceAdapter(MenuCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MenuCategory> findById(UUID id) {
        return repository.findById(Objects.requireNonNull(id));
    }

    @Override
    public Optional<MenuCategory> findByIdAndRestaurantId(UUID id, UUID restaurantId) {
        return repository.findByIdAndRestaurantId(id, restaurantId);
    }

    @Override
    public boolean existsByRestaurantIdAndNameIgnoreCase(UUID restaurantId, String name) {
        return repository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, name);
    }

    @Override
    public boolean existsByRestaurantIdAndNameIgnoreCaseAndIdNot(UUID restaurantId, String name, UUID id) {
        return repository.existsByRestaurantIdAndNameIgnoreCaseAndIdNot(restaurantId, name, id);
    }

    @Override
    public PaginatedResult<MenuCategory> findByRestaurantIdAndActiveTrue(UUID restaurantId, int page, int size) {
        Page<MenuCategory> result = repository.findByRestaurantIdAndActiveTrue(restaurantId, PageRequest.of(page, size));
        return new PaginatedResult<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public MenuCategory save(MenuCategory menuCategory) {
        return repository.save(Objects.requireNonNull(menuCategory));
    }

    @Override
    public void delete(MenuCategory menuCategory) {
        repository.delete(Objects.requireNonNull(menuCategory));
    }
}
