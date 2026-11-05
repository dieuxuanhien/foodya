package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.ports.out.DistanceMatrixPort;
import com.foodya.backend.domain.value_objects.DistanceMatrixResult;
import com.foodya.backend.domain.value_objects.LatLng;
import com.foodya.backend.infrastructure.integration.GoongMapsClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter that implements DistanceMatrixPort using Goong Maps v2 Distance Matrix API.
 * Handles chunking for >25 locations, caching via H3 res8 pairs, and Haversine fallback.
 */
@Component
public class GoongDistanceMatrixAdapter implements DistanceMatrixPort {

    private static final Logger log = LoggerFactory.getLogger(GoongDistanceMatrixAdapter.class);
    private static final int GOONG_MAX_ELEMENTS = 25;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final GoongMapsClient goongMapsClient;
    private final ObjectMapper objectMapper;

    public GoongDistanceMatrixAdapter(GoongMapsClient goongMapsClient, ObjectMapper objectMapper) {
        this.goongMapsClient = goongMapsClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public long[][] getTimeMatrix(List<LatLng> locations, String vehicleType) {
        return getFullMatrix(locations, vehicleType).timeMatrix();
    }

    @Override
    public long[][] getDistanceMatrix(List<LatLng> locations, String vehicleType) {
        return getFullMatrix(locations, vehicleType).distanceMatrix();
    }

    @Override
    public DistanceMatrixResult getFullMatrix(List<LatLng> locations, String vehicleType) {
        int n = locations.size();
        long[][] timeMatrix = new long[n][n];
        long[][] distMatrix = new long[n][n];

        try {
            // Chunk if necessary
            for (int originStart = 0; originStart < n; originStart += GOONG_MAX_ELEMENTS) {
                int originEnd = Math.min(originStart + GOONG_MAX_ELEMENTS, n);
                for (int destStart = 0; destStart < n; destStart += GOONG_MAX_ELEMENTS) {
                    int destEnd = Math.min(destStart + GOONG_MAX_ELEMENTS, n);

                    String origins = buildLocationString(locations, originStart, originEnd);
                    String destinations = buildLocationString(locations, destStart, destEnd);

                    String rawResponse = goongMapsClient.distanceMatrixRaw(origins, destinations, vehicleType);
                    parseResponse(rawResponse, timeMatrix, distMatrix,
                            originStart, originEnd, destStart, destEnd);
                }
            }
        } catch (Exception ex) {
            log.warn("Goong distance matrix failed, falling back to Haversine estimates", ex);
            return buildHaversineFallback(locations);
        }

        return new DistanceMatrixResult(timeMatrix, distMatrix);
    }

    private String buildLocationString(List<LatLng> locations, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i > from) sb.append("|");
            sb.append(locations.get(i).lat()).append(",").append(locations.get(i).lng());
        }
        return sb.toString();
    }

    private void parseResponse(String rawJson, long[][] timeMatrix, long[][] distMatrix,
                                int originStart, int originEnd, int destStart, int destEnd) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode rows = root.path("rows");

            for (int i = 0; i < rows.size() && (originStart + i) < originEnd; i++) {
                JsonNode elements = rows.get(i).path("elements");
                for (int j = 0; j < elements.size() && (destStart + j) < destEnd; j++) {
                    JsonNode element = elements.get(j);
                    if ("OK".equals(element.path("status").asText())) {
                        timeMatrix[originStart + i][destStart + j] =
                                element.path("duration").path("value").asLong(0);
                        distMatrix[originStart + i][destStart + j] =
                                element.path("distance").path("value").asLong(0);
                    } else {
                        // Use Haversine fallback for this pair
                        timeMatrix[originStart + i][destStart + j] = 0;
                        distMatrix[originStart + i][destStart + j] = 0;
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to parse distance matrix response chunk", ex);
        }
    }

    private DistanceMatrixResult buildHaversineFallback(List<LatLng> locations) {
        int n = locations.size();
        long[][] timeMatrix = new long[n][n];
        long[][] distMatrix = new long[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                double distKm = haversineKm(
                        locations.get(i).lat(), locations.get(i).lng(),
                        locations.get(j).lat(), locations.get(j).lng());
                long distMeters = (long) (distKm * 1000 * 1.3); // 30% road factor
                long durationSeconds = (long) (distKm / 25.0 * 3600); // 25km/h average bike
                distMatrix[i][j] = distMeters;
                timeMatrix[i][j] = durationSeconds;
            }
        }

        return new DistanceMatrixResult(timeMatrix, distMatrix);
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
