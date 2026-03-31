package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.ports.out.WeatherContextPort;
import com.foodya.backend.infrastructure.integration.OpenWeatherAdapter;
import org.springframework.stereotype.Component;

@Component
public class WeatherContextAdapter implements WeatherContextPort {

    private final OpenWeatherAdapter openWeatherAdapter;

    public WeatherContextAdapter(OpenWeatherAdapter openWeatherAdapter) {
        this.openWeatherAdapter = openWeatherAdapter;
    }

    @Override
    public String getCurrentWeatherRaw(double lat, double lng) {
        return openWeatherAdapter.getCurrentWeatherRaw(lat, lng);
    }
}
