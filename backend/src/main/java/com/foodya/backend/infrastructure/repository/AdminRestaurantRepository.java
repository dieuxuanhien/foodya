package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.value_objects.OrderStatus;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.domain.entities.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface AdminRestaurantRepository extends JpaRepository<Restaurant, UUID> {

    Page<Restaurant> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Restaurant> findByStatusAndNameContainingIgnoreCase(RestaurantStatus status, String keyword, Pageable pageable);

    @Query("""
            SELECT (COUNT(o) > 0)
            FROM Order o
            WHERE o.restaurantId = :restaurantId
              AND o.status IN :statuses
            """)
    boolean hasOrdersInStatuses(@Param("restaurantId") UUID restaurantId,
                                @Param("statuses") Collection<OrderStatus> statuses);
}
