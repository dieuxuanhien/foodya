package com.foodya.backend.domain.policies;

public final class PasswordPolicy {

    public static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";
    public static final String REQUIREMENTS_MESSAGE = "must be >=8 chars and include uppercase, lowercase, number, special char";

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || !password.matches(REGEX)) {
            throw new IllegalArgumentException(REQUIREMENTS_MESSAGE);
        }
    }
}
