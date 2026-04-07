package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginApiRequest(
        @NotBlank String usernameOrEmail,
        @NotBlank String password
) {
}
