package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.ports.out.CartPort;
import com.foodya.backend.domain.value_objects.CartStatus;
import com.foodya.backend.domain.entities.Cart;
import com.foodya.backend.infrastructure.repository.CartRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class CartPersistenceAdapter implements CartPort {

    private final CartRepository repository;

    public CartPersistenceAdapter(CartRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Cart> findByCustomerUserIdAndStatus(UUID customerUserId, CartStatus status) {
        return repository.findByCustomerUserIdAndStatus(customerUserId, status);
    }

    @Override
    public Cart save(Cart cart) {
        return repository.save(Objects.requireNonNull(cart));
    }
}
