package com.foodya.backend.application.dto;

/**
 * Current conditions for a location.
 *
 * @param weatherMain          broad category ("Clear", "Clouds", "Rain", "Drizzle", "Thunderstorm", "Snow", "Fog")
 * @param description          human text derived from the WMO code ("Slight rain")
 * @param temperatureCelsius   air temperature at 2 m, °C
 * @param humidityPercent      relative humidity at 2 m, % (nullable)
 * @param windSpeedMs          wind speed at 10 m, m/s (nullable)
 * @param precipitationMm      precipitation in the last 15 minutes, mm (nullable)
 * @param weatherCode          raw WMO weather code (nullable)
 * @param isRaining            true for drizzle/rain/showers/thunderstorms (snow and frozen rain excluded)
 */
public record WeatherData(
        String weatherMain,
        String description,
        Double temperatureCelsius,
        Integer humidityPercent,
        Double windSpeedMs,
        Double precipitationMm,
        Integer weatherCode,
        boolean isRaining
) {
    /** Convenience factory for callers/tests that only have a category and temperature. */
    public static WeatherData of(String weatherMain, String description, Double temperatureCelsius) {
        return new WeatherData(weatherMain, description, temperatureCelsius, null, null, null, null, false);
    }

    /** Derives {@code isRaining} from a category, matching the drizzle/rain/thunderstorm keyword rule. */
    public static boolean looksRaining(String weatherMain) {
        if (weatherMain == null) return false;
        String main = weatherMain.toLowerCase(java.util.Locale.ROOT);
        return main.contains("rain") || main.contains("drizzle") || main.contains("thunder");
    }
}
