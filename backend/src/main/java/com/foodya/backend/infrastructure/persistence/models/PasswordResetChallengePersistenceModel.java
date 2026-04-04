package com.foodya.backend.infrastructure.persistence.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * PERSISTENCE MODEL — JPA-annotated version of PasswordResetChallenge domain entity.
 *
 * This class handles ONLY database mapping concerns. All business logic
 * stays in the domain entity (PasswordResetChallenge). Mappers convert between this model
 * and the domain entity at adapter boundaries.
 *
 * REASON: Clean Architecture principle — domain layer must not depend on
 * persistence framework. This separation makes the domain reusable across
 * technologies and testable independently.
 *
 * Invariant: Always convert to/from PasswordResetChallenge domain entity at infrastructure
 * boundaries via AuthPersistenceMapper.
 */
@Entity
@Table(name = "password_reset_challenges")
public class PasswordResetChallengePersistenceModel {

    @Id
    private UUID id;

    @Column(name = "challenge_token", nullable = false, length = 128, unique = true)
    private String challengeToken;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountPersistenceModel user;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
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

    public UserAccountPersistenceModel getUser() {
        return user;
    }

    public void setUser(UserAccountPersistenceModel user) {
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
}
