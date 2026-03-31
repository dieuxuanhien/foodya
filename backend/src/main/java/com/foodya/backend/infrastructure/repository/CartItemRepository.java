package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.persistence.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCartId(UUID cartId);

    Optional<CartItem> findByCartIdAndMenuItemId(UUID cartId, UUID menuItemId);

    void deleteByCartId(UUID cartId);
}
