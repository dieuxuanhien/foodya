package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.value_objects.PayeeType;
import com.foodya.backend.infrastructure.persistence.models.SettlementPersistenceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SettlementRepository extends JpaRepository<SettlementPersistenceModel, UUID> {

    Page<SettlementPersistenceModel> findAllByOrderBySettledAtDesc(Pageable pageable);

    Page<SettlementPersistenceModel> findByPayeeTypeOrderBySettledAtDesc(PayeeType payeeType, Pageable pageable);

    Page<SettlementPersistenceModel> findByPayeeTypeAndPayeeUserIdOrderBySettledAtDesc(
            PayeeType payeeType, UUID payeeUserId, Pageable pageable);
}