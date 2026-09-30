package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.DriverSessionView;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.DriverLifecycleUseCase;
import com.foodya.backend.application.ports.out.DriverOnlineSessionPort;
import com.foodya.backend.application.ports.out.GeoPort;
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
    private static final int H3_DRIVER_RESOLUTION = 9;

    private final DriverOnlineSessionPort driverOnlineSessionPort;
    private final GeoPort geoPort;

    public DriverLifecycleService(DriverOnlineSessionPort driverOnlineSessionPort,
                                  GeoPort geoPort) {
        this.driverOnlineSessionPort = driverOnlineSessionPort;
        this.geoPort = geoPort;
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

        String h3Index = geoPort.geoToH3Index(lat, lng, H3_DRIVER_RESOLUTION);
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
