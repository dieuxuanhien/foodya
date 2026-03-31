package com.foodya.backend.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AiChatHistoryModel {

    private UUID id;
    private UUID userId;
    private String prompt;
    private String responseSummary;
    private BigDecimal contextLatitude;
    private BigDecimal contextLongitude;
    private String weatherH3IndexRes8;
    private OffsetDateTime createdAt;

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
}