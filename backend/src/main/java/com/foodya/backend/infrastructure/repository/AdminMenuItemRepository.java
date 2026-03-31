package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.entities.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminMenuItemRepository extends JpaRepository<MenuItem, UUID> {
}
