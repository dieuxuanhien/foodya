package com.foodya.backend.interfaces.rest.support;

import com.foodya.backend.application.exception.UnauthorizedException;
import com.foodya.backend.application.security.CurrentUserPrincipal;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID userId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("unauthorized");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CurrentUserPrincipal currentUserPrincipal) {
            return currentUserPrincipal.userId();
        }

        throw new UnauthorizedException("unauthorized");
    }
}