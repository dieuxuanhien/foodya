package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.DriverOnlineSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Outbound port for persisting and querying driver online sessions.
 */
public interface DriverOnlineSessionPort {

    /**
     * Finds the current open session (ended_at IS NULL) for the given driver.
     * A driver should have at most one open session at any time.
     */
    Optional<DriverOnlineSession> findActiveSession(UUID driverUserId);

    /**
     * Persists (create or update) the given session.
     *
     * @param session the session to save
     * @return the saved session (with generated id if new)
     */
    DriverOnlineSession save(DriverOnlineSession session);

    /**
     * Finds all active (non-closed, ONLINE or ON_DELIVERY) sessions whose H3 cell
     * is in the given set. Used for driver matching.
     *
     * @param h3Indexes set of H3 res9 cell indexes to search within
     * @return matching active sessions
     */
    List<DriverOnlineSession> findOnlineDriversInH3Cells(Set<String> h3Indexes);

    /**
     * Efficiently stamps the heartbeat timestamp on a session without loading the full entity.
     *
     * @param sessionId the session to update
     * @param now       the current time
     */
    void updateHeartbeat(UUID sessionId, Instant now);
}
