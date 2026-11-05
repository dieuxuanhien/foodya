package com.foodya.backend.application.dto;

public record CreateOrderReviewRequest(
	int stars,
	String comment
) {
}
