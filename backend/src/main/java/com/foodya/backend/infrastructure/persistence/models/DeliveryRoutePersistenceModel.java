package com.foodya.backend.infrastructure.persistence.models;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_routes")
public class DeliveryRoutePersistenceModel {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "encoded_polyline", nullable = false, columnDefinition = "text")
    private String encodedPolyline;

    @Column(name = "distance_meters", nullable = false)
    private int distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "waypoints", columnDefinition = "text")
    private String waypoints;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

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
    public String getWaypoints() { return waypoints; }
    public void setWaypoints(String w) { this.waypoints = w; }
    public OffsetDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(OffsetDateTime t) { this.calculatedAt = t; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
