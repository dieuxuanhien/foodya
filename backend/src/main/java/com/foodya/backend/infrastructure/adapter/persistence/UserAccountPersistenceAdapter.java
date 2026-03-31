package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.UserAccountPort;
import com.foodya.backend.domain.persistence.UserAccount;
import com.foodya.backend.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserAccountPersistenceAdapter implements UserAccountPort {

    private final UserAccountRepository repository;

    public UserAccountPersistenceAdapter(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return repository.findByEmail(email);
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
        return repository.save(userAccount);
    }
}
