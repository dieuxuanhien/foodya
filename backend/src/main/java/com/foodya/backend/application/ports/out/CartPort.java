package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.value_objects.CartStatus;
import com.foodya.backend.domain.entities.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartPort {

    Optional<Cart> findByCustomerUserIdAndStatus(UUID customerUserId, CartStatus status);

    Cart save(Cart cart);
}
