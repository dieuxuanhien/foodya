package com.foodya.backend.infrastructure.adapter.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.dto.WeatherData;
import com.foodya.backend.application.dto.WmoWeatherCode;
import com.foodya.backend.application.ports.out.WeatherContextPort;
import com.foodya.backend.infrastructure.integration.OpenMeteoClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WeatherContextAdapter implements WeatherContextPort {

    private final OpenMeteoClient openMeteoClient;
    private final ObjectMapper objectMapper;

    public WeatherContextAdapter(OpenMeteoClient openMeteoClient, ObjectMapper objectMapper) {
        this.openMeteoClient = openMeteoClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<WeatherData> getCurrentWeather(double lat, double lng) {
        try {
            String raw = openMeteoClient.currentWeatherRaw(lat, lng);
            return Optional.ofNullable(parse(raw));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    WeatherData parse(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        JsonNode root = objectMapper.readTree(raw);
        if (root.path("error").asBoolean(false)) {
            return null;
        }
        JsonNode current = root.path("current");
        if (!current.isObject()) {
            return null;
        }
        WmoWeatherCode code = WmoWeatherCode.fromCode(optionalInt(current, "weather_code"));
        String category = code.category();
        return new WeatherData(
                category,
                code.description(),
                optionalDouble(current, "temperature_2m"),
                optionalInt(current, "relative_humidity_2m"),
                optionalDouble(current, "wind_speed_10m"),
                optionalDouble(current, "precipitation"),
                code == WmoWeatherCode.UNKNOWN ? null : code.code(),
                WeatherData.looksRaining(category));
    }

    private static Double optionalDouble(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asDouble();
    }

    private static Integer optionalInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }
}
