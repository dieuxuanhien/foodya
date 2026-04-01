package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.CartItemPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItemPersistenceModel, UUID> {

    List<CartItemPersistenceModel> findByCartId(UUID cartId);

    Optional<CartItemPersistenceModel> findByCartIdAndMenuItemId(UUID cartId, UUID menuItemId);

    void deleteByCartId(UUID cartId);
}
