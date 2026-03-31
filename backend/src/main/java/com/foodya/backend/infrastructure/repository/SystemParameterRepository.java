package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.persistence.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemParameterRepository extends JpaRepository<SystemParameter, String> {

	List<SystemParameter> findAllByOrderByKeyAsc();
}
