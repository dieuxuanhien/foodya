package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.RefreshToken;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.persistence.models.RefreshTokenPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenMapper {

    public RefreshToken toDomain(RefreshTokenPersistenceModel model, UserAccount user) {
        if (model == null) {
            return null;
        }

        RefreshToken domain = new RefreshToken();
        domain.setId(model.getId());
        domain.setUser(user);
        domain.setTokenJti(model.getTokenJti());
        domain.setTokenFamily(model.getTokenFamily());
        domain.setExpiresAt(model.getExpiresAt());
        domain.setRevokedAt(model.getRevokedAt());
        domain.setCreatedAt(model.getCreatedAt());
        domain.setReplacedByJti(model.getReplacedByJti());
        return domain;
    }

    public RefreshTokenPersistenceModel toPersistence(RefreshToken domain) {
        if (domain == null) {
            return null;
        }

        RefreshTokenPersistenceModel model = new RefreshTokenPersistenceModel();
        model.setId(domain.getId());
        model.setUserId(domain.getUser() == null ? null : domain.getUser().getId());
        model.setTokenJti(domain.getTokenJti());
        model.setTokenFamily(domain.getTokenFamily());
        model.setExpiresAt(domain.getExpiresAt());
        model.setRevokedAt(domain.getRevokedAt());
        model.setCreatedAt(domain.getCreatedAt());
        model.setReplacedByJti(domain.getReplacedByJti());
        return model;
    }
}