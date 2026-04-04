package com.foodya.backend.infrastructure.adapter.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.ports.out.RouteDistancePort;
import com.foodya.backend.infrastructure.integration.GoongMapsClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class GoongRouteDistanceAdapter implements RouteDistancePort {

    private final GoongMapsClient goongMapsClient;
    private final ObjectMapper objectMapper;

    public GoongRouteDistanceAdapter(GoongMapsClient goongMapsClient, ObjectMapper objectMapper) {
        this.goongMapsClient = goongMapsClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public BigDecimal routeDistanceKm(double originLat,
                                      double originLng,
                                      double destinationLat,
                                      double destinationLng) {
        String raw = goongMapsClient.routeDistanceRaw(
                originLat + "," + originLng,
                destinationLat + "," + destinationLng
        );

        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode metersNode = root.path("routes").path(0).path("legs").path(0).path("distance").path("value");
            if (!metersNode.isNumber()) {
                throw new IllegalStateException("Goong response missing distance");
            }
            BigDecimal meters = metersNode.decimalValue();
            return meters.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot parse Goong route distance", ex);
        }
    }
}
