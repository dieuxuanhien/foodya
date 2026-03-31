package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.SystemParameter;

import java.util.List;
import java.util.Optional;

public interface SystemParameterPort {

    Optional<SystemParameter> findById(String key);

    boolean existsById(String key);

    SystemParameter save(SystemParameter parameter);

    List<SystemParameter> findAllOrderedByKey();
}
