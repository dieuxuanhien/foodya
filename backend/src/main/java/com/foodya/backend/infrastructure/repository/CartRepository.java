package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.model.CartStatus;
import com.foodya.backend.domain.persistence.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByCustomerUserIdAndStatus(UUID customerUserId, CartStatus status);
}
