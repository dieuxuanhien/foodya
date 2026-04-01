package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.SystemParameterPort;
import com.foodya.backend.domain.entities.SystemParameter;
import com.foodya.backend.infrastructure.repository.SystemParameterRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class SystemParameterAdapter implements SystemParameterPort {

    private final SystemParameterRepository repository;

    public SystemParameterAdapter(SystemParameterRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SystemParameter> findById(String key) {
        return repository.findById(Objects.requireNonNull(key));
    }

    @Override
    public boolean existsById(String key) {
        return repository.existsById(Objects.requireNonNull(key));
    }

    @Override
    public SystemParameter save(SystemParameter parameter) {
        return repository.save(Objects.requireNonNull(parameter));
    }

    @Override
    public List<SystemParameter> findAllOrderedByKey() {
        return repository.findAllByOrderByKeyAsc();
    }
}
