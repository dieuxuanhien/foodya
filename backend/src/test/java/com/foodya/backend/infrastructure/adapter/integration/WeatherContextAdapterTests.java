package com.foodya.backend.infrastructure.adapter.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.dto.WeatherData;
import com.foodya.backend.application.dto.WmoWeatherCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherContextAdapterTests {

    private final WeatherContextAdapter adapter = new WeatherContextAdapter(null, new ObjectMapper());

    @Test
    void parsesCurrentBlockAndMapsWmoCodeToCategoryText() throws Exception {
        String raw = """
                {"latitude":10.75,"longitude":106.75,"timezone":"Asia/Bangkok","utc_offset_seconds":25200,
                 "current":{"time":"2026-11-04T10:15","interval":900,"temperature_2m":28.4,
                   "relative_humidity_2m":74,"precipitation":1.2,"rain":1.2,
                   "weather_code":63,"wind_speed_10m":3.6}}
                """;
        WeatherData data = adapter.parse(raw);
        assertNotNull(data);
        assertEquals("Rain", data.weatherMain());
        assertEquals("Moderate rain", data.description());
        assertEquals(28.4, data.temperatureCelsius());
        assertEquals(74, data.humidityPercent());
        assertEquals(3.6, data.windSpeedMs());
        assertEquals(1.2, data.precipitationMm());
        assertEquals(63, data.weatherCode());
        assertTrue(data.isRaining());
    }

    @Test
    void clearSkyIsNotRainingAndHasNoPrecipitation() throws Exception {
        WeatherData data = adapter.parse("""
                {"current":{"temperature_2m":33.1,"relative_humidity_2m":55,"precipitation":0.0,
                  "weather_code":0,"wind_speed_10m":2.0}}
                """);
        assertNotNull(data);
        assertEquals("Clear", data.weatherMain());
        assertEquals("Clear sky", data.description());
        assertFalse(data.isRaining());
    }

    @Test
    void snowfallIsSnowCategoryButNotRaining() throws Exception {
        WeatherData data = adapter.parse("""
                {"current":{"temperature_2m":-2.0,"weather_code":73}}
                """);
        assertNotNull(data);
        assertEquals("Snow", data.weatherMain());
        assertFalse(data.isRaining());
        assertNull(data.humidityPercent());
    }

    @Test
    void returnsNullForErrorOrMissingCurrent() throws Exception {
        assertNull(adapter.parse("{\"error\":true,\"reason\":\"Latitude must be in range\"}"));
        assertNull(adapter.parse("{\"latitude\":10.75}"));
        assertNull(adapter.parse(""));
        assertNull(adapter.parse(null));
    }

    @Test
    void unknownWmoCodeFallsBackToUnknownLabel() throws Exception {
        WeatherData data = adapter.parse("{\"current\":{\"temperature_2m\":25.0,\"weather_code\":12345}}");
        assertNotNull(data);
        assertEquals("Unknown", data.weatherMain());
        assertNull(data.weatherCode());
    }

    @Test
    void everyDocumentedWmoCodeIsMapped() {
        int[] documented = {0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77,
                80, 81, 82, 85, 86, 95, 96, 97, 99};
        for (int code : documented) {
            assertNotEquals(WmoWeatherCode.UNKNOWN, WmoWeatherCode.fromCode(code), "code " + code);
        }
    }
}
