package com.foodya.backend.infrastructure.integration;

import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import com.foodya.backend.infrastructure.config.IntegrationKey;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenWeatherAdapter {

    private static final String OPEN_WEATHER_BASE_URL = "https://api.openweathermap.org";

    private final RestClient restClient;
    private final ApiSecretsProvider apiSecretsProvider;

    public OpenWeatherAdapter(ApiSecretsProvider apiSecretsProvider) {
        this.apiSecretsProvider = apiSecretsProvider;
        this.restClient = RestClient.builder().baseUrl(OPEN_WEATHER_BASE_URL).build();
    }

    public String getCurrentWeatherRaw(double lat, double lng) {
        String apiKey = apiSecretsProvider.get(IntegrationKey.OPENWEATHER_API_KEY)
                .orElseThrow(() -> new IllegalStateException("Missing OpenWeather API key"));

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data/2.5/weather")
                        .queryParam("lat", lat)
                        .queryParam("lon", lng)
                        .queryParam("units", "metric")
                        .queryParam("appid", apiKey)
                        .build())
                .retrieve()
                .body(String.class);
    }
}
