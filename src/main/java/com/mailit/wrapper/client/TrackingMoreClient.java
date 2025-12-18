package com.mailit.wrapper.client;

import com.mailit.wrapper.model.trackingmore.TrackingMoreResponse;
import com.mailit.wrapper.model.trackingmore.TrackingMoreShipment;
import com.mailit.wrapper.model.trackingmore.TrackingMoreTrackingItem;

import java.util.List;

/**
 * Client interface for TrackingMore API operations.
 * 
 * <p>This interface abstracts the HTTP communication with TrackingMore,
 * enabling easy mocking in tests and potential implementation swapping.</p>
 */
public interface TrackingMoreClient {

    /**
     * Create multiple trackings in batch.
     * 
     * @param shipments list of shipments to track (max 40)
     * @return response with created tracking IDs and any failures
     */
    TrackingMoreResponse createBatchTrackings(List<TrackingMoreShipment> shipments);

    /**
     * Get tracking details by tracking number and courier.
     * 
     * @param trackingNumber the tracking number
     * @param courierCode the courier code
     * @return tracking details
     */
    TrackingMoreTrackingItem getTracking(String trackingNumber, String courierCode);

    /**
     * Get details for multiple trackings.
     * 
     * @param trackingNumbers list of tracking numbers
     * @return list of tracking details
     */
    List<TrackingMoreTrackingItem> getBatchTrackings(List<String> trackingNumbers);

    /**
     * Delete a tracking.
     * 
     * @param trackingNumber the tracking number
     * @param courierCode the courier code
     * @return true if deleted successfully
     */
    boolean deleteTracking(String trackingNumber, String courierCode);

    /**
     * Detect courier for a tracking number.
     * 
     * @param trackingNumber the tracking number
     * @return list of possible courier codes
     */
    List<String> detectCourier(String trackingNumber);
}
