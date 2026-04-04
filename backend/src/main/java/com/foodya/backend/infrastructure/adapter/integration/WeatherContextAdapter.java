package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.ports.out.WeatherContextPort;
import com.foodya.backend.infrastructure.integration.OpenWeatherClient;
import org.springframework.stereotype.Component;

@Component
public class WeatherContextAdapter implements WeatherContextPort {

    private final OpenWeatherClient openWeatherClient;

    public WeatherContextAdapter(OpenWeatherClient openWeatherClient) {
        this.openWeatherClient = openWeatherClient;
    }

    @Override
    public String getCurrentWeatherRaw(double lat, double lng) {
        return openWeatherClient.getCurrentWeatherRaw(lat, lng);
    }
}
