package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.policies.PasswordPolicy;
import com.foodya.backend.interfaces.rest.validation.FieldsMatch;
import com.foodya.backend.interfaces.rest.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@FieldsMatch(first = "newPassword", second = "confirmPassword", message = "confirmPassword must match newPassword")
public record ChangePasswordApiRequest(
        @NotBlank String currentPassword,
        @NotBlank
        @StrongPassword
        @Schema(
                description = "Password must contain uppercase, lowercase, number, and special character, with at least 8 characters",
                pattern = PasswordPolicy.REGEX,
                minLength = 8
        )
        String newPassword,
        @NotBlank String confirmPassword
) {
}
