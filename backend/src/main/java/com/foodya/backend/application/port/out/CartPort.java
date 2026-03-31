package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.model.CartStatus;
import com.foodya.backend.domain.persistence.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartPort {

    Optional<Cart> findByCustomerUserIdAndStatus(UUID customerUserId, CartStatus status);

    Cart save(Cart cart);
}
