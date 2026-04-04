package com.foodya.backend.domain.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DOMAIN ENTITY: RefreshToken
 *
 * This is a PURE domain entity with NO framework dependencies or annotations.
 * - All business logic and state transitions are here
 * - No persistence concerns (JPA, database mapping)
 * - Fully unit-testable without any framework
 * - Framework-independent and reusable across technologies
 *
 * Persistence mapping is handled by:
 * - RefreshTokenPersistenceModel (in infrastructure/persistence/models/)
 * - AuthPersistenceMapper (in infrastructure/mapper/)
 *
 * This separation follows Clean Architecture's inward dependency principle:
 * Domain → no outer layer (framework) dependencies.
 *
 * Note: This domain entity references UserAccount by ID only, not by JPA relationship.
 * The persistence model handles the @ManyToOne relationship.
 */
public class RefreshToken {

    private UUID id;

    private UUID userId;

    private String tokenJti;

    private String tokenFamily;

    private OffsetDateTime expiresAt;

    private OffsetDateTime revokedAt;

    private OffsetDateTime createdAt;

    private String replacedByJti;

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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getReplacedByJti() {
        return replacedByJti;
    }

    public void setReplacedByJti(String replacedByJti) {
        this.replacedByJti = replacedByJti;
    }
}
