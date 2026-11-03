package com.foodya.backend.infrastructure.integration;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Open-Meteo current-weather client. No API key is required for the standard (non-commercial) endpoint.
 *
 * <p>Requested variables are fixed to the flat {@code current} block so the adapter can read them without
 * parsing arrays; {@code wind_speed_unit=ms} keeps wind in m/s regardless of temperature unit.
 */
@Component
public class OpenMeteoClient {

    private static final String BASE_URL = "https://api.open-meteo.com";
    private static final String CURRENT_VARIABLES =
            "temperature_2m,relative_humidity_2m,precipitation,rain,weather_code,wind_speed_10m";
    private static final int MAX_ATTEMPTS = 2;

    private final RestClient restClient;

    public OpenMeteoClient() {
        this(RestClient.builder().baseUrl(BASE_URL).build());
    }

    public OpenMeteoClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public String currentWeatherRaw(double lat, double lng) {
        RestClientException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1/forecast")
                                .queryParam("latitude", lat)
                                .queryParam("longitude", lng)
                                .queryParam("current", CURRENT_VARIABLES)
                                .queryParam("temperature_unit", "celsius")
                                .queryParam("wind_speed_unit", "ms")
                                .queryParam("precipitation_unit", "mm")
                                .queryParam("timezone", "auto")
                                .build())
                        .retrieve()
                        .body(String.class);
            } catch (RestClientException ex) {
                lastFailure = ex;
            }
        }
        throw new IllegalStateException("Open-Meteo request failed after " + MAX_ATTEMPTS + " attempts", lastFailure);
    }
}
