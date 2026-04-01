package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.CartItemPort;
import com.foodya.backend.domain.entities.CartItem;
import com.foodya.backend.infrastructure.mapper.CartItemMapper;
import com.foodya.backend.infrastructure.repository.CartItemRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class CartItemAdapter implements CartItemPort {

    private final CartItemRepository repository;
    private final CartItemMapper mapper;

    public CartItemAdapter(CartItemRepository repository, CartItemMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<CartItem> findByCartId(UUID cartId) {
        return repository.findByCartId(cartId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<CartItem> findByCartIdAndMenuItemId(UUID cartId, UUID menuItemId) {
        return repository.findByCartIdAndMenuItemId(cartId, menuItemId).map(mapper::toDomain);
    }

    @Override
    @SuppressWarnings("null")
    public CartItem save(CartItem cartItem) {
        var saved = repository.save(mapper.toPersistence(Objects.requireNonNull(cartItem)));
        return mapper.toDomain(saved);
    }

    @Override
    @SuppressWarnings("null")
    public void delete(CartItem cartItem) {
        repository.delete(mapper.toPersistence(Objects.requireNonNull(cartItem)));
    }

    @Override
    public void deleteByCartId(UUID cartId) {
        repository.deleteByCartId(cartId);
    }
}
