package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.PaginatedResult;
import com.foodya.backend.domain.entities.MenuItem;

import java.util.Optional;
import java.util.UUID;

public interface AdminMenuItemPort {

    PaginatedResult<MenuItem> search(UUID restaurantId, String taxonomyCode, String keyword, int page, int size);

    Optional<MenuItem> findById(UUID menuItemId);

    void delete(MenuItem menuItem);
}
