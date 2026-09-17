package com.foodya.backend.infrastructure.integration;

import com.foodya.backend.infrastructure.config.ApiSecretsProvider;
import com.foodya.backend.application.constants.IntegrationKeyCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GoongMapsClient {

    private static final String GOONG_BASE_URL = "https://rsapi.goong.io";
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MILLIS = 1_000L;

    private final RestClient restClient;
    private final ApiSecretsProvider apiSecretsProvider;

    @Autowired
    public GoongMapsClient(ApiSecretsProvider apiSecretsProvider) {
        this(apiSecretsProvider, RestClient.builder().baseUrl(GOONG_BASE_URL).build());
    }

    public GoongMapsClient(ApiSecretsProvider apiSecretsProvider, RestClient restClient) {
        this.apiSecretsProvider = apiSecretsProvider;
        this.restClient = restClient;
    }

    public String routeDistanceRaw(String originLatLng, String destinationLatLng) {
        String apiKey = apiSecretsProvider.get(IntegrationKeyCatalog.GOONG_API_KEY)
                .orElseThrow(() -> new IllegalStateException("Missing Goong API key"));

        return executeWithRetry(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/direction")
                        .queryParam("origin", originLatLng)
                        .queryParam("destination", destinationLatLng)
                        .queryParam("vehicle", "bike")
                        .queryParam("api_key", apiKey)
                        .build())
                .retrieve()
                .body(String.class));
    }

    public String reverseGeocodeRaw(String latLng) {
        String apiKey = apiSecretsProvider.get(IntegrationKeyCatalog.GOONG_API_KEY)
                .orElseThrow(() -> new IllegalStateException("Missing Goong API key"));

        return executeWithRetry(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/geocode")
                        .queryParam("latlng", latLng)
                        .queryParam("language", "vi")
                        .queryParam("has_deprecated_administrative_unit", false)
                        .queryParam("api_key", apiKey)
                        .build())
                .retrieve()
                .body(String.class));
    }

    public String tripRouteRaw(String originLatLng,
                               String destinationLatLng,
                               String waypoints,
                               String vehicle,
                               boolean roundtrip) {
        String apiKey = apiSecretsProvider.get(IntegrationKeyCatalog.GOONG_API_KEY)
                .orElseThrow(() -> new IllegalStateException("Missing Goong API key"));

        return executeWithRetry(() -> restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/v2/trip")
                            .queryParam("origin", originLatLng)
                            .queryParam("destination", destinationLatLng)
                            .queryParam("vehicle", vehicle == null || vehicle.isBlank() ? "bike" : vehicle)
                            .queryParam("roundtrip", roundtrip)
                            .queryParam("api_key", apiKey);
                    if (waypoints != null && !waypoints.isBlank()) {
                        builder.queryParam("waypoints", waypoints);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(String.class));
    }

    private String executeWithRetry(java.util.function.Supplier<String> request) {
        RestClientException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return request.get();
            } catch (RestClientException ex) {
                lastFailure = ex;
                if (attempt == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    Thread.sleep(INITIAL_BACKOFF_MILLIS << (attempt - 1));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Goong request interrupted", interrupted);
                }
            }
        }
        throw new IllegalStateException("Goong request failed after " + MAX_ATTEMPTS + " attempts", lastFailure);
    }
}
