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
 * PERSISTENCE MODEL — JPA-annotated version of RefreshToken domain entity.
 *
 * This class handles ONLY database mapping concerns. All business logic
 * stays in the domain entity (RefreshToken). Mappers convert between this model
 * and the domain entity at adapter boundaries.
 *
 * REASON: Clean Architecture principle — domain layer must not depend on
 * persistence framework. This separation makes the domain reusable across
 * technologies and testable independently.
 *
 * Invariant: Always convert to/from RefreshToken domain entity at infrastructure
 * boundaries via AuthPersistenceMapper.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenPersistenceModel {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountPersistenceModel user;

    @Column(name = "token_jti", nullable = false, length = 128, unique = true)
    private String tokenJti;

    @Column(name = "token_family", nullable = false, length = 128)
    private String tokenFamily;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "replaced_by_jti", length = 128)
    private String replacedByJti;

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

    public UserAccountPersistenceModel getUser() {
        return user;
    }

    public void setUser(UserAccountPersistenceModel user) {
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
