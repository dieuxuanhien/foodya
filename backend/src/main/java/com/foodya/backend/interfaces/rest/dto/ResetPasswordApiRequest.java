package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordApiRequest(
        @NotBlank String resetToken,
        @NotBlank String newPassword,
        @NotBlank String confirmPassword
) {
}
