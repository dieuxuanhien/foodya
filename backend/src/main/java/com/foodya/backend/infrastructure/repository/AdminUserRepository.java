package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.infrastructure.persistence.models.UserAccountPersistenceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<UserAccountPersistenceModel, UUID> {

    Page<UserAccountPersistenceModel> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrPhoneNumberContainingIgnoreCase(
            String username,
            String email,
            String fullName,
            String phoneNumber,
            Pageable pageable
    );

    @Query("""
            SELECT (COUNT(o) > 0)
                                                FROM OrderPersistenceModel o
            WHERE o.customerUserId = :userId
              AND o.status IN :statuses
            """)
    boolean hasCustomerOrdersInStatuses(@Param("userId") UUID userId,
                                        @Param("statuses") Collection<OrderStatus> statuses);

    @Query("""
            SELECT (COUNT(o) > 0)
                                                FROM OrderPersistenceModel o, RestaurantPersistenceModel r
            WHERE o.restaurantId = r.id
              AND r.ownerUserId = :userId
              AND o.status IN :statuses
            """)
    boolean hasMerchantOrdersInStatuses(@Param("userId") UUID userId,
                                        @Param("statuses") Collection<OrderStatus> statuses);
}
