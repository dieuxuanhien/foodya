package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.infrastructure.persistence.models.OrderPersistenceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Raw JPA repository for OrderPersistenceModel.
 * 
 * This interface is INTERNAL to the infrastructure layer and should NEVER
 * be directly accessed from application or domain layers. Always use the
 * adapter (OrderManagementAdapter) which converts to/from domain entities.
 * 
 * See: OrderManagementAdapter for proper cleanup of domain entity persistence.
 */
public interface OrderManagementRepository extends JpaRepository<OrderPersistenceModel, UUID> {

    Page<OrderPersistenceModel> findByCustomerUserIdOrderByPlacedAtDesc(UUID customerUserId, Pageable pageable);

    Page<OrderPersistenceModel> findByRestaurantIdOrderByPlacedAtDesc(UUID restaurantId, Pageable pageable);

    List<OrderPersistenceModel> findByStatusInOrderByPlacedAtAsc(Collection<OrderStatus> statuses);
}
