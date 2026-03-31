package com.foodya.backend.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAiChatRequest(
        @NotBlank @Size(max = 1000) String prompt,
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal lat,
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal lng
) {
}
