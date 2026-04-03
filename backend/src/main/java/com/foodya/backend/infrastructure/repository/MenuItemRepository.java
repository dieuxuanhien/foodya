package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.MenuItemPersistenceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItemPersistenceModel, UUID> {

    Page<MenuItemPersistenceModel> findByRestaurantIdAndActiveTrueAndDeletedAtIsNull(UUID restaurantId, Pageable pageable);

    Page<MenuItemPersistenceModel> findByRestaurantIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNull(UUID restaurantId, Pageable pageable);

    List<MenuItemPersistenceModel> findByRestaurantIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNull(UUID restaurantId);

    List<MenuItemPersistenceModel> findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(String keyword);

    List<MenuItemPersistenceModel> findByActiveTrueAndAvailableTrueAndDeletedAtIsNull();

    List<MenuItemPersistenceModel> findByRestaurantIdInAndActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(Collection<UUID> restaurantIds,
                                                                                                                    String keyword);

    Optional<MenuItemPersistenceModel> findByIdAndRestaurantIdAndDeletedAtIsNull(UUID id, UUID restaurantId);
}
