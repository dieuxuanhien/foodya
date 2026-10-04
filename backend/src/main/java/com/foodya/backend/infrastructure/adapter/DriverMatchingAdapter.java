package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.DriverMatchingPort;
import com.foodya.backend.application.ports.out.DriverOnlineSessionPort;
import com.foodya.backend.application.ports.out.GeoPort;
import com.foodya.backend.domain.entities.DriverOnlineSession;
import com.foodya.backend.domain.value_objects.DriverMatchRequest;
import com.foodya.backend.domain.value_objects.DriverMatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Infrastructure adapter implementing intelligent driver matching.
 * <p>
 * Algorithm:
 * 1. H3 k-ring expansion from restaurant (k=3 → k=10 → k=20)
 * 2. Filter to drivers that can accept orders
 * 3. Score by: proximity (50%), capacity (20%), freshness (10%), neutral quality (20%)
 * 4. Return highest-scoring driver
 */
@Component
public class DriverMatchingAdapter implements DriverMatchingPort {

    private static final Logger log = LoggerFactory.getLogger(DriverMatchingAdapter.class);

    // k-ring expansion tiers: ~1km, ~3km, ~6km at H3 res9
    private static final double[] SEARCH_RADII_KM = {1.0, 3.0, 6.0};

    // Scoring weights
    private static final double WEIGHT_PROXIMITY = 0.50;
    private static final double WEIGHT_QUALITY = 0.20;
    private static final double WEIGHT_CAPACITY = 0.20;
    private static final double WEIGHT_FRESHNESS = 0.10;

    private final DriverOnlineSessionPort driverOnlineSessionPort;
    private final GeoPort geoPort;

    public DriverMatchingAdapter(DriverOnlineSessionPort driverOnlineSessionPort,
                                 GeoPort geoPort) {
        this.driverOnlineSessionPort = driverOnlineSessionPort;
        this.geoPort = geoPort;
    }

    @Override
    public Optional<DriverMatchResult> findBestDriver(DriverMatchRequest request) {
        for (double radiusKm : SEARCH_RADII_KM) {
            Set<String> cells = geoPort.h3KRingRes9(
                    request.restaurantLat(), request.restaurantLng(), radiusKm);

            if (cells.isEmpty()) {
                continue;
            }

            List<DriverOnlineSession> candidates =
                    driverOnlineSessionPort.findOnlineDriversInH3Cells(cells);

            // Filter to drivers that can actually accept an order
            List<DriverOnlineSession> available = candidates.stream()
                    .filter(DriverOnlineSession::canAcceptOrder)
                    .toList();

            if (available.isEmpty()) {
                log.debug("No available drivers in {}km radius ({} cells)", radiusKm, cells.size());
                continue;
            }

            log.info("Found {} available drivers in {}km radius", available.size(), radiusKm);

            Optional<DriverMatchResult> best = available.stream()
                    .map(session -> scoreDriver(session, request.restaurantLat(), request.restaurantLng()))
                    .max(Comparator.comparingDouble(DriverMatchResult::score));

            if (best.isPresent()) {
                log.info("Best driver match: driver={}, distance={}km, score={}",
                        best.get().driverUserId(), 
                        String.format("%.2f", best.get().distanceKm()),
                        String.format("%.3f", best.get().score()));
                return best;
            }
        }

        log.warn("No driver found for restaurant at ({}, {})",
                request.restaurantLat(), request.restaurantLng());
        return Optional.empty();
    }

    /**
     * Scores a driver candidate. Higher score = better match.
     */
    private DriverMatchResult scoreDriver(DriverOnlineSession session,
                                          double restaurantLat, double restaurantLng) {
        // 1. Proximity: inverse distance (closer = higher score), 0-1 range
        double distanceKm = 0.0;
        if (session.getCurrentLat() != null && session.getCurrentLng() != null) {
            distanceKm = geoPort.haversineKm(
                    session.getCurrentLat(), session.getCurrentLng(),
                    restaurantLat, restaurantLng
            ).doubleValue();
        }
        double proximityScore = 1.0 / (1.0 + distanceKm);

        // 2. Quality: neutral 1.0 (no driver rating data yet)
        double qualityScore = 1.0;

        // 3. Capacity: fraction of remaining order slots
        double capacityScore = 1.0 - ((double) session.getActiveOrderCount() / session.getMaxConcurrentOrders());

        // 4. Freshness: minutes since last heartbeat (more recent = driver is actively updating location)
        double minutesSinceHeartbeat = 0.0;
        if (session.getLastHeartbeat() != null) {
            minutesSinceHeartbeat = Duration.between(session.getLastHeartbeat(), Instant.now()).toMinutes();
        }
        // Invert: fresher heartbeat = higher score. Cap at 60 min.
        double freshnessScore = 1.0 - Math.min(minutesSinceHeartbeat / 60.0, 1.0);

        double totalScore = proximityScore * WEIGHT_PROXIMITY
                + qualityScore * WEIGHT_QUALITY
                + capacityScore * WEIGHT_CAPACITY
                + freshnessScore * WEIGHT_FRESHNESS;

        return new DriverMatchResult(
                session.getDriverUserId(),
                session.getId(),
                distanceKm,
                totalScore
        );
    }
}
