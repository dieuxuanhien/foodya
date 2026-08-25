package com.foodya.backend.infrastructure.adapter.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.dto.WeatherData;
import com.foodya.backend.application.ports.out.WeatherContextPort;
import com.foodya.backend.infrastructure.integration.OpenWeatherClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WeatherContextAdapter implements WeatherContextPort {

    private final OpenWeatherClient openWeatherClient;
    private final ObjectMapper objectMapper;

    public WeatherContextAdapter(OpenWeatherClient openWeatherClient, ObjectMapper objectMapper) {
        this.openWeatherClient = openWeatherClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<WeatherData> getCurrentWeather(double lat, double lng) {
        try {
            String raw = openWeatherClient.getCurrentWeatherRaw(lat, lng);
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(raw);
            String weatherMain = root.path("weather").path(0).path("main").asText(null);
            String description = root.path("weather").path(0).path("description").asText(null);
            Double temp = root.path("main").has("temp") && !root.path("main").path("temp").isNull()
                    ? root.path("main").path("temp").asDouble()
                    : null;
            return Optional.of(new WeatherData(weatherMain, description, temp));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
