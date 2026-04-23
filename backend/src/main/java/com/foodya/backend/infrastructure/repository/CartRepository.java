package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.value_objects.CartStatus;
import com.foodya.backend.infrastructure.persistence.models.CartPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<CartPersistenceModel, UUID> {

    Optional<CartPersistenceModel> findByCustomerUserIdAndStatus(UUID customerUserId, CartStatus status);
}
