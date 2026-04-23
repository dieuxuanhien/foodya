package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.MenuItem;

import java.util.Optional;
import java.util.UUID;

public interface AdminMenuItemPort {

    Optional<MenuItem> findById(UUID menuItemId);

    void delete(MenuItem menuItem);
}
