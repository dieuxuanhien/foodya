package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.DriverOnlineSessionPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DriverOnlineSessionPersistenceModel}.
 */
public interface DriverOnlineSessionRepository
        extends JpaRepository<DriverOnlineSessionPersistenceModel, UUID> {

    /**
     * Finds the single open (not yet ended) session for a driver.
     * A driver should have at most one open session at any time.
     */
    @Query("SELECT s FROM DriverOnlineSessionPersistenceModel s " +
           "WHERE s.driverUserId = :driverUserId AND s.endedAt IS NULL " +
           "ORDER BY s.startedAt DESC")
    Optional<DriverOnlineSessionPersistenceModel> findActiveByDriverUserId(
            @Param("driverUserId") UUID driverUserId);

    /**
     * Finds all active sessions whose H3 cell is in the given set and whose status indicates
     * the driver is available or delivering (not offline/suspended).
     */
    @Query("SELECT s FROM DriverOnlineSessionPersistenceModel s " +
           "WHERE s.h3IndexRes9 IN :h3Indexes " +
           "AND s.status IN ('ONLINE', 'ON_DELIVERY') " +
           "AND s.endedAt IS NULL")
    List<DriverOnlineSessionPersistenceModel> findOnlineDriversInH3Cells(
            @Param("h3Indexes") Set<String> h3Indexes);

    /**
     * Efficiently updates just the heartbeat timestamp without loading the full entity.
     */
    @Modifying
    @Transactional
    @Query("UPDATE DriverOnlineSessionPersistenceModel s " +
           "SET s.lastHeartbeat = :now " +
           "WHERE s.id = :sessionId")
    void updateHeartbeat(@Param("sessionId") UUID sessionId,
                         @Param("now") OffsetDateTime now);
}
