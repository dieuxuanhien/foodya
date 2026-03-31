package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.dto.PasswordResetChallengeModel;
import com.foodya.backend.application.ports.out.PasswordResetChallengePort;
import com.foodya.backend.infrastructure.adapter.mapper.AuthPersistenceMapper;
import com.foodya.backend.domain.entities.PasswordResetChallenge;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.repository.PasswordResetChallengeRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class PasswordResetChallengePersistenceAdapter implements PasswordResetChallengePort {

    private final PasswordResetChallengeRepository repository;
    private final UserAccountRepository userAccountRepository;

    public PasswordResetChallengePersistenceAdapter(PasswordResetChallengeRepository repository,
                                                    UserAccountRepository userAccountRepository) {
        this.repository = repository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public Optional<PasswordResetChallengeModel> findByChallengeToken(String challengeToken) {
        return repository.findByChallengeToken(Objects.requireNonNull(challengeToken))
            .map(AuthPersistenceMapper::toModel);
    }

    @Override
    public PasswordResetChallengeModel save(PasswordResetChallengeModel challenge) {
        PasswordResetChallengeModel challengeModel = Objects.requireNonNull(challenge);
        UserAccount userEntity = userAccountRepository.findById(Objects.requireNonNull(challengeModel.getUser().getId()))
            .orElseThrow(() -> new IllegalArgumentException("user not found for password reset challenge persistence"));
        PasswordResetChallenge entity = challengeModel.getId() == null
                ? new PasswordResetChallenge()
            : repository.findById(Objects.requireNonNull(challengeModel.getId())).orElseGet(PasswordResetChallenge::new);
        AuthPersistenceMapper.copyToEntity(challengeModel, entity, userEntity);
        return AuthPersistenceMapper.toModel(repository.save(Objects.requireNonNull(entity)));
    }
}
