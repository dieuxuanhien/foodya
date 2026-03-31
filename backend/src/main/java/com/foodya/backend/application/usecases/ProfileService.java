package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.UserAccountModel;
import com.foodya.backend.application.dto.ChangePasswordRequest;
import com.foodya.backend.application.dto.UpdateProfileRequest;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.out.PasswordHashPort;
import com.foodya.backend.application.ports.out.UserAccountPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfileService {

    private final UserAccountPort userAccountPort;
    private final PasswordHashPort passwordHashPort;
    private final AuditLogService auditLogService;

    public ProfileService(UserAccountPort userAccountPort,
                          PasswordHashPort passwordHashPort,
                          AuditLogService auditLogService) {
        this.userAccountPort = userAccountPort;
        this.passwordHashPort = passwordHashPort;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public UserAccountModel me(UUID userId) {
        return userAccountPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found"));
    }

    @Transactional
    public UserAccountModel update(UUID userId, UpdateProfileRequest request) {
        UserAccountModel user = me(userId);
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
        UserAccountModel saved = userAccountPort.save(user);
        String now = "{\"email\":\"" + saved.getEmail() + "\",\"phoneNumber\":\"" + saved.getPhoneNumber() + "\"}";
        auditLogService.securityEvent(saved.getId().toString(), "PROFILE_UPDATED", "USER", saved.getId().toString(), old, now);
        return saved;
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        UserAccountModel user = me(userId);
        if (!passwordHashPort.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ValidationException("currentPassword is invalid", Map.of("currentPassword", "does not match current password"));
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ValidationException("confirmPassword does not match", Map.of("confirmPassword", "must equal newPassword"));
        }

        PasswordPolicy.validate(request.newPassword());
        if (passwordHashPort.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ValidationException("new password must differ from current password", Map.of("newPassword", "must differ from current password"));
        }

        user.setPasswordHash(passwordHashPort.encode(request.newPassword()));
        userAccountPort.save(user);
        auditLogService.securityEvent(user.getId().toString(), "PROFILE_PASSWORD_CHANGED", "USER", user.getId().toString(), null, "password-changed");
    }
}
