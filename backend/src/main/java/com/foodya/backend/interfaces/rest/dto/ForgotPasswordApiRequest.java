package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordApiRequest(
        @NotBlank @Email String email
) {
}
