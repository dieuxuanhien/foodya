package com.foodya.backend.infrastructure.adapter.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodya.backend.application.ports.out.RouteCalculationPort;
import com.foodya.backend.domain.value_objects.LatLng;
import com.foodya.backend.domain.value_objects.PolylineCodec;
import com.foodya.backend.domain.value_objects.RouteLeg;
import com.foodya.backend.domain.value_objects.RouteResult;
import com.foodya.backend.infrastructure.integration.GoongMapsClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoongTripAdapter implements RouteCalculationPort {

    private static final String VEHICLE = "bike";

    private final GoongMapsClient goongMapsClient;
    private final ObjectMapper objectMapper;

    public GoongTripAdapter(GoongMapsClient goongMapsClient, ObjectMapper objectMapper) {
        this.goongMapsClient = goongMapsClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Trip API reorders waypoints, so to honour the caller's order we route each consecutive pair
     * (no intermediate points => order is forced) and stitch the polylines together.
     */
    @Override
    public RouteResult calculateRoute(LatLng origin, LatLng destination, List<LatLng> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return trip(origin, destination, List.of());
        }
        List<LatLng> chain = new ArrayList<>();
        chain.add(origin);
        chain.addAll(waypoints);
        chain.add(destination);

        List<LatLng> merged = new ArrayList<>();
        List<RouteLeg> legs = new ArrayList<>();
        int distance = 0;
        int duration = 0;
        for (int i = 0; i < chain.size() - 1; i++) {
            RouteResult part = trip(chain.get(i), chain.get(i + 1), List.of());
            List<LatLng> decoded = PolylineCodec.decode(part.encodedPolyline());
            if (!merged.isEmpty() && !decoded.isEmpty()) {
                decoded = decoded.subList(1, decoded.size());
            }
            merged.addAll(decoded);
            legs.addAll(part.legs());
            distance += part.distanceMeters();
            duration += part.durationSeconds();
        }
        return new RouteResult(PolylineCodec.encode(merged), distance, duration, legs);
    }

    @Override
    public RouteResult calculateOptimizedRoute(LatLng origin, LatLng destination, List<LatLng> waypoints) {
        return trip(origin, destination, waypoints == null ? List.of() : waypoints);
    }

    private RouteResult trip(LatLng origin, LatLng destination, List<LatLng> waypoints) {
        String wp = waypoints.stream().map(LatLng::toGoongString).collect(Collectors.joining(";"));
        String raw = goongMapsClient.tripRouteRaw(origin.toGoongString(), destination.toGoongString(),
                wp, VEHICLE, false);
        return parse(raw);
    }

    RouteResult parse(String raw) {
        try {
            JsonNode route = objectMapper.readTree(raw).path("routes").path(0);
            if (route.isMissingNode()) {
                throw new IllegalStateException("Goong response has no routes");
            }
            JsonNode polyNode = route.path("overview_polyline");
            String polyline = polyNode.isTextual() ? polyNode.asText() : polyNode.path("points").asText("");
            if (polyline.isEmpty()) {
                throw new IllegalStateException("Goong response missing polyline");
            }
            List<RouteLeg> legs = new ArrayList<>();
            int distance = 0;
            int duration = 0;
            for (JsonNode leg : route.path("legs")) {
                int d = leg.path("distance").path("value").asInt(0);
                int t = leg.path("duration").path("value").asInt(0);
                legs.add(new RouteLeg(d, t));
                distance += d;
                duration += t;
            }
            return new RouteResult(polyline, distance, duration, legs);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot parse Goong trip response", ex);
        }
    }
}
