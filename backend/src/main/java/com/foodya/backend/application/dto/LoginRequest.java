package com.foodya.backend.application.dto;

public record LoginRequest(
        String usernameOrEmail,
        String password
) {
}
