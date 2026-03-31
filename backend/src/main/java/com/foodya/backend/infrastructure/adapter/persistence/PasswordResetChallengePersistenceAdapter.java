package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.PasswordResetChallengePort;
import com.foodya.backend.domain.persistence.PasswordResetChallenge;
import com.foodya.backend.infrastructure.repository.PasswordResetChallengeRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PasswordResetChallengePersistenceAdapter implements PasswordResetChallengePort {

    private final PasswordResetChallengeRepository repository;

    public PasswordResetChallengePersistenceAdapter(PasswordResetChallengeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PasswordResetChallenge> findByChallengeToken(String challengeToken) {
        return repository.findByChallengeToken(challengeToken);
    }

    @Override
    public PasswordResetChallenge save(PasswordResetChallenge challenge) {
        return repository.save(challenge);
    }
}
