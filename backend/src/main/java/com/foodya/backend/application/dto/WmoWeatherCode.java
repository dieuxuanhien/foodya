package com.foodya.backend.application.dto;

/**
 * WMO 4677 weather interpretation codes as returned by Open-Meteo's {@code weather_code}.
 * Open-Meteo returns numbers only, so this is the mapping layer that turns them into the
 * human text the chat prompt and weather signal logic expect.
 */
public enum WmoWeatherCode {
    CLEAR_SKY(0, "Clear sky"),
    MAINLY_CLEAR(1, "Mainly clear"),
    PARTLY_CLOUDY(2, "Partly cloudy"),
    OVERCAST(3, "Overcast"),
    FOG(45, "Fog"),
    DEPOSITING_RIME_FOG(48, "Depositing rime fog"),
    LIGHT_DRIZZLE(51, "Light drizzle"),
    MODERATE_DRIZZLE(53, "Moderate drizzle"),
    DENSE_DRIZZLE(55, "Dense drizzle"),
    LIGHT_FREEZING_DRIZZLE(56, "Light freezing drizzle"),
    DENSE_FREEZING_DRIZZLE(57, "Dense freezing drizzle"),
    SLIGHT_RAIN(61, "Slight rain"),
    MODERATE_RAIN(63, "Moderate rain"),
    HEAVY_RAIN(65, "Heavy rain"),
    LIGHT_FREEZING_RAIN(66, "Light freezing rain"),
    HEAVY_FREEZING_RAIN(67, "Heavy freezing rain"),
    SLIGHT_SNOWFALL(71, "Slight snowfall"),
    MODERATE_SNOWFALL(73, "Moderate snowfall"),
    HEAVY_SNOWFALL(75, "Heavy snowfall"),
    SNOW_GRAINS(77, "Snow grains"),
    SLIGHT_RAIN_SHOWERS(80, "Slight rain showers"),
    MODERATE_RAIN_SHOWERS(81, "Moderate rain showers"),
    VIOLENT_RAIN_SHOWERS(82, "Violent rain showers"),
    SLIGHT_SNOW_SHOWERS(85, "Slight snow showers"),
    HEAVY_SNOW_SHOWERS(86, "Heavy snow showers"),
    THUNDERSTORM(95, "Thunderstorm"),
    THUNDERSTORM_SLIGHT_HAIL(96, "Thunderstorm with slight hail"),
    HEAVY_THUNDERSTORM(97, "Heavy thunderstorm"),
    THUNDERSTORM_HEAVY_HAIL(99, "Thunderstorm with heavy hail"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String description;

    WmoWeatherCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int code() {
        return code;
    }

    public String description() {
        return description;
    }

    /**
     * Broad category used by the weather-signal logic. Deliberately spelled like the values the old
     * the previous provider's {@code weather[0].main} returned ("Rain", "Clear", "Clouds", ...) so downstream
     * keyword matching did not have to change.
     */
    public String category() {
        return switch (this) {
            case CLEAR_SKY, MAINLY_CLEAR -> "Clear";
            case PARTLY_CLOUDY, OVERCAST -> "Clouds";
            case FOG, DEPOSITING_RIME_FOG -> "Fog";
            case LIGHT_DRIZZLE, MODERATE_DRIZZLE, DENSE_DRIZZLE,
                 LIGHT_FREEZING_DRIZZLE, DENSE_FREEZING_DRIZZLE -> "Drizzle";
            case SLIGHT_RAIN, MODERATE_RAIN, HEAVY_RAIN, LIGHT_FREEZING_RAIN, HEAVY_FREEZING_RAIN,
                 SLIGHT_RAIN_SHOWERS, MODERATE_RAIN_SHOWERS, VIOLENT_RAIN_SHOWERS -> "Rain";
            case SLIGHT_SNOWFALL, MODERATE_SNOWFALL, HEAVY_SNOWFALL, SNOW_GRAINS,
                 SLIGHT_SNOW_SHOWERS, HEAVY_SNOW_SHOWERS -> "Snow";
            case THUNDERSTORM, THUNDERSTORM_SLIGHT_HAIL, HEAVY_THUNDERSTORM, THUNDERSTORM_HEAVY_HAIL -> "Thunderstorm";
            case UNKNOWN -> "Unknown";
        };
    }

    public static WmoWeatherCode fromCode(Integer code) {
        if (code == null) return UNKNOWN;
        for (WmoWeatherCode c : values()) {
            if (c.code == code) return c;
        }
        return UNKNOWN;
    }
}
