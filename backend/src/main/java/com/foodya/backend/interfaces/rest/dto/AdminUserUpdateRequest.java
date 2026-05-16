package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.value_objects.UserRole;
import com.foodya.backend.domain.value_objects.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminUserUpdateRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String phoneNumber,
        @NotBlank String fullName,
        @NotNull UserRole role,
        @NotNull UserStatus status
) {
}
