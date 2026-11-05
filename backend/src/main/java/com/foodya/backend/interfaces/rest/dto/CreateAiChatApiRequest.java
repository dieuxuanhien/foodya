package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.application.dto.CreateAiChatRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAiChatApiRequest(
        @NotBlank @Size(max = 2000) String prompt,
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal lat,
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal lng
) {
    public CreateAiChatRequest toApplicationDto() {
        return new CreateAiChatRequest(prompt, lat, lng);
    }
}
