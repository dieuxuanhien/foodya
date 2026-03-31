package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.CartItemPort;
import com.foodya.backend.domain.persistence.CartItem;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CartItemPersistenceAdapter implements CartItemPort {

    private final CartItemRepository repository;

    public CartItemPersistenceAdapter(CartItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CartItem> findByCartId(UUID cartId) {
        return repository.findByCartId(cartId);
    }

    @Override
    public Optional<CartItem> findByCartIdAndMenuItemId(UUID cartId, UUID menuItemId) {
        return repository.findByCartIdAndMenuItemId(cartId, menuItemId);
    }

    @Override
    public CartItem save(CartItem cartItem) {
        return repository.save(cartItem);
    }

    @Override
    public void delete(CartItem cartItem) {
        repository.delete(cartItem);
    }

    @Override
    public void deleteByCartId(UUID cartId) {
        repository.deleteByCartId(cartId);
    }
}
