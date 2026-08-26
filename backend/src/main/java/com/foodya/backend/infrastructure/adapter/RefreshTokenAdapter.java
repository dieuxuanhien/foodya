package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.RefreshTokenPort;
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
    public Optional<RefreshToken> findByTokenJti(String tokenJti) {
        return repository.findByTokenJti(Objects.requireNonNull(tokenJti))
                .map(model -> refreshTokenMapper.toDomain(model, loadUserDomain(model.getUserId())));
    }

    @Override
    public List<RefreshToken> findByTokenFamily(String tokenFamily) {
        return repository.findByTokenFamily(Objects.requireNonNull(tokenFamily)).stream()
                .map(model -> refreshTokenMapper.toDomain(model, loadUserDomain(model.getUserId())))
                .toList();
    }

    @Override
    public List<RefreshToken> findByUserAndRevokedAtIsNullAndExpiresAtAfter(UserAccount user, OffsetDateTime now) {
        UserAccount userDomain = Objects.requireNonNull(user);
        return repository.findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(userDomain.getId(), Objects.requireNonNull(now))
            .stream()
            .map(model -> refreshTokenMapper.toDomain(model, userDomain))
            .toList();
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshToken tokenDomain = Objects.requireNonNull(refreshToken);
        UserAccount userDomain = tokenDomain.getUser() != null
                ? tokenDomain.getUser()
                : (tokenDomain.getId() != null ? repository.findById(tokenDomain.getId()).map(m -> loadUserDomain(m.getUserId())).orElse(null) : null);
        return refreshTokenMapper.toDomain(repository.save(Objects.requireNonNull(refreshTokenMapper.toPersistence(tokenDomain))), userDomain);
    }

    @Override
    public List<RefreshToken> saveAll(List<RefreshToken> refreshTokens) {
        return refreshTokens.stream().map(this::save).toList();
    }

    private UserAccount loadUserDomain(UUID userId) {
        return userAccountRepository.findById(Objects.requireNonNull(userId))
                .map(userAccountMapper::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("user not found for refresh token persistence"));
    }
}
