package com.foodya.backend.domain.entities;

import com.foodya.backend.domain.value_objects.RestaurantStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Restaurant {

    private UUID id;

    private UUID ownerUserId;

    private String name;

    private String cuisineType;

    private List<String> cuisineTypes = new ArrayList<>();

    private String imageUrl;

    private String backgroundImageUrl;

    private String avatarImageUrl;

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

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

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

    public List<String> getCuisineTypes() {
        return cuisineTypes;
    }

    public void setCuisineTypes(List<String> cuisineTypes) {
        this.cuisineTypes = cuisineTypes == null ? new ArrayList<>() : new ArrayList<>(cuisineTypes);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getBackgroundImageUrl() {
        return backgroundImageUrl;
    }

    public void setBackgroundImageUrl(String backgroundImageUrl) {
        this.backgroundImageUrl = backgroundImageUrl;
        this.imageUrl = backgroundImageUrl;
    }

    public String getAvatarImageUrl() {
        return avatarImageUrl;
    }

    public void setAvatarImageUrl(String avatarImageUrl) {
        this.avatarImageUrl = avatarImageUrl;
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

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public RestaurantStatus getStatus() {
        return status;
    }

    public void setStatus(RestaurantStatus status) {
        this.status = status;
    }

    public BigDecimal getMaxDeliveryKm() {
        return maxDeliveryKm;
    }

    public void setMaxDeliveryKm(BigDecimal maxDeliveryKm) {
        this.maxDeliveryKm = maxDeliveryKm;
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

    public void approve() {
        this.status = RestaurantStatus.ACTIVE;
        this.updatedAt = OffsetDateTime.now();
    }

    public void suspend() {
        this.status = RestaurantStatus.INACTIVE;
        this.updatedAt = OffsetDateTime.now();
    }

    public void toggleOperatingStatus(boolean open) {
        this.open = open;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateRating(BigDecimal newRating, int newCount) {
        if (newRating != null && (newRating.compareTo(BigDecimal.ZERO) < 0 || newRating.compareTo(new BigDecimal("5.0")) > 0)) {
            throw new IllegalArgumentException("Rating must be between 0.0 and 5.0");
        }
        if (newCount < 0) {
            throw new IllegalArgumentException("Review count cannot be negative");
        }
        this.avgRating = newRating;
        this.reviewCount = newCount;
        this.updatedAt = OffsetDateTime.now();
    }
}
