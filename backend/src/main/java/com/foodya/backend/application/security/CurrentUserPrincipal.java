package com.foodya.backend.application.security;

import java.util.UUID;

public interface CurrentUserPrincipal {

    UUID userId();

    String role();
}