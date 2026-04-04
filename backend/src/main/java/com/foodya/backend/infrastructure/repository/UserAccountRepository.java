package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccountPersistenceModel, UUID> {
    Optional<UserAccountPersistenceModel> findByUsername(String username);

    Optional<UserAccountPersistenceModel> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);
}
