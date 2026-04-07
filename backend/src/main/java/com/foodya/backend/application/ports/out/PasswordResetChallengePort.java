package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.PasswordResetChallengeData;

import java.util.Optional;

public interface PasswordResetChallengePort {

    Optional<PasswordResetChallengeData> findByChallengeToken(String challengeToken);

    PasswordResetChallengeData save(PasswordResetChallengeData challenge);
}
