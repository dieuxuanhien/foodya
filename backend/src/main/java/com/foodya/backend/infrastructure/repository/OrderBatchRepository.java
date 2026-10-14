package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.OrderBatchPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderBatchRepository extends JpaRepository<OrderBatchPersistenceModel, UUID> {

    @Query("SELECT b FROM OrderBatchPersistenceModel b WHERE b.driverUserId = :driverUserId AND b.status IN ('PENDING', 'ACTIVE') ORDER BY b.createdAt DESC")
    Optional<OrderBatchPersistenceModel> findActiveBatchForDriver(UUID driverUserId);

    List<OrderBatchPersistenceModel> findAllByOrderByCreatedAtDesc();
}
