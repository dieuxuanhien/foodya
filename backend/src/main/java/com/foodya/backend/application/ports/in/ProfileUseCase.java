package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.ChangePasswordRequest;
import com.foodya.backend.application.dto.UpdateProfileRequest;
import com.foodya.backend.application.dto.UserAccountModel;

import java.util.UUID;

public interface ProfileUseCase {

    UserAccountModel me(UUID userId);

    UserAccountModel update(UUID userId, UpdateProfileRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);
}
