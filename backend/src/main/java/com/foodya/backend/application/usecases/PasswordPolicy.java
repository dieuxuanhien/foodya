package com.foodya.backend.application.usecases;

import com.foodya.backend.application.exception.ValidationException;

import java.util.Map;

public final class PasswordPolicy {

    private static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || !password.matches(REGEX)) {
            throw new ValidationException(
                    "password does not meet complexity requirements",
                    Map.of("password", "must be >=8 chars and include uppercase, lowercase, number, special char")
            );
        }
    }
}
