package com.foodya.backend.application.ports.out;

import com.foodya.backend.application.dto.PasswordResetChallengeModel;

import java.util.Optional;

public interface PasswordResetChallengePort {

    Optional<PasswordResetChallengeModel> findByChallengeToken(String challengeToken);

    PasswordResetChallengeModel save(PasswordResetChallengeModel challenge);
}
