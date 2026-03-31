package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.SystemParameterPort;
import com.foodya.backend.domain.persistence.SystemParameter;
import com.foodya.backend.infrastructure.repository.SystemParameterRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SystemParameterPersistenceAdapter implements SystemParameterPort {

    private final SystemParameterRepository repository;

    public SystemParameterPersistenceAdapter(SystemParameterRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SystemParameter> findById(String key) {
        return repository.findById(key);
    }

    @Override
    public boolean existsById(String key) {
        return repository.existsById(key);
    }

    @Override
    public SystemParameter save(SystemParameter parameter) {
        return repository.save(parameter);
    }

    @Override
    public List<SystemParameter> findAllOrderedByKey() {
        return repository.findAllByOrderByKeyAsc();
    }
}
