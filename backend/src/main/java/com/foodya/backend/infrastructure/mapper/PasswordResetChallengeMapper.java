package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.PasswordResetChallenge;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.persistence.models.PasswordResetChallengePersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetChallengeMapper {

    public PasswordResetChallenge toDomain(PasswordResetChallengePersistenceModel model, UserAccount user) {
        if (model == null) {
            return null;
        }

        PasswordResetChallenge domain = new PasswordResetChallenge();
        domain.setId(model.getId());
        domain.setChallengeToken(model.getChallengeToken());
        domain.setUser(user);
        domain.setOtpHash(model.getOtpHash());
        domain.setExpiresAt(model.getExpiresAt());
        domain.setVerifiedAt(model.getVerifiedAt());
        domain.setConsumedAt(model.getConsumedAt());
        domain.setCreatedAt(model.getCreatedAt());
        return domain;
    }

    public PasswordResetChallengePersistenceModel toPersistence(PasswordResetChallenge domain) {
        if (domain == null) {
            return null;
        }

        PasswordResetChallengePersistenceModel model = new PasswordResetChallengePersistenceModel();
        model.setId(domain.getId());
        model.setChallengeToken(domain.getChallengeToken());
        model.setUserId(domain.getUser() == null ? null : domain.getUser().getId());
        model.setOtpHash(domain.getOtpHash());
        model.setExpiresAt(domain.getExpiresAt());
        model.setVerifiedAt(domain.getVerifiedAt());
        model.setConsumedAt(domain.getConsumedAt());
        model.setCreatedAt(domain.getCreatedAt());
        return model;
    }
}