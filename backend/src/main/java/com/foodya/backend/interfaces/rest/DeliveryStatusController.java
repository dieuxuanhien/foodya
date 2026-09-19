package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.DriverSessionView;
import com.foodya.backend.application.ports.in.DriverLifecycleUseCase;
import com.foodya.backend.domain.value_objects.DriverSessionStatus;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.DriverStatusApiResponse;
import com.foodya.backend.interfaces.rest.dto.UpdateDriverLocationApiRequest;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Grab-style driver online/offline toggle endpoints.
 * All endpoints require DELIVERY or ADMIN role (enforced by SecurityConfig).
 *
 * <ul>
 *   <li>POST /status/online   — driver taps GO (go online)</li>
 *   <li>POST /status/offline  — driver taps GO again (go offline / last-trip mode)</li>
 *   <li>GET  /status          — get current driver session status</li>
 *   <li>PUT  /status/location — send GPS location update</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/delivery/status")
@Tag(name = "Driver Lifecycle", description = "Grab-style driver online/offline toggle and location updates")
public class DeliveryStatusController {

    private final DriverLifecycleUseCase driverLifecycleUseCase;

    public DeliveryStatusController(DriverLifecycleUseCase driverLifecycleUseCase) {
        this.driverLifecycleUseCase = driverLifecycleUseCase;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/delivery/status/online
    // -------------------------------------------------------------------------

    @Operation(summary = "Go online", description = "Driver taps the GO button. Creates a new online session. Returns 409 if already online.")
    @PostMapping("/online")
    public ResponseEntity<ApiSuccessResponse<DriverStatusApiResponse>> goOnline(
            Authentication authentication,
            HttpServletRequest request) {

        UUID driverUserId = CurrentUser.userId(authentication);
        DriverSessionView session = driverLifecycleUseCase.toggleOnline(driverUserId);
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(session, null), RequestTrace.from(request)));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/delivery/status/offline
    // -------------------------------------------------------------------------

    @Operation(summary = "Go offline",
            description = "Driver taps GO again. If mid-delivery, enters 'last trip' mode (GOING_OFFLINE) and auto-goes offline after delivering.")
    @PostMapping("/offline")
    public ResponseEntity<ApiSuccessResponse<DriverStatusApiResponse>> goOffline(
            Authentication authentication,
            HttpServletRequest request) {

        UUID driverUserId = CurrentUser.userId(authentication);
        DriverSessionView session = driverLifecycleUseCase.toggleOffline(driverUserId);

        String message = session.status() == DriverSessionStatus.GOING_OFFLINE
                ? "Will go offline after current delivery is completed"
                : null;

        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(session, message), RequestTrace.from(request)));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/delivery/status
    // -------------------------------------------------------------------------

    @Operation(summary = "Get driver status", description = "Returns the driver's current session status. Returns OFFLINE if no active session.")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<DriverStatusApiResponse>> getStatus(
            Authentication authentication,
            HttpServletRequest request) {

        UUID driverUserId = CurrentUser.userId(authentication);
        DriverSessionView session = driverLifecycleUseCase.getActiveSession(driverUserId);
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(session, null), RequestTrace.from(request)));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/delivery/status/location
    // -------------------------------------------------------------------------

    @Operation(summary = "Update location", description = "Driver sends their current GPS coordinates. Updates H3 cell for spatial matching.")
    @PutMapping("/location")
    public ResponseEntity<ApiSuccessResponse<DriverStatusApiResponse>> updateLocation(
            @Valid @RequestBody UpdateDriverLocationApiRequest locationRequest,
            Authentication authentication,
            HttpServletRequest request) {

        UUID driverUserId = CurrentUser.userId(authentication);
        DriverSessionView session = driverLifecycleUseCase.updateLocation(
                driverUserId, locationRequest.lat(), locationRequest.lng());
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(session, null), RequestTrace.from(request)));
    }

    // -------------------------------------------------------------------------
    // Mapping helper
    // -------------------------------------------------------------------------

    private DriverStatusApiResponse toResponse(DriverSessionView view, String message) {
        return new DriverStatusApiResponse(
                view.sessionId(),
                view.status(),
                view.startedAt(),
                view.endedAt(),
                view.lastHeartbeat(),
                view.currentLat(),
                view.currentLng(),
                view.h3IndexRes9(),
                view.activeOrderCount(),
                view.maxConcurrentOrders(),
                message
        );
    }
}
