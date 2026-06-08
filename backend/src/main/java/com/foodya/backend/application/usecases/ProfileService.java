package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.UserAccountData;
import com.foodya.backend.application.dto.ChangePasswordRequest;
import com.foodya.backend.application.dto.UpdateProfileRequest;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.ProfileUseCase;
import com.foodya.backend.application.ports.out.PasswordHashPort;
import com.foodya.backend.application.ports.out.UserAccountPort;
import com.foodya.backend.application.ports.out.UserAvatarStoragePort;
import com.foodya.backend.domain.policies.PasswordPolicy;
import com.foodya.backend.domain.services.PhoneNormalizer;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ProfileService implements ProfileUseCase {

    private final UserAccountPort userAccountPort;
    private final PasswordHashPort passwordHashPort;
    private final AuditLogService auditLogService;
    private final UserAvatarStoragePort userAvatarStoragePort;

    public ProfileService(UserAccountPort userAccountPort,
                          PasswordHashPort passwordHashPort,
                          AuditLogService auditLogService,
                          UserAvatarStoragePort userAvatarStoragePort) {
        this.userAccountPort = userAccountPort;
        this.passwordHashPort = passwordHashPort;
        this.auditLogService = auditLogService;
        this.userAvatarStoragePort = userAvatarStoragePort;
    }

    public UserAccountData me(UUID userId) {
        return userAccountPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found"));
    }

    public UserAccountData update(UUID userId, UpdateProfileRequest request) {
        UserAccountData user = me(userId);
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String normalizedPhone;
        try {
            normalizedPhone = PhoneNormalizer.normalize(request.phoneNumber());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid phone number", Map.of("phoneNumber", ex.getMessage()));
        }

        if (userAccountPort.existsByEmailAndIdNot(normalizedEmail, userId)) {
            throw new ValidationException("email already exists", Map.of("email", "already exists"));
        }
        if (userAccountPort.existsByPhoneNumberAndIdNot(normalizedPhone, userId)) {
            throw new ValidationException("phoneNumber already exists", Map.of("phoneNumber", "already exists"));
        }

        String old = "{\"email\":\"" + user.getEmail() + "\",\"phoneNumber\":\"" + user.getPhoneNumber() + "\"}";
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(normalizedPhone);
        user.setAvatarUrl(request.avatarUrl());
        UserAccountData saved = userAccountPort.save(user);
        String now = "{\"email\":\"" + saved.getEmail() + "\",\"phoneNumber\":\"" + saved.getPhoneNumber() + "\"}";
        auditLogService.securityEvent(saved.getId().toString(), "PROFILE_UPDATED", "USER", saved.getId().toString(), old, now);
        return saved;
    }

    public UserAccountData uploadAvatar(UUID userId, String originalFileName, String contentType, byte[] content) {
        if (content == null || content.length == 0) {
            throw new ValidationException("invalid content", Map.of("file", "must not be empty"));
        }
        int maxBytes = 5 * 1024 * 1024;
        if (content.length > maxBytes) {
            throw new ValidationException("invalid content", Map.of("file", "must be <= 5MB"));
        }
        if (contentType != null && !contentType.isBlank() && !contentType.startsWith("image/")) {
            throw new ValidationException("invalid contentType", Map.of("contentType", "must be an image mime type"));
        }

        UserAccountData user = me(userId);
        String avatarUrl = userAvatarStoragePort.store(userId, originalFileName, contentType, content);
        user.setAvatarUrl(avatarUrl);
        return userAccountPort.save(user);
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        UserAccountData user = me(userId);
        if (!passwordHashPort.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ValidationException("currentPassword is invalid", Map.of("currentPassword", "does not match current password"));
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ValidationException("confirmPassword does not match", Map.of("confirmPassword", "must equal newPassword"));
        }

        try {
            PasswordPolicy.validate(request.newPassword());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(
                    "password does not meet complexity requirements",
                    Map.of("password", ex.getMessage())
            );
        }
        if (passwordHashPort.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ValidationException("new password must differ from current password", Map.of("newPassword", "must differ from current password"));
        }

        user.setPasswordHash(passwordHashPort.encode(request.newPassword()));
        userAccountPort.save(user);
        auditLogService.securityEvent(user.getId().toString(), "PROFILE_PASSWORD_CHANGED", "USER", user.getId().toString(), null, "password-changed");
    }
}
