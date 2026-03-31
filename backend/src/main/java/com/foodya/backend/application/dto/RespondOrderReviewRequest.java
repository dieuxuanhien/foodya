package com.foodya.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RespondOrderReviewRequest(
        @NotBlank @Size(max = 2000) String response
) {
}
