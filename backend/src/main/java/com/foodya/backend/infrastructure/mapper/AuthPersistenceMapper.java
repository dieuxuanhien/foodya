package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.application.dto.PasswordResetChallengeData;
import com.foodya.backend.application.dto.RefreshTokenData;
import com.foodya.backend.application.dto.UserAccountData;
import com.foodya.backend.domain.entities.PasswordResetChallenge;
import com.foodya.backend.domain.entities.RefreshToken;
import com.foodya.backend.domain.entities.UserAccount;

public final class AuthPersistenceMapper {

    private AuthPersistenceMapper() {
    }

    public static UserAccountData toData(UserAccount entity) {
        if (entity == null) {
            return null;
        }
        UserAccountData data = new UserAccountData();
        data.setId(entity.getId());
        data.setUsername(entity.getUsername());
        data.setEmail(entity.getEmail());
        data.setPhoneNumber(entity.getPhoneNumber());
        data.setFullName(entity.getFullName());
        data.setAvatarUrl(entity.getAvatarUrl());
        data.setRole(entity.getRole());
        data.setStatus(entity.getStatus());
        data.setPasswordHash(entity.getPasswordHash());
        return data;
    }

    public static UserAccount toEntity(UserAccountData model) {
        if (model == null) {
            return null;
        }
        UserAccount entity = new UserAccount();
        copyToEntity(model, entity);
        return entity;
    }

    public static void copyToEntity(UserAccountData model, UserAccount entity) {
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

    public static RefreshTokenData toData(RefreshToken entity) {
        if (entity == null) {
            return null;
        }
        RefreshTokenData data = new RefreshTokenData();
        data.setId(entity.getId());
        data.setUser(toData(entity.getUser()));
        data.setTokenJti(entity.getTokenJti());
        data.setTokenFamily(entity.getTokenFamily());
        data.setExpiresAt(entity.getExpiresAt());
        data.setRevokedAt(entity.getRevokedAt());
        data.setReplacedByJti(entity.getReplacedByJti());
        return data;
    }

    public static RefreshToken toEntity(RefreshTokenData model, UserAccount userEntity) {
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

    public static void copyToEntity(RefreshTokenData model, RefreshToken entity, UserAccount userEntity) {
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

    public static PasswordResetChallengeData toData(PasswordResetChallenge entity) {
        if (entity == null) {
            return null;
        }
        PasswordResetChallengeData data = new PasswordResetChallengeData();
        data.setId(entity.getId());
        data.setChallengeToken(entity.getChallengeToken());
        data.setUser(toData(entity.getUser()));
        data.setOtpHash(entity.getOtpHash());
        data.setExpiresAt(entity.getExpiresAt());
        data.setVerifiedAt(entity.getVerifiedAt());
        data.setConsumedAt(entity.getConsumedAt());
        return data;
    }

    public static PasswordResetChallenge toEntity(PasswordResetChallengeData model, UserAccount userEntity) {
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

    public static void copyToEntity(PasswordResetChallengeData model, PasswordResetChallenge entity, UserAccount userEntity) {
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