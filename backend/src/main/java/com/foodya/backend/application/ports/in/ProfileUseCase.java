package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.ChangePasswordRequest;
import com.foodya.backend.application.dto.UpdateProfileRequest;
import com.foodya.backend.domain.entities.UserAccount;

import java.util.UUID;

public interface ProfileUseCase {

    UserAccount me(UUID userId);

    UserAccount update(UUID userId, UpdateProfileRequest request);

    UserAccount uploadAvatar(UUID userId, String originalFileName, String contentType, byte[] content);

    void changePassword(UUID userId, ChangePasswordRequest request);
}
