package com.foodya.backend.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PasswordResetChallengeData {

    private UUID id;
    private String challengeToken;
    private UserAccountData user;
    private String otpHash;
    private OffsetDateTime expiresAt;
    private OffsetDateTime verifiedAt;
    private OffsetDateTime consumedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getChallengeToken() {
        return challengeToken;
    }

    public void setChallengeToken(String challengeToken) {
        this.challengeToken = challengeToken;
    }

    public UserAccountData getUser() {
        return user;
    }

    public void setUser(UserAccountData user) {
        this.user = user;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(OffsetDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(OffsetDateTime.now());
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean verifyOtp(String inputOtp, com.foodya.backend.application.ports.out.PasswordHashPort passwordHashPort) {
        if (isConsumed() || isExpired()) {
            return false;
        }
        return passwordHashPort != null && passwordHashPort.matches(inputOtp, otpHash);
    }

    public void consume() {
        if (isConsumed() || isExpired()) {
            throw new IllegalStateException("cannot consume expired or already consumed challenge");
        }
        this.consumedAt = OffsetDateTime.now();
    }
}