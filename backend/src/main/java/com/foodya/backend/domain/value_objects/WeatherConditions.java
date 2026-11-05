package com.foodya.backend.domain.value_objects;

/**
 * The weather facts pricing cares about, expressed in domain terms (no dependency on provider DTOs).
 *
 * @param wmoCode   WMO 4677 code, or null when unknown
 * @param category  broad category text ("Rain", "Clear", ...)
 * @param isRaining true for drizzle/rain/showers/thunderstorms
 */
public record WeatherConditions(Integer wmoCode, String category, boolean isRaining) {

    public static WeatherConditions none() {
        return new WeatherConditions(null, null, false);
    }

    /** True when the category text implies rain even if the provider did not set {@code isRaining}. */
    public boolean rainyByCategory() {
        if (isRaining) return true;
        if (category == null) return false;
        String c = category.toLowerCase(java.util.Locale.ROOT);
        return c.contains("rain") || c.contains("drizzle") || c.contains("thunder");
    }
}
