package com.foodya.backend.application.port.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.persistence.MenuItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemPort {

    List<MenuItem> findByRestaurantIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNull(UUID restaurantId);

    List<MenuItem> findByActiveTrueAndDeletedAtIsNullAndNameContainingIgnoreCase(String keyword);

    PaginatedResult<MenuItem> findByRestaurantIdAndActiveTrueAndDeletedAtIsNull(UUID restaurantId, int page, int size);

    Optional<MenuItem> findById(UUID id);

    MenuItem save(MenuItem menuItem);
}
