package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.BatchOrderItemPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchOrderItemRepository extends JpaRepository<BatchOrderItemPersistenceModel, UUID> {

    List<BatchOrderItemPersistenceModel> findByBatchIdOrderByPickupIndexAsc(UUID batchId);

    Optional<BatchOrderItemPersistenceModel> findByBatchIdAndOrderId(UUID batchId, UUID orderId);
}
