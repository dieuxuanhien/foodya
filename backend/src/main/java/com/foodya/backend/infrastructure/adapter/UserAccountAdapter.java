package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.dto.UserAccountModel;
import com.foodya.backend.application.ports.out.UserAccountPort;
import com.foodya.backend.infrastructure.mapper.AuthPersistenceMapper;
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
    public Optional<UserAccountModel> findById(UUID id) {
        return repository.findById(Objects.requireNonNull(id)).map(mapper::toDomain).map(AuthPersistenceMapper::toModel);
    }

    @Override
    public Optional<UserAccountModel> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain).map(AuthPersistenceMapper::toModel);
    }

    @Override
    public Optional<UserAccountModel> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain).map(AuthPersistenceMapper::toModel);
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
    public UserAccountModel save(UserAccountModel userAccount) {
        UserAccountModel accountModel = Objects.requireNonNull(userAccount);
        UserAccount entity = accountModel.getId() == null
                ? new UserAccount()
            : repository.findById(Objects.requireNonNull(accountModel.getId()))
                .map(mapper::toDomain)
                .orElseGet(() -> {
                    UserAccount newEntity = new UserAccount();
                    newEntity.setId(accountModel.getId());
                    return newEntity;
                });
        AuthPersistenceMapper.copyToEntity(accountModel, entity);
        entity.setId(accountModel.getId());
        return AuthPersistenceMapper.toModel(mapper.toDomain(repository.save(Objects.requireNonNull(mapper.toPersistence(entity)))));
    }
}
