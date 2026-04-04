package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.PasswordResetChallengeModel;
import com.foodya.backend.application.ports.out.PasswordResetChallengePort;
import com.foodya.backend.infrastructure.mapper.AuthPersistenceMapper;
import com.foodya.backend.domain.entities.PasswordResetChallenge;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.mapper.PasswordResetChallengeMapper;
import com.foodya.backend.infrastructure.mapper.UserAccountMapper;
import com.foodya.backend.infrastructure.repository.PasswordResetChallengeRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class PasswordResetChallengeAdapter implements PasswordResetChallengePort {

    private final PasswordResetChallengeRepository repository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordResetChallengeMapper passwordResetChallengeMapper;
    private final UserAccountMapper userAccountMapper;

    public PasswordResetChallengeAdapter(PasswordResetChallengeRepository repository,
                                         UserAccountRepository userAccountRepository,
                                         PasswordResetChallengeMapper passwordResetChallengeMapper,
                                         UserAccountMapper userAccountMapper) {
        this.repository = repository;
        this.userAccountRepository = userAccountRepository;
        this.passwordResetChallengeMapper = passwordResetChallengeMapper;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public Optional<PasswordResetChallengeModel> findByChallengeToken(String challengeToken) {
        return repository.findByChallengeToken(Objects.requireNonNull(challengeToken))
            .map(model -> AuthPersistenceMapper.toModel(passwordResetChallengeMapper.toDomain(model, loadUserDomain(model.getUserId()))));
    }

    @Override
    public PasswordResetChallengeModel save(PasswordResetChallengeModel challenge) {
        PasswordResetChallengeModel challengeModel = Objects.requireNonNull(challenge);
        UserAccount userEntity = loadUserDomain(Objects.requireNonNull(challengeModel.getUser().getId()));
        PasswordResetChallenge entity = challengeModel.getId() == null
                ? new PasswordResetChallenge()
            : repository.findById(Objects.requireNonNull(challengeModel.getId()))
                .map(model -> passwordResetChallengeMapper.toDomain(model, loadUserDomain(model.getUserId())))
                .orElseGet(() -> {
                    PasswordResetChallenge newEntity = new PasswordResetChallenge();
                    newEntity.setId(challengeModel.getId());
                    return newEntity;
                });
        AuthPersistenceMapper.copyToEntity(challengeModel, entity, userEntity);
        entity.setId(challengeModel.getId());
        return AuthPersistenceMapper.toModel(passwordResetChallengeMapper.toDomain(repository.save(Objects.requireNonNull(passwordResetChallengeMapper.toPersistence(entity))), userEntity));
    }

    private UserAccount loadUserDomain(UUID userId) {
        return userAccountRepository.findById(Objects.requireNonNull(userId))
            .map(userAccountMapper::toDomain)
            .orElseThrow(() -> new IllegalArgumentException("user not found for password reset challenge persistence"));
    }
}
