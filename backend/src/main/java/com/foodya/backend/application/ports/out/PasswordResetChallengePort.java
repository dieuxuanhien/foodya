package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.PasswordResetChallenge;

import java.util.Optional;

public interface PasswordResetChallengePort {

    Optional<PasswordResetChallenge> findByChallengeToken(String challengeToken);

    PasswordResetChallenge save(PasswordResetChallenge challenge);
}
