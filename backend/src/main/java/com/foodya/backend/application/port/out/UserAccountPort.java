package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountPort {

    Optional<UserAccount> findById(UUID id);

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);

    UserAccount save(UserAccount userAccount);
}
