package com.foodya.backend.infrastructure.adapter.mapper;

import com.foodya.backend.application.dto.PasswordResetChallengeModel;
import com.foodya.backend.application.dto.RefreshTokenModel;
import com.foodya.backend.application.dto.UserAccountModel;
import com.foodya.backend.domain.entities.PasswordResetChallenge;
import com.foodya.backend.domain.entities.RefreshToken;
import com.foodya.backend.domain.entities.UserAccount;

public final class AuthPersistenceMapper {

    private AuthPersistenceMapper() {
    }

    public static UserAccountModel toModel(UserAccount entity) {
        if (entity == null) {
            return null;
        }
        UserAccountModel model = new UserAccountModel();
        model.setId(entity.getId());
        model.setUsername(entity.getUsername());
        model.setEmail(entity.getEmail());
        model.setPhoneNumber(entity.getPhoneNumber());
        model.setFullName(entity.getFullName());
        model.setAvatarUrl(entity.getAvatarUrl());
        model.setRole(entity.getRole());
        model.setStatus(entity.getStatus());
        model.setPasswordHash(entity.getPasswordHash());
        return model;
    }

    public static UserAccount toEntity(UserAccountModel model) {
        if (model == null) {
            return null;
        }
        UserAccount entity = new UserAccount();
        copyToEntity(model, entity);
        return entity;
    }

    public static void copyToEntity(UserAccountModel model, UserAccount entity) {
        if (model == null || entity == null) {
            return;
        }
        entity.setUsername(model.getUsername());
        entity.setEmail(model.getEmail());
        entity.setPhoneNumber(model.getPhoneNumber());
        entity.setFullName(model.getFullName());
        entity.setAvatarUrl(model.getAvatarUrl());
        entity.setRole(model.getRole());
        entity.setStatus(model.getStatus());
        entity.setPasswordHash(model.getPasswordHash());
    }

    public static RefreshTokenModel toModel(RefreshToken entity) {
        if (entity == null) {
            return null;
        }
        RefreshTokenModel model = new RefreshTokenModel();
        model.setId(entity.getId());
        model.setUser(toModel(entity.getUser()));
        model.setTokenJti(entity.getTokenJti());
        model.setTokenFamily(entity.getTokenFamily());
        model.setExpiresAt(entity.getExpiresAt());
        model.setRevokedAt(entity.getRevokedAt());
        model.setReplacedByJti(entity.getReplacedByJti());
        return model;
    }

    public static RefreshToken toEntity(RefreshTokenModel model, UserAccount userEntity) {
        if (model == null) {
            return null;
        }
        RefreshToken entity = new RefreshToken();
        entity.setUser(userEntity);
        entity.setTokenJti(model.getTokenJti());
        entity.setTokenFamily(model.getTokenFamily());
        entity.setExpiresAt(model.getExpiresAt());
        entity.setRevokedAt(model.getRevokedAt());
        entity.setReplacedByJti(model.getReplacedByJti());
        return entity;
    }

    public static void copyToEntity(RefreshTokenModel model, RefreshToken entity, UserAccount userEntity) {
        if (model == null || entity == null) {
            return;
        }
        entity.setUser(userEntity);
        entity.setTokenJti(model.getTokenJti());
        entity.setTokenFamily(model.getTokenFamily());
        entity.setExpiresAt(model.getExpiresAt());
        entity.setRevokedAt(model.getRevokedAt());
        entity.setReplacedByJti(model.getReplacedByJti());
    }

    public static PasswordResetChallengeModel toModel(PasswordResetChallenge entity) {
        if (entity == null) {
            return null;
        }
        PasswordResetChallengeModel model = new PasswordResetChallengeModel();
        model.setId(entity.getId());
        model.setChallengeToken(entity.getChallengeToken());
        model.setUser(toModel(entity.getUser()));
        model.setOtpHash(entity.getOtpHash());
        model.setExpiresAt(entity.getExpiresAt());
        model.setVerifiedAt(entity.getVerifiedAt());
        model.setConsumedAt(entity.getConsumedAt());
        return model;
    }

    public static PasswordResetChallenge toEntity(PasswordResetChallengeModel model, UserAccount userEntity) {
        if (model == null) {
            return null;
        }
        PasswordResetChallenge entity = new PasswordResetChallenge();
        entity.setChallengeToken(model.getChallengeToken());
        entity.setUser(userEntity);
        entity.setOtpHash(model.getOtpHash());
        entity.setExpiresAt(model.getExpiresAt());
        entity.setVerifiedAt(model.getVerifiedAt());
        entity.setConsumedAt(model.getConsumedAt());
        return entity;
    }

    public static void copyToEntity(PasswordResetChallengeModel model, PasswordResetChallenge entity, UserAccount userEntity) {
        if (model == null || entity == null) {
            return;
        }
        entity.setChallengeToken(model.getChallengeToken());
        entity.setUser(userEntity);
        entity.setOtpHash(model.getOtpHash());
        entity.setExpiresAt(model.getExpiresAt());
        entity.setVerifiedAt(model.getVerifiedAt());
        entity.setConsumedAt(model.getConsumedAt());
    }
}