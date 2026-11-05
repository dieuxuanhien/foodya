package com.foodya.backend.domain.entities;

import com.foodya.backend.domain.value_objects.RouteWaypoint;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Planned route for an order (shared polyline when the order belongs to a batch). Pure domain object. */
public class DeliveryRoute {

    private UUID id;
    private UUID orderId;
    private UUID batchId;
    private String encodedPolyline;
    private int distanceMeters;
    private int durationSeconds;
    private List<RouteWaypoint> waypoints;
    private Instant calculatedAt;
    private boolean active;

    public DeliveryRoute() {}

    public static DeliveryRoute create(UUID orderId, UUID batchId, String encodedPolyline,
                                       int distanceMeters, int durationSeconds,
                                       List<RouteWaypoint> waypoints) {
        DeliveryRoute r = new DeliveryRoute();
        r.id = UUID.randomUUID();
        r.orderId = orderId;
        r.batchId = batchId;
        r.encodedPolyline = encodedPolyline;
        r.distanceMeters = distanceMeters;
        r.durationSeconds = durationSeconds;
        r.waypoints = waypoints;
        r.calculatedAt = Instant.now();
        r.active = true;
        return r;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }
    public String getEncodedPolyline() { return encodedPolyline; }
    public void setEncodedPolyline(String p) { this.encodedPolyline = p; }
    public int getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(int d) { this.distanceMeters = d; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int d) { this.durationSeconds = d; }
    public List<RouteWaypoint> getWaypoints() { return waypoints; }
    public void setWaypoints(List<RouteWaypoint> w) { this.waypoints = w; }
    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant t) { this.calculatedAt = t; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
