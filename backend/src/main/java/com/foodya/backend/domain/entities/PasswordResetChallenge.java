package com.foodya.backend.domain.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PasswordResetChallenge {

    private UUID id;

    private String challengeToken;

    private UserAccount user;

    private String otpHash;

    private OffsetDateTime expiresAt;

    private OffsetDateTime verifiedAt;

    private OffsetDateTime consumedAt;

    private OffsetDateTime createdAt;

    public void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = OffsetDateTime.now();
    }

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

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(OffsetDateTime.now());
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean verifyOtp(String inputOtp, java.util.function.BiPredicate<String, String> matcher) {
        if (isConsumed() || isExpired()) {
            return false;
        }
        return matcher != null && matcher.test(inputOtp, otpHash);
    }

    public void verifyOtp(String inputOtp) {
        if (isConsumed()) {
            throw new IllegalStateException("Challenge is already consumed");
        }
        if (isExpired()) {
            throw new IllegalStateException("Challenge has expired");
        }
        if (inputOtp == null || inputOtp.isBlank()) {
            throw new IllegalArgumentException("OTP input cannot be blank");
        }
        this.verifiedAt = OffsetDateTime.now();
    }

    public void consume() {
        if (isConsumed() || isExpired()) {
            throw new IllegalStateException("cannot consume expired or already consumed challenge");
        }
        this.consumedAt = OffsetDateTime.now();
    }
}
