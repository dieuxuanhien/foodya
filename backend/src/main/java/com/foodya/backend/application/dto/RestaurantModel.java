package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.RestaurantStatus;

import java.math.BigDecimal;
import java.util.UUID;

public class RestaurantModel {

    private UUID id;
    private UUID ownerUserId;
    private String name;
    private String cuisineType;
    private String description;
    private String addressLine;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String h3IndexRes9;
    private BigDecimal avgRating;
    private int reviewCount;
    private RestaurantStatus status;
    private boolean open;
    private BigDecimal maxDeliveryKm;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCuisineType() {
        return cuisineType;
    }

    public void setCuisineType(String cuisineType) {
        this.cuisineType = cuisineType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getH3IndexRes9() {
        return h3IndexRes9;
    }

    public void setH3IndexRes9(String h3IndexRes9) {
        this.h3IndexRes9 = h3IndexRes9;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public RestaurantStatus getStatus() {
        return status;
    }

    public void setStatus(RestaurantStatus status) {
        this.status = status;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public BigDecimal getMaxDeliveryKm() {
        return maxDeliveryKm;
    }

    public void setMaxDeliveryKm(BigDecimal maxDeliveryKm) {
        this.maxDeliveryKm = maxDeliveryKm;
    }
}