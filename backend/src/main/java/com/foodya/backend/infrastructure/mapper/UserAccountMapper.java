package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class UserAccountMapper {

    public UserAccount toDomain(UserAccountPersistenceModel model) {
        if (model == null) {
            return null;
        }

        UserAccount domain = new UserAccount();
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

    public UserAccountPersistenceModel toPersistence(UserAccount domain) {
        if (domain == null) {
            return null;
        }

        UserAccountPersistenceModel model = new UserAccountPersistenceModel();
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