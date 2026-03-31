package com.foodya.backend.infrastructure.integration;

import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import com.foodya.backend.infrastructure.config.IntegrationKey;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GoongMapsAdapter {

    private static final String GOONG_BASE_URL = "https://rsapi.goong.io";

    private final RestClient restClient;
    private final ApiSecretsProvider apiSecretsProvider;

    public GoongMapsAdapter(ApiSecretsProvider apiSecretsProvider) {
        this.apiSecretsProvider = apiSecretsProvider;
        this.restClient = RestClient.builder().baseUrl(GOONG_BASE_URL).build();
    }

    public String routeDistanceRaw(String originLatLng, String destinationLatLng) {
        String apiKey = apiSecretsProvider.get(IntegrationKey.GOONG_API_KEY)
                .orElseThrow(() -> new IllegalStateException("Missing Goong API key"));

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/Direction")
                        .queryParam("origin", originLatLng)
                        .queryParam("destination", destinationLatLng)
                        .queryParam("vehicle", "bike")
                        .queryParam("api_key", apiKey)
                        .build())
                .retrieve()
                .body(String.class);
    }
}
