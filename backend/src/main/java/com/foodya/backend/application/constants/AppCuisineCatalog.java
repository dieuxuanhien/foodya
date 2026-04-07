package com.foodya.backend.application.constants;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AppCuisineCatalog {

    private static final List<String> DISPLAY_VALUES = List.of(
            "Vietnamese", "Thai", "Japanese", "Korean", "Chinese",
            "Indian", "Italian", "American", "Mexican", "Vegetarian", "Vegan", "Fusion"
    );

    private static final Set<String> NORMALIZED_VALUES = DISPLAY_VALUES.stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private AppCuisineCatalog() {
    }

    public static List<String> displayValues() {
        return DISPLAY_VALUES;
    }

    public static boolean isSupported(String cuisine) {
        if (cuisine == null || cuisine.isBlank()) {
            return false;
        }
        return NORMALIZED_VALUES.contains(cuisine.trim().toLowerCase(Locale.ROOT));
    }
}
