package com.foodya.backend.interfaces.rest.dto;

import com.foodya.backend.application.dto.RespondOrderReviewRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RespondOrderReviewApiRequest(
        @NotBlank @Size(max = 1000) String response
) {
    public RespondOrderReviewRequest toApplicationDto() {
        return new RespondOrderReviewRequest(response);
    }
}
