package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.entities.MenuCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

    Page<MenuCategory> findByRestaurantIdAndActiveTrue(UUID restaurantId, Pageable pageable);

    Optional<MenuCategory> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    boolean existsByRestaurantIdAndNameIgnoreCase(UUID restaurantId, String name);

    boolean existsByRestaurantIdAndNameIgnoreCaseAndIdNot(UUID restaurantId, String name, UUID id);
}
