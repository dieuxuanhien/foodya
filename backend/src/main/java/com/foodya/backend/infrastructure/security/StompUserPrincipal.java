package com.foodya.backend.infrastructure.security;

import com.foodya.backend.application.security.AuthenticatedUserPrincipal;

import java.security.Principal;
import java.util.UUID;

public record StompUserPrincipal(UUID userId, String role)
        implements Principal, AuthenticatedUserPrincipal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
