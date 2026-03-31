package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.UserAccountModel;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountPort {

    Optional<UserAccountModel> findById(UUID id);

    Optional<UserAccountModel> findByUsername(String username);

    Optional<UserAccountModel> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);

    UserAccountModel save(UserAccountModel userAccount);
}
