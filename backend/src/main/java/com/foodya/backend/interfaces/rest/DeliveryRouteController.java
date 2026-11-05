package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.DeliveryRouteUseCase;
import com.foodya.backend.domain.entities.DeliveryRoute;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.DeliveryRouteApiResponse;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Delivery Routes", description = "Planned delivery route (polyline, waypoints, ETA)")
public class DeliveryRouteController {

    private final DeliveryRouteUseCase deliveryRouteUseCase;

    public DeliveryRouteController(DeliveryRouteUseCase deliveryRouteUseCase) {
        this.deliveryRouteUseCase = deliveryRouteUseCase;
    }

    @Operation(summary = "Get planned route (driver)",
            description = "Returns the planned route for an order assigned to the calling driver.")
    @GetMapping("/api/v1/delivery/orders/{orderId}/route")
    public ResponseEntity<ApiSuccessResponse<DeliveryRouteApiResponse>> driverRoute(
            @PathVariable UUID orderId, Authentication authentication, HttpServletRequest request) {
        DeliveryRoute route = deliveryRouteUseCase.getRouteForDriver(orderId, CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(route), RequestTrace.from(request)));
    }

    @Operation(summary = "Get planned route (customer)",
            description = "Returns the planned route for one of the calling customer's orders.")
    @GetMapping("/api/v1/customer/orders/{orderId}/route")
    public ResponseEntity<ApiSuccessResponse<DeliveryRouteApiResponse>> customerRoute(
            @PathVariable UUID orderId, Authentication authentication, HttpServletRequest request) {
        DeliveryRoute route = deliveryRouteUseCase.getRouteForCustomer(orderId, CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(toResponse(route), RequestTrace.from(request)));
    }

    private DeliveryRouteApiResponse toResponse(DeliveryRoute r) {
        return new DeliveryRouteApiResponse(r.getEncodedPolyline(), r.getDistanceMeters(), r.getDurationSeconds(),
                r.getWaypoints().stream()
                        .map(w -> new DeliveryRouteApiResponse.Waypoint(w.lat(), w.lng(), w.type())).toList(),
                r.getCalculatedAt());
    }
}
