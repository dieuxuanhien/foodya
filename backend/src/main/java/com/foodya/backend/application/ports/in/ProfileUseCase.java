package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.ChangePasswordRequest;
import com.foodya.backend.application.dto.UpdateProfileRequest;
import com.foodya.backend.application.dto.UserAccountData;

import java.util.UUID;

public interface ProfileUseCase {

    UserAccountData me(UUID userId);

    UserAccountData update(UUID userId, UpdateProfileRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);
}
