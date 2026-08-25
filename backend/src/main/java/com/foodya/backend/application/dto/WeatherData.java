package com.foodya.backend.application.dto;

public record WeatherData(
        String weatherMain,
        String description,
        Double temperatureCelsius
) {
}
