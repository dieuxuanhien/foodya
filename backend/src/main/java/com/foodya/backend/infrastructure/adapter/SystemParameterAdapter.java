package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.domain.entities.SystemParameter;
import com.foodya.backend.infrastructure.mapper.SystemParameterMapper;
import com.foodya.backend.infrastructure.repository.SystemParameterRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class SystemParameterAdapter implements SystemParameterPort {

    private final SystemParameterRepository repository;
    private final SystemParameterMapper mapper;

    public SystemParameterAdapter(SystemParameterRepository repository, SystemParameterMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<SystemParameter> findById(String key) {
        return repository.findById(Objects.requireNonNull(key))
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String key) {
        return repository.existsById(Objects.requireNonNull(key));
    }

    @Override
    public SystemParameter save(SystemParameter parameter) {
        var model = mapper.toPersistence(Objects.requireNonNull(parameter));
        var saved = repository.save(model);
        return mapper.toDomain(saved);
    }

    @Override
    public List<SystemParameter> findAllOrderedByKey() {
        return repository.findAllByOrderByKeyAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
