package com.foodya.backend.application.port.out;

import com.foodya.backend.domain.persistence.PasswordResetChallenge;

import java.util.Optional;

public interface PasswordResetChallengePort {

    Optional<PasswordResetChallenge> findByChallengeToken(String challengeToken);

    PasswordResetChallenge save(PasswordResetChallenge challenge);
}
