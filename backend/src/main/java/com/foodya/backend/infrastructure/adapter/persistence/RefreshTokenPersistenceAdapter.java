package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.RefreshTokenPort;
import com.foodya.backend.domain.persistence.RefreshToken;
import com.foodya.backend.domain.persistence.UserAccount;
import com.foodya.backend.infrastructure.repository.RefreshTokenRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class RefreshTokenPersistenceAdapter implements RefreshTokenPort {

    private final RefreshTokenRepository repository;

    public RefreshTokenPersistenceAdapter(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RefreshToken> findByTokenJti(String tokenJti) {
        return repository.findByTokenJti(tokenJti);
    }

    @Override
    public List<RefreshToken> findByTokenFamily(String tokenFamily) {
        return repository.findByTokenFamily(tokenFamily);
    }

    @Override
    public List<RefreshToken> findByUserAndRevokedAtIsNullAndExpiresAtAfter(UserAccount user, OffsetDateTime now) {
        return repository.findByUserAndRevokedAtIsNullAndExpiresAtAfter(user, now);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return repository.save(refreshToken);
    }

    @Override
    public List<RefreshToken> saveAll(List<RefreshToken> refreshTokens) {
        return repository.saveAll(refreshTokens);
    }
}
