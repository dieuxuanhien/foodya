package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.DeliveryRoutePersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoutePersistenceModel, UUID> {

    Optional<DeliveryRoutePersistenceModel> findFirstByOrderIdAndActiveTrueOrderByCalculatedAtDesc(UUID orderId);

    @Modifying
    @Query("UPDATE DeliveryRoutePersistenceModel r SET r.active = false WHERE r.orderId = :orderId AND r.active = true")
    void deactivateByOrderId(UUID orderId);
}
