package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.domain.policies.PasswordPolicy;
import com.foodya.backend.interfaces.rest.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import com.foodya.backend.domain.value_objects.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterApiRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "must be a valid phone number") String phoneNumber,
        @NotBlank String fullName,
        @NotBlank
        @StrongPassword
        @Schema(
                description = "Password must contain uppercase, lowercase, number, and special character, with at least 8 characters",
                pattern = PasswordPolicy.REGEX,
                minLength = 8
        )
        String password,
        @NotNull UserRole role
) {
}
