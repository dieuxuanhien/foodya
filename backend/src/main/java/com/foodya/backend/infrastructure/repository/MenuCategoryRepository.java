package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.MenuCategoryPersistenceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategoryPersistenceModel, UUID> {

    Page<MenuCategoryPersistenceModel> findByRestaurantIdAndActiveTrue(UUID restaurantId, Pageable pageable);

    Optional<MenuCategoryPersistenceModel> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    boolean existsByRestaurantIdAndNameIgnoreCase(UUID restaurantId, String name);

    boolean existsByRestaurantIdAndNameIgnoreCaseAndIdNot(UUID restaurantId, String name, UUID id);
}
