package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.CartPort;
import com.foodya.backend.domain.value_objects.CartStatus;
import com.foodya.backend.domain.entities.Cart;
import com.foodya.backend.infrastructure.mapper.CartMapper;
import com.foodya.backend.infrastructure.repository.CartRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class CartAdapter implements CartPort {

    private final CartRepository repository;
    private final CartMapper mapper;

    public CartAdapter(CartRepository repository, CartMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Cart> findByCustomerUserIdAndStatus(UUID customerUserId, CartStatus status) {
        return repository.findByCustomerUserIdAndStatus(customerUserId, status).map(mapper::toDomain);
    }

    @Override
    @SuppressWarnings("null")
    public Cart save(Cart cart) {
        var saved = repository.save(mapper.toPersistence(Objects.requireNonNull(cart)));
        return mapper.toDomain(saved);
    }
}
