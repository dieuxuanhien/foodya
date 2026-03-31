package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String phoneNumber,
        @NotBlank String fullName,
        @NotBlank String password,
        @NotNull UserRole role
) {
}
