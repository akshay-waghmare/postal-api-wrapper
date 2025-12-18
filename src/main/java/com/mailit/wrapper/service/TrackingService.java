package com.mailit.wrapper.service;

import com.mailit.wrapper.model.dto.request.CreateTrackingRequest;
import com.mailit.wrapper.model.dto.response.BatchCreateResponse;
import com.mailit.wrapper.model.dto.response.TrackingDetailResponse;
import com.mailit.wrapper.model.dto.response.TrackingListResponse;
import com.mailit.wrapper.model.entity.Client;

import java.util.List;

import org.springframework.data.domain.Pageable;

/**
 * Service interface for tracking operations.
 */
public interface TrackingService {
    
    /**
     * Create multiple trackings in a batch.
     * 
     * <p>Supports partial success - individual tracking failures do not 
     * block other valid trackings from being created.</p>
     * 
     * @param client the authenticated client
     * @param request the batch creation request
     * @return response with created and failed trackings
     */
    BatchCreateResponse createTrackings(Client client, CreateTrackingRequest request);
    
    /**
     * Get paginated list of trackings for a client.
     * 
     * @param client the authenticated client
     * @param status optional status filter
     * @param pageable pagination parameters
     * @return paginated tracking list
     */
    TrackingListResponse listTrackings(Client client, String status, Pageable pageable);
    
    /**
     * Get detailed tracking information including event history.
     * 
     * @param client the authenticated client
     * @param trackingId the wrapper tracking ID
     * @return tracking details with events
     */
    TrackingDetailResponse getTracking(Client client, String trackingId);

    /**
     * Get details for multiple trackings in a single request.
     * 
     * @param client the authenticated client
     * @param trackingIds list of wrapper tracking IDs
     * @return list of detailed tracking info
     */
    List<TrackingDetailResponse> getBatchTrackingDetails(Client client, List<String> trackingIds);
    
    /**
     * Soft delete a tracking.
     * 
     * <p>Idempotent - calling delete on an already deleted tracking returns
     * success without error.</p>
     * 
     * @param client the authenticated client
     * @param trackingId the wrapper tracking ID
     */
    void deleteTracking(Client client, String trackingId);
}
