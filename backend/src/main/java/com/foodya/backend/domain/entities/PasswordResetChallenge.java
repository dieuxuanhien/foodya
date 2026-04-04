package com.foodya.backend.domain.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DOMAIN ENTITY: PasswordResetChallenge
 *
 * This is a PURE domain entity with NO framework dependencies or annotations.
 * - All business logic and state transitions are here
 * - No persistence concerns (JPA, database mapping)
 * - Fully unit-testable without any framework
 * - Framework-independent and reusable across technologies
 *
 * Persistence mapping is handled by:
 * - PasswordResetChallengePersistenceModel (in infrastructure/persistence/models/)
 * - AuthPersistenceMapper (in infrastructure/mapper/)
 *
 * This separation follows Clean Architecture's inward dependency principle:
 * Domain → no outer layer (framework) dependencies.
 *
 * Note: This domain entity references UserAccount by ID only, not by JPA relationship.
 * The persistence model handles the @ManyToOne relationship.
 */
public class PasswordResetChallenge {

    private UUID id;

    private String challengeToken;

    private UUID userId;

    private String otpHash;

    private OffsetDateTime expiresAt;

    private OffsetDateTime verifiedAt;

    private OffsetDateTime consumedAt;

    private OffsetDateTime createdAt;

    void onCreate() {
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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
