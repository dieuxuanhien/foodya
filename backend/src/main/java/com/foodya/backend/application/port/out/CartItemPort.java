package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.CartItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemPort {

    List<CartItem> findByCartId(UUID cartId);

    Optional<CartItem> findByCartIdAndMenuItemId(UUID cartId, UUID menuItemId);

    CartItem save(CartItem cartItem);

    void delete(CartItem cartItem);

    void deleteByCartId(UUID cartId);
}
