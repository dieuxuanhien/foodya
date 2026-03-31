package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.persistence.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    Page<MenuItem> findByRestaurantIdAndActiveTrueAndDeletedAtIsNull(UUID restaurantId, Pageable pageable);

    Page<MenuItem> findByRestaurantIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNull(UUID restaurantId, Pageable pageable);

    List<MenuItem> findByRestaurantIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNull(UUID restaurantId);

    List<MenuItem> findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(String keyword);

    List<MenuItem> findByRestaurantIdInAndActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(Collection<UUID> restaurantIds,
                                                                                                    String keyword);

    Optional<MenuItem> findByIdAndRestaurantIdAndDeletedAtIsNull(UUID id, UUID restaurantId);
}
