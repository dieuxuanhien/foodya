package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.infrastructure.persistence.models.OrderPersistenceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

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

    @Query("SELECT o FROM OrderPersistenceModel o WHERE o.status = com.foodya.backend.domain.value_objects.OrderStatus.ACCEPTED AND o.driverUserId IS NULL AND o.batchId IS NULL ORDER BY o.placedAt ASC")
    List<OrderPersistenceModel> findEligibleForBatching();

    List<OrderPersistenceModel> findByStatusAndPlacedAtBeforeOrderByPlacedAtAsc(OrderStatus status,
                                                                               java.time.OffsetDateTime before);

    List<OrderPersistenceModel> findByDriverUserIdAndStatusInOrderByPlacedAtAsc(
            UUID driverUserId, Collection<OrderStatus> statuses);

    List<OrderPersistenceModel> findByDriverUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtAsc(
            UUID driverUserId, OrderStatus status, java.time.OffsetDateTime from, java.time.OffsetDateTime to);

    // ---- settlements: completed orders not yet covered by a settlement line ----

    @Query("SELECT o, r.ownerUserId FROM OrderPersistenceModel o, RestaurantPersistenceModel r "
            + "WHERE r.id = o.restaurantId "
            + "AND o.status = com.foodya.backend.domain.value_objects.OrderStatus.SUCCESS "
            + "AND o.paymentStatus <> com.foodya.backend.domain.value_objects.PaymentStatus.REFUNDED "
            + "AND NOT EXISTS (SELECT 1 FROM SettlementLinePersistenceModel l WHERE l.orderId = o.id "
            + "AND l.payeeType = com.foodya.backend.domain.value_objects.PayeeType.MERCHANT) "
            + "ORDER BY o.completedAt ASC, o.orderCode ASC")
    List<Object[]> findUnsettledMerchantOrders();

    @Query("SELECT o, r.ownerUserId FROM OrderPersistenceModel o, RestaurantPersistenceModel r "
            + "WHERE r.id = o.restaurantId AND r.ownerUserId = :payeeUserId "
            + "AND o.status = com.foodya.backend.domain.value_objects.OrderStatus.SUCCESS "
            + "AND o.paymentStatus <> com.foodya.backend.domain.value_objects.PaymentStatus.REFUNDED "
            + "AND NOT EXISTS (SELECT 1 FROM SettlementLinePersistenceModel l WHERE l.orderId = o.id "
            + "AND l.payeeType = com.foodya.backend.domain.value_objects.PayeeType.MERCHANT) "
            + "ORDER BY o.completedAt ASC, o.orderCode ASC")
    List<Object[]> findUnsettledMerchantOrders(@org.springframework.data.repository.query.Param("payeeUserId") UUID payeeUserId);

    @Query("SELECT o FROM OrderPersistenceModel o "
            + "WHERE o.status = com.foodya.backend.domain.value_objects.OrderStatus.SUCCESS "
            + "AND o.driverUserId IS NOT NULL "
            + "AND NOT EXISTS (SELECT 1 FROM SettlementLinePersistenceModel l WHERE l.orderId = o.id "
            + "AND l.payeeType = com.foodya.backend.domain.value_objects.PayeeType.DRIVER) "
            + "ORDER BY o.completedAt ASC, o.orderCode ASC")
    List<OrderPersistenceModel> findUnsettledDriverOrders();

    @Query("SELECT o FROM OrderPersistenceModel o "
            + "WHERE o.status = com.foodya.backend.domain.value_objects.OrderStatus.SUCCESS "
            + "AND o.driverUserId = :payeeUserId "
            + "AND NOT EXISTS (SELECT 1 FROM SettlementLinePersistenceModel l WHERE l.orderId = o.id "
            + "AND l.payeeType = com.foodya.backend.domain.value_objects.PayeeType.DRIVER) "
            + "ORDER BY o.completedAt ASC, o.orderCode ASC")
    List<OrderPersistenceModel> findUnsettledDriverOrders(@org.springframework.data.repository.query.Param("payeeUserId") UUID payeeUserId);
}
