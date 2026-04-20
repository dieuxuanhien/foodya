package com.foodya.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpApiRequest(
        @NotBlank String challengeToken,
        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "must be 6 digits")
        String otp
) {
}
