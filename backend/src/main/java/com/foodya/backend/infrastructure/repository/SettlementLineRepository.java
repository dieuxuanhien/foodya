package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.SettlementLinePersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementLineRepository extends JpaRepository<SettlementLinePersistenceModel, UUID> {

    List<SettlementLinePersistenceModel> findBySettlementIdOrderByOrderCodeAsc(UUID settlementId);
}