package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.infrastructure.integration.GoongMapsClient;
import com.uber.h3core.H3Core;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

@Component
public class GeoAdapter implements GeoPort {

    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final int H3_RESTAURANT_RES = 9;

    private final H3Core h3Core;
    private final GoongMapsClient goongMapsClient;
    private final ObjectMapper objectMapper;

    public GeoAdapter(GoongMapsClient goongMapsClient, ObjectMapper objectMapper) {
        this.goongMapsClient = goongMapsClient;
        this.objectMapper = objectMapper;
        try {
            this.h3Core = H3Core.newInstance();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize H3", ex);
        }
    }

    @Override
    public String h3Res9(double lat, double lng) {
        return h3Core.geoToH3Address(lat, lng, H3_RESTAURANT_RES);
    }

    @Override
    public Set<String> h3KRingRes9(double lat, double lng, double radiusKm) {
        String center = h3Res9(lat, lng);
        int ringSize = Math.max(1, (int) Math.ceil(radiusKm / 0.5));
        return new HashSet<>(h3Core.kRing(center, ringSize));
    }

    @Override
    public BigDecimal haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;
        return BigDecimal.valueOf(distance).setScale(3, RoundingMode.HALF_UP);
    }

    @Override
    public String reverseGeocodeVi(double lat, double lng) {
        String raw = goongMapsClient.reverseGeocodeRaw(lat + "," + lng);
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode firstResult = root.path("results").path(0);
            String formattedAddress = firstResult.path("formatted_address").asText(null);
            if (formattedAddress == null || formattedAddress.isBlank()) {
                throw new IllegalStateException("Goong response missing formatted_address");
            }
            return formattedAddress;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot parse Goong reverse geocode", ex);
        }
    }

    @Override
    public String geoToH3Index(double lat, double lng, int resolution) {
        return h3Core.geoToH3Address(lat, lng, resolution);
    }
}