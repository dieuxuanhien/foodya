package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordApiRequest(
        @NotBlank String currentPassword,
        @NotBlank String newPassword,
        @NotBlank String confirmPassword
) {
}
