package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.UserAccountPort;
import com.foodya.backend.domain.entities.UserAccount;
import com.foodya.backend.infrastructure.mapper.UserAccountMapper;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserAccountAdapter implements UserAccountPort {

    private final UserAccountRepository repository;
    private final UserAccountMapper mapper;

    public UserAccountAdapter(UserAccountRepository repository, UserAccountMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return repository.findById(Objects.requireNonNull(id)).map(mapper::toDomain);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return repository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, UUID id) {
        return repository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id) {
        return repository.existsByPhoneNumberAndIdNot(phoneNumber, id);
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        Objects.requireNonNull(userAccount);
        return mapper.toDomain(repository.save(Objects.requireNonNull(mapper.toPersistence(userAccount))));
    }
}
