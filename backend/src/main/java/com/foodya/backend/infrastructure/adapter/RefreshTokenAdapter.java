package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.RefreshTokenModel;
import com.foodya.backend.application.dto.UserAccountModel;
import com.foodya.backend.application.ports.out.RefreshTokenPort;
import com.foodya.backend.infrastructure.mapper.AuthPersistenceMapper;
import com.foodya.backend.domain.entities.RefreshToken;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.mapper.RefreshTokenMapper;
import com.foodya.backend.infrastructure.mapper.UserAccountMapper;
import com.foodya.backend.infrastructure.repository.RefreshTokenRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenAdapter implements RefreshTokenPort {

    private final RefreshTokenRepository repository;
    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserAccountMapper userAccountMapper;

    public RefreshTokenAdapter(RefreshTokenRepository repository,
                               UserAccountRepository userAccountRepository,
                               RefreshTokenMapper refreshTokenMapper,
                               UserAccountMapper userAccountMapper) {
        this.repository = repository;
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenMapper = refreshTokenMapper;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public Optional<RefreshTokenModel> findByTokenJti(String tokenJti) {
        return repository.findByTokenJti(Objects.requireNonNull(tokenJti))
                .map(model -> AuthPersistenceMapper.toModel(refreshTokenMapper.toDomain(model, loadUserDomain(model.getUserId()))));
    }

    @Override
    public List<RefreshTokenModel> findByTokenFamily(String tokenFamily) {
        return repository.findByTokenFamily(Objects.requireNonNull(tokenFamily)).stream()
                .map(model -> AuthPersistenceMapper.toModel(refreshTokenMapper.toDomain(model, loadUserDomain(model.getUserId()))))
                .toList();
    }

    @Override
    public List<RefreshTokenModel> findByUserAndRevokedAtIsNullAndExpiresAtAfter(UserAccountModel user, OffsetDateTime now) {
        UserAccountModel userModel = Objects.requireNonNull(user);
        UserAccount userEntity = loadUserDomain(Objects.requireNonNull(userModel.getId()));
        return repository.findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(userModel.getId(), Objects.requireNonNull(now))
            .stream()
            .map(model -> AuthPersistenceMapper.toModel(refreshTokenMapper.toDomain(model, userEntity)))
            .toList();
    }

    @Override
    public RefreshTokenModel save(RefreshTokenModel refreshToken) {
        RefreshTokenModel tokenModel = Objects.requireNonNull(refreshToken);
        UserAccount userEntity = loadUserDomain(tokenModel.getUser().getId());
        RefreshToken entity = tokenModel.getId() == null
                ? new RefreshToken()
            : repository.findById(Objects.requireNonNull(tokenModel.getId()))
                .map(model -> refreshTokenMapper.toDomain(model, loadUserDomain(model.getUserId())))
                .orElseGet(() -> {
                    RefreshToken newEntity = new RefreshToken();
                    newEntity.setId(tokenModel.getId());
                    return newEntity;
                });
        AuthPersistenceMapper.copyToEntity(tokenModel, entity, userEntity);
        entity.setId(tokenModel.getId());
        return AuthPersistenceMapper.toModel(refreshTokenMapper.toDomain(repository.save(Objects.requireNonNull(refreshTokenMapper.toPersistence(entity))), userEntity));
    }

    @Override
    public List<RefreshTokenModel> saveAll(List<RefreshTokenModel> refreshTokens) {
        return refreshTokens.stream().map(this::save).toList();
    }

    private UserAccount loadUserDomain(UUID userId) {
        return userAccountRepository.findById(Objects.requireNonNull(userId))
                .map(userAccountMapper::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("user not found for refresh token persistence"));
    }
}
