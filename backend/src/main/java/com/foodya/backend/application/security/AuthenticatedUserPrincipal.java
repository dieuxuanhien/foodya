package com.foodya.backend.application.security;

import java.util.UUID;

public interface AuthenticatedUserPrincipal {

    UUID userId();

    String role();
}