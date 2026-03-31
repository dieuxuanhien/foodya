package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.entities.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);
}
