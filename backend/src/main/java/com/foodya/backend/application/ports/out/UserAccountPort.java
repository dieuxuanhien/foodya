package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.UserAccountData;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountPort {

    Optional<UserAccountData> findById(UUID id);

    Optional<UserAccountData> findByUsername(String username);

    Optional<UserAccountData> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);

    UserAccountData save(UserAccountData userAccount);
}
