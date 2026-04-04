package com.foodya.backend.infrastructure.persistence.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_chat_histories")
public class AiChatHistoryPersistenceModel {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(name = "response_summary", nullable = false, columnDefinition = "text")
    private String responseSummary;

    @Column(name = "context_latitude", precision = 10, scale = 7)
    private BigDecimal contextLatitude;

    @Column(name = "context_longitude", precision = 10, scale = 7)
    private BigDecimal contextLongitude;

    @Column(name = "weather_h3_index_res8", length = 32)
    private String weatherH3IndexRes8;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResponseSummary() {
        return responseSummary;
    }

    public void setResponseSummary(String responseSummary) {
        this.responseSummary = responseSummary;
    }

    public BigDecimal getContextLatitude() {
        return contextLatitude;
    }

    public void setContextLatitude(BigDecimal contextLatitude) {
        this.contextLatitude = contextLatitude;
    }

    public BigDecimal getContextLongitude() {
        return contextLongitude;
    }

    public void setContextLongitude(BigDecimal contextLongitude) {
        this.contextLongitude = contextLongitude;
    }

    public String getWeatherH3IndexRes8() {
        return weatherH3IndexRes8;
    }

    public void setWeatherH3IndexRes8(String weatherH3IndexRes8) {
        this.weatherH3IndexRes8 = weatherH3IndexRes8;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
