package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.MenuItemPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminMenuItemRepository extends JpaRepository<MenuItemPersistenceModel, UUID> {
}
