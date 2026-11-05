package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.WeatherData;

import java.util.Optional;

public interface WeatherContextPort {

    Optional<WeatherData> getCurrentWeather(double lat, double lng);
}
