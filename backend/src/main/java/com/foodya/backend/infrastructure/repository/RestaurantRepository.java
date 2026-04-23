package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.value_objects.RestaurantStatus;
import com.foodya.backend.infrastructure.persistence.models.RestaurantPersistenceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<RestaurantPersistenceModel, UUID> {

    Page<RestaurantPersistenceModel> findByStatusInAndNameContainingIgnoreCase(Collection<RestaurantStatus> statuses,
                                                                               String nameKeyword,
                                                                               Pageable pageable);

    List<RestaurantPersistenceModel> findByH3IndexRes9InAndStatusIn(Collection<String> h3Indexes, Collection<RestaurantStatus> statuses);

    List<RestaurantPersistenceModel> findByIdInAndStatusIn(Collection<UUID> ids, Collection<RestaurantStatus> statuses);

    Optional<RestaurantPersistenceModel> findByIdAndStatusIn(UUID id, Collection<RestaurantStatus> statuses);

    Optional<RestaurantPersistenceModel> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
}
