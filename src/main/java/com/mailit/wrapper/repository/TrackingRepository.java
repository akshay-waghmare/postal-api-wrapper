package com.mailit.wrapper.repository;

import com.mailit.wrapper.model.WrapperStatus;
import com.mailit.wrapper.model.entity.Client;
import com.mailit.wrapper.model.entity.Tracking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Tracking entity operations.
 * 
 * <p>All queries automatically filter out soft-deleted records
 * due to the {@code @SQLRestriction} annotation on the entity.</p>
 */
@Repository
public interface TrackingRepository extends JpaRepository<Tracking, Long> {

    /**
     * Find a tracking by its wrapper tracking ID.
     * 
     * @param trackingId the wrapper tracking ID (e.g., "trk_9f3a2b8c")
     * @return the tracking if found and not deleted
     */
    Optional<Tracking> findByTrackingId(String trackingId);

    /**
     * Find a tracking by wrapper ID and client ID.
     * Used for ownership validation.
     * 
     * @param trackingId the wrapper tracking ID
     * @param clientId the client's ID
     * @return the tracking if found, owned by client, and not deleted
     */
    Optional<Tracking> findByTrackingIdAndClientId(String trackingId, Long clientId);

    /**
     * Find all trackings for a client.
     * 
     * @param clientId the client's ID
     * @param pageable pagination parameters
     * @return page of trackings
     */
    Page<Tracking> findByClientId(Long clientId, Pageable pageable);
    
    /**
     * Find all trackings for a client entity.
     * 
     * @param client the client entity
     * @param pageable pagination parameters
     * @return page of trackings
     */
    Page<Tracking> findByClient(Client client, Pageable pageable);

    /**
     * Find trackings by client and a list of tracking IDs.
     * 
     * @param client the client entity
     * @param trackingIds list of tracking IDs
     * @return list of trackings
     */
    List<Tracking> findByClientAndTrackingIdIn(Client client, Collection<String> trackingIds);
    
    /**
     * Find trackings by client and status.
     * 
     * @param client the client entity
     * @param status the tracking status
     * @param pageable pagination parameters
     * @return page of matching trackings
     */
    Page<Tracking> findByClientAndStatus(Client client, WrapperStatus status, Pageable pageable);
    
    /**
     * Check if a tracking number already exists for a client.
     * 
     * @param client the client entity
     * @param trackingNumber the tracking number
     * @param courierCode the courier code
     * @return true if already exists
     */
    boolean existsByClientAndTrackingNumberAndCourierCode(
            Client client, String trackingNumber, String courierCode);

    /**
     * Find trackings by client and status.
     * 
     * @param clientId the client's ID
     * @param status the tracking status
     * @param pageable pagination parameters
     * @return page of matching trackings
     */
    Page<Tracking> findByClientIdAndStatus(Long clientId, WrapperStatus status, Pageable pageable);

    /**
     * Find trackings by tracking numbers (within client scope).
     * 
     * @param clientId the client's ID
     * @param trackingNumbers list of tracking numbers to find
     * @return list of matching trackings
     */
    List<Tracking> findByClientIdAndTrackingNumberIn(Long clientId, Collection<String> trackingNumbers);

    /**
     * Find trackings by tracking numbers and status (within client scope).
     * 
     * @param clientId the client's ID
     * @param trackingNumbers list of tracking numbers
     * @param status the status to filter by
     * @param pageable pagination parameters
     * @return page of matching trackings
     */
    @Query("SELECT t FROM Tracking t WHERE t.client.id = :clientId " +
           "AND t.trackingNumber IN :trackingNumbers " +
           "AND (:status IS NULL OR t.status = :status)")
    Page<Tracking> findByClientIdAndTrackingNumbersAndStatus(
            @Param("clientId") Long clientId,
            @Param("trackingNumbers") Collection<String> trackingNumbers,
            @Param("status") WrapperStatus status,
            Pageable pageable);

    /**
     * Find a tracking by TrackingMore ID.
     * 
     * @param trackingmoreId the upstream TrackingMore ID
     * @return the tracking if found
     */
    Optional<Tracking> findByTrackingmoreId(String trackingmoreId);

    /**
     * Check if a tracking number already exists for a client.
     * Used to prevent duplicate trackings.
     * 
     * @param clientId the client's ID
     * @param trackingNumber the tracking number
     * @param courierCode the courier code
     * @return true if already exists
     */
    boolean existsByClientIdAndTrackingNumberAndCourierCode(
            Long clientId, String trackingNumber, String courierCode);

    /**
     * Count trackings for a client.
     * 
     * @param clientId the client's ID
     * @return count of active trackings
     */
    long countByClientId(Long clientId);

    /**
     * Find all trackings for a client with optional status filter.
     * 
     * @param clientId the client's ID
     * @param status optional status filter (null for all)
     * @param pageable pagination parameters
     * @return page of trackings
     */
    @Query("SELECT t FROM Tracking t WHERE t.client.id = :clientId " +
           "AND (:status IS NULL OR t.status = :status)")
    Page<Tracking> findByClientIdWithOptionalStatus(
            @Param("clientId") Long clientId,
            @Param("status") WrapperStatus status,
            Pageable pageable);
}
