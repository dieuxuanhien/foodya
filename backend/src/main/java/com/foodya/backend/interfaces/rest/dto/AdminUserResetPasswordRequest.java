package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserResetPasswordRequest(
        @NotBlank String newPassword
) {
}
