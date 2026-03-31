package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.entities.Restaurant;
import com.foodya.backend.domain.value_objects.RestaurantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    Page<Restaurant> findByStatusInAndNameContainingIgnoreCase(Collection<RestaurantStatus> statuses,
                                                                String nameKeyword,
                                                                Pageable pageable);

    List<Restaurant> findByH3IndexRes9InAndStatusIn(Collection<String> h3Indexes, Collection<RestaurantStatus> statuses);

    List<Restaurant> findByIdInAndStatusIn(Collection<UUID> ids, Collection<RestaurantStatus> statuses);

    Optional<Restaurant> findByIdAndStatusIn(UUID id, Collection<RestaurantStatus> statuses);

    Optional<Restaurant> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
}
