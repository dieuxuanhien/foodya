package com.foodya.backend.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RefreshTokenModel {

    private UUID id;
    private UserAccountModel user;
    private String tokenJti;
    private String tokenFamily;
    private OffsetDateTime expiresAt;
    private OffsetDateTime revokedAt;
    private String replacedByJti;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserAccountModel getUser() {
        return user;
    }

    public void setUser(UserAccountModel user) {
        this.user = user;
    }

    public String getTokenJti() {
        return tokenJti;
    }

    public void setTokenJti(String tokenJti) {
        this.tokenJti = tokenJti;
    }

    public String getTokenFamily() {
        return tokenFamily;
    }

    public void setTokenFamily(String tokenFamily) {
        this.tokenFamily = tokenFamily;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getReplacedByJti() {
        return replacedByJti;
    }

    public void setReplacedByJti(String replacedByJti) {
        this.replacedByJti = replacedByJti;
    }
}