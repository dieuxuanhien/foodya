package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.MenuItem;

import java.util.Optional;
import java.util.UUID;

public interface AdminMenuItemPort {

    Optional<MenuItem> findById(UUID menuItemId);

    void delete(MenuItem menuItem);
}
