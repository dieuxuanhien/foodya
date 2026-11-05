package com.foodya.backend.application.dto;

import java.math.BigDecimal;

public record CreateAiChatRequest(
	String prompt,
	BigDecimal lat,
	BigDecimal lng
) {
}
