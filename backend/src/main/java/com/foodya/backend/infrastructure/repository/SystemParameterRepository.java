package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.SystemParameterPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemParameterRepository extends JpaRepository<SystemParameterPersistenceModel, String> {

    List<SystemParameterPersistenceModel> findAllByOrderByKeyAsc();
}
