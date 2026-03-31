package com.foodya.backend.application.usecases;

public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    public static String normalize(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        String normalized = trimmed.replaceAll("\\s+", "");
        if (!normalized.matches("^\\+?[0-9]{9,15}$")) {
            throw new IllegalArgumentException("invalid phone number format");
        }
        return normalized;
    }
}
