package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpApiRequest(
        @NotBlank String challengeToken,
        @NotBlank String otp
) {
}
