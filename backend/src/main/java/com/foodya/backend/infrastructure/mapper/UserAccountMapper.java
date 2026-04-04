package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel;
import org.springframework.stereotype.Component;

/**
 * MAPPER: UserAccount Domain Entity ↔ UserAccountPersistenceModel
 *
 * Bidirectional mapping between domain business logic layer and persistence layer.
 * This mapper ensures proper separation of concerns per Clean Architecture:
 *
 * - Domain layer (UserAccount.java) stays framework-independent and test-friendly
 * - Persistence layer (UserAccountPersistenceModel.java) handles JPA/database concerns
 * - Mapping happens ONLY at infrastructure boundaries (adapters)
 *
 * Usage:
 *   // Load from DB: persistence → domain
 *   UserAccountPersistenceModel persisted = repository.findById(id);
 *   UserAccount domainUser = mapper.toDomain(persisted);
 *
 *   // Save to DB: domain → persistence
 *   UserAccount domainUser = userService.process(...);
 *   UserAccountPersistenceModel persisted = mapper.toPersistence(domainUser);
 *   repository.save(persisted);
 */
@Component
public class UserAccountMapper {

    /**
     * Convert persistence model to domain entity.
     * Used when loading from database.
     */
    public UserAccount toDomain(UserAccountPersistenceModel model) {
        if (model == null) {
            return null;
        }

        UserAccount domain = new UserAccount();

        // All fields map 1:1 (names match between domain and persistence model)
        domain.setId(model.getId());
        domain.setUsername(model.getUsername());
        domain.setEmail(model.getEmail());
        domain.setPhoneNumber(model.getPhoneNumber());
        domain.setFullName(model.getFullName());
        domain.setAvatarUrl(model.getAvatarUrl());
        domain.setRole(model.getRole());
        domain.setStatus(model.getStatus());
        domain.setPasswordHash(model.getPasswordHash());
        domain.setCreatedAt(model.getCreatedAt());
        domain.setUpdatedAt(model.getUpdatedAt());

        return domain;
    }

    /**
     * Convert domain entity to persistence model.
     * Used when saving to database.
     */
    public UserAccountPersistenceModel toPersistence(UserAccount domain) {
        if (domain == null) {
            return null;
        }

        UserAccountPersistenceModel model = new UserAccountPersistenceModel();

        // All fields map 1:1 (names match between domain and persistence model)
        model.setId(domain.getId());
        model.setUsername(domain.getUsername());
        model.setEmail(domain.getEmail());
        model.setPhoneNumber(domain.getPhoneNumber());
        model.setFullName(domain.getFullName());
        model.setAvatarUrl(domain.getAvatarUrl());
        model.setRole(domain.getRole());
        model.setStatus(domain.getStatus());
        model.setPasswordHash(domain.getPasswordHash());
        model.setCreatedAt(domain.getCreatedAt());
        model.setUpdatedAt(domain.getUpdatedAt());

        return model;
    }
}
