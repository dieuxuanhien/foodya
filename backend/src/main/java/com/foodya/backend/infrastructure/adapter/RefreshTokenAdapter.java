package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.RefreshTokenModel;
import com.foodya.backend.application.dto.UserAccountModel;
import com.foodya.backend.application.ports.out.RefreshTokenPort;
import com.foodya.backend.infrastructure.mapper.AuthPersistenceMapper;
import com.foodya.backend.domain.entities.RefreshToken;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.repository.RefreshTokenRepository;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class RefreshTokenAdapter implements RefreshTokenPort {

    private final RefreshTokenRepository repository;
    private final UserAccountRepository userAccountRepository;

    public RefreshTokenAdapter(RefreshTokenRepository repository,
                                          UserAccountRepository userAccountRepository) {
        this.repository = repository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public Optional<RefreshTokenModel> findByTokenJti(String tokenJti) {
        return repository.findByTokenJti(Objects.requireNonNull(tokenJti)).map(AuthPersistenceMapper::toModel);
    }

    @Override
    public List<RefreshTokenModel> findByTokenFamily(String tokenFamily) {
        return repository.findByTokenFamily(Objects.requireNonNull(tokenFamily)).stream().map(AuthPersistenceMapper::toModel).toList();
    }

    @Override
    public List<RefreshTokenModel> findByUserAndRevokedAtIsNullAndExpiresAtAfter(UserAccountModel user, OffsetDateTime now) {
        UserAccountModel userModel = Objects.requireNonNull(user);
        return repository.findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(Objects.requireNonNull(userModel.getId()), Objects.requireNonNull(now))
                .stream()
                .map(AuthPersistenceMapper::toModel)
                .toList();
    }

    @Override
    public RefreshTokenModel save(RefreshTokenModel refreshToken) {
        RefreshTokenModel tokenModel = Objects.requireNonNull(refreshToken);
        UserAccount userEntity = loadUser(tokenModel.getUser());
        RefreshToken entity = tokenModel.getId() == null
                ? new RefreshToken()
            : repository.findById(Objects.requireNonNull(tokenModel.getId())).orElseGet(RefreshToken::new);
        AuthPersistenceMapper.copyToEntity(tokenModel, entity, userEntity);
        return AuthPersistenceMapper.toModel(repository.save(Objects.requireNonNull(entity)));
    }

    @Override
    public List<RefreshTokenModel> saveAll(List<RefreshTokenModel> refreshTokens) {
        return refreshTokens.stream().map(this::save).toList();
    }

    private UserAccount loadUser(UserAccountModel user) {
        UserAccountModel userModel = Objects.requireNonNull(user);
        return userAccountRepository.findById(Objects.requireNonNull(userModel.getId()))
                .orElseThrow(() -> new IllegalArgumentException("user not found for refresh token persistence"));
    }
}
