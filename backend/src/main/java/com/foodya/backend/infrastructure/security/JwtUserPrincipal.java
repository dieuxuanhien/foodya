package com.foodya.backend.infrastructure.security;

import com.foodya.backend.application.security.AuthenticatedUserPrincipal;

import java.util.UUID;

public record JwtUserPrincipal(
        UUID userId,
        String role
)
        implements AuthenticatedUserPrincipal {
}
