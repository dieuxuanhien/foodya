package com.foodya.backend.infrastructure.persistence.models;

import com.foodya.backend.domain.value_objects.BatchStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_batches")
public class OrderBatchPersistenceModel {

    @Id
    private UUID id;

    @Column(name = "driver_user_id", nullable = false)
    private UUID driverUserId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchStatus status;

    @Column(name = "pickup_sequence", nullable = false, columnDefinition = "text")
    private String pickupSequence;

    @Column(name = "delivery_sequence", nullable = false, columnDefinition = "text")
    private String deliverySequence;

    @Column(name = "total_distance_meters")
    private Integer totalDistanceMeters;

    @Column(name = "total_duration_seconds")
    private Integer totalDurationSeconds;

    @Column(name = "optimized_route_polyline", columnDefinition = "text")
    private String optimizedRoutePolyline;

    @Column(name = "solver_metadata", columnDefinition = "text")
    private String solverMetadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = BatchStatus.PENDING;
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDriverUserId() { return driverUserId; }
    public void setDriverUserId(UUID driverUserId) { this.driverUserId = driverUserId; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }

    public String getPickupSequence() { return pickupSequence; }
    public void setPickupSequence(String pickupSequence) { this.pickupSequence = pickupSequence; }

    public String getDeliverySequence() { return deliverySequence; }
    public void setDeliverySequence(String deliverySequence) { this.deliverySequence = deliverySequence; }

    public Integer getTotalDistanceMeters() { return totalDistanceMeters; }
    public void setTotalDistanceMeters(Integer totalDistanceMeters) { this.totalDistanceMeters = totalDistanceMeters; }

    public Integer getTotalDurationSeconds() { return totalDurationSeconds; }
    public void setTotalDurationSeconds(Integer totalDurationSeconds) { this.totalDurationSeconds = totalDurationSeconds; }

    public String getOptimizedRoutePolyline() { return optimizedRoutePolyline; }
    public void setOptimizedRoutePolyline(String optimizedRoutePolyline) { this.optimizedRoutePolyline = optimizedRoutePolyline; }

    public String getSolverMetadata() { return solverMetadata; }
    public void setSolverMetadata(String solverMetadata) { this.solverMetadata = solverMetadata; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}
