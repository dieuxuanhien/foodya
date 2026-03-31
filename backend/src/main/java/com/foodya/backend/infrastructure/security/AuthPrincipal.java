package com.foodya.backend.infrastructure.security;

import com.foodya.backend.application.security.CurrentUserPrincipal;

import java.util.UUID;

public record AuthPrincipal(
        UUID userId,
        String role
)
        implements CurrentUserPrincipal {
}
