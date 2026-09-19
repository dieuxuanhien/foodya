package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.DriverSessionView;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.DriverLifecycleUseCase;
import com.foodya.backend.application.ports.out.DriverOnlineSessionPort;
import com.foodya.backend.domain.entities.DriverOnlineSession;
import com.foodya.backend.domain.value_objects.DriverSessionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service implementing the Grab-style driver lifecycle toggle.
 * No framework annotations — wired explicitly in AppConfig.
 */
public class DriverLifecycleService implements DriverLifecycleUseCase {

    private static final Logger log = LoggerFactory.getLogger(DriverLifecycleService.class);

    private final DriverOnlineSessionPort driverOnlineSessionPort;

    public DriverLifecycleService(DriverOnlineSessionPort driverOnlineSessionPort) {
        this.driverOnlineSessionPort = driverOnlineSessionPort;
    }

    // -------------------------------------------------------------------------
    // Use case: toggle online (driver taps GO)
    // -------------------------------------------------------------------------

    @Override
    public DriverSessionView toggleOnline(UUID driverUserId) {
        Optional<DriverOnlineSession> existing = driverOnlineSessionPort.findActiveSession(driverUserId);

        if (existing.isPresent()) {
            DriverSessionStatus currentStatus = existing.get().getStatus();
            if (currentStatus != DriverSessionStatus.OFFLINE && currentStatus != DriverSessionStatus.SUSPENDED) {
                throw new ValidationException(
                        "driver is already online",
                        Map.of("status", "current status is " + currentStatus.name() + "; cannot go online again"));
            }
        }

        // Create a new session
        DriverOnlineSession session = new DriverOnlineSession();
        session.goOnline(driverUserId);
        DriverOnlineSession saved = driverOnlineSessionPort.save(session);

        log.info("Driver {} went ONLINE, session {}", driverUserId, saved.getId());
        return toView(saved);
    }

    // -------------------------------------------------------------------------
    // Use case: toggle offline (driver taps GO again)
    // -------------------------------------------------------------------------

    @Override
    public DriverSessionView toggleOffline(UUID driverUserId) {
        DriverOnlineSession session = requireActiveSession(driverUserId);

        if (session.getStatus() == DriverSessionStatus.OFFLINE) {
            throw new ValidationException(
                    "driver is already offline",
                    Map.of("status", "current status is OFFLINE"));
        }

        try {
            session.requestOffline();
        } catch (IllegalStateException ex) {
            throw new ValidationException("cannot go offline from current state",
                    Map.of("status", ex.getMessage()));
        }

        DriverOnlineSession saved = driverOnlineSessionPort.save(session);
        log.info("Driver {} toggled offline → status={}, session={}",
                driverUserId, saved.getStatus(), saved.getId());
        return toView(saved);
    }

    // -------------------------------------------------------------------------
    // Use case: update location
    // -------------------------------------------------------------------------

    @Override
    public DriverSessionView updateLocation(UUID driverUserId, double lat, double lng) {
        DriverOnlineSession session = requireActiveSession(driverUserId);

        if (session.getStatus() == DriverSessionStatus.OFFLINE
                || session.getStatus() == DriverSessionStatus.SUSPENDED) {
            throw new ValidationException(
                    "cannot update location when offline or suspended",
                    Map.of("status", "current status is " + session.getStatus().name()));
        }

        // Compute a simple H3 res9 placeholder string.
        // NOTE: In Phase 3, this will be replaced by a real H3IndexPort call.
        // For now we use a placeholder format that will be replaced.
        String h3Index = computeH3Placeholder(lat, lng);
        session.updateLocation(lat, lng, h3Index);

        DriverOnlineSession saved = driverOnlineSessionPort.save(session);
        return toView(saved);
    }

    // -------------------------------------------------------------------------
    // Use case: get current status
    // -------------------------------------------------------------------------

    @Override
    public DriverSessionView getActiveSession(UUID driverUserId) {
        return driverOnlineSessionPort.findActiveSession(driverUserId)
                .map(this::toView)
                .orElse(DriverSessionView.offline(driverUserId));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private DriverOnlineSession requireActiveSession(UUID driverUserId) {
        return driverOnlineSessionPort.findActiveSession(driverUserId)
                .orElseThrow(() -> new ValidationException(
                        "no active session found for driver",
                        Map.of("driverUserId", driverUserId.toString())));
    }

    /**
     * Temporary H3 placeholder until the H3IndexPort (Phase 3) is wired.
     * Encodes lat/lng as a bucketed string at ~res9 granularity (~100m cells).
     * This will be replaced by {@code h3IndexPort.latLngToCell(lat, lng, 9)} in Phase 3.
     */
    private String computeH3Placeholder(double lat, double lng) {
        long latBucket = Math.round(lat * 1000);
        long lngBucket = Math.round(lng * 1000);
        return String.format("h3_%d_%d", latBucket, lngBucket);
    }

    private DriverSessionView toView(DriverOnlineSession session) {
        return new DriverSessionView(
                session.getId(),
                session.getDriverUserId(),
                session.getStatus(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getLastHeartbeat(),
                session.getCurrentLat(),
                session.getCurrentLng(),
                session.getH3IndexRes9(),
                session.getActiveOrderCount(),
                session.getMaxConcurrentOrders()
        );
    }
}
