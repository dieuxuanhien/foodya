package com.foodya.backend.application.port.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.persistence.MenuCategory;

import java.util.Optional;
import java.util.UUID;

public interface MenuCategoryPort {

    Optional<MenuCategory> findById(UUID id);

    Optional<MenuCategory> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    boolean existsByRestaurantIdAndNameIgnoreCase(UUID restaurantId, String name);

    boolean existsByRestaurantIdAndNameIgnoreCaseAndIdNot(UUID restaurantId, String name, UUID id);

    PaginatedResult<MenuCategory> findByRestaurantIdAndActiveTrue(UUID restaurantId, int page, int size);

    MenuCategory save(MenuCategory menuCategory);

    void delete(MenuCategory menuCategory);
}
