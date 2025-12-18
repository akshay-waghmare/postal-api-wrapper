package com.mailit.wrapper.util;

import com.mailit.wrapper.model.WrapperStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Maps TrackingMore status codes to normalized wrapper statuses.
 * 
 * <p>Mapping table (from spec.md SC-002):</p>
 * <table>
 *   <tr><th>TrackingMore</th><th>Wrapper</th></tr>
 *   <tr><td>pending</td><td>PENDING</td></tr>
 *   <tr><td>notfound</td><td>NOT_FOUND</td></tr>
 *   <tr><td>transit</td><td>IN_TRANSIT</td></tr>
 *   <tr><td>pickup</td><td>OUT_FOR_DELIVERY</td></tr>
 *   <tr><td>delivered</td><td>DELIVERED</td></tr>
 *   <tr><td>expired</td><td>EXPIRED</td></tr>
 *   <tr><td>undelivered</td><td>EXCEPTION</td></tr>
 *   <tr><td>exception</td><td>EXCEPTION</td></tr>
 *   <tr><td>inforeceived</td><td>PENDING</td></tr>
 * </table>
 */
@Slf4j
@Component
public class StatusMapper {

    /**
     * Map a TrackingMore status string to WrapperStatus.
     * 
     * @param trackingMoreStatus the status from TrackingMore API
     * @return the normalized WrapperStatus
     */
    public WrapperStatus map(String trackingMoreStatus) {
        if (trackingMoreStatus == null || trackingMoreStatus.isBlank()) {
            log.warn("Null or empty TrackingMore status received, defaulting to PENDING");
            return WrapperStatus.PENDING;
        }

        return switch (trackingMoreStatus.toLowerCase().trim()) {
            case "pending", "inforeceived" -> WrapperStatus.PENDING;
            case "notfound" -> WrapperStatus.NOT_FOUND;
            case "transit" -> WrapperStatus.IN_TRANSIT;
            case "pickup" -> WrapperStatus.OUT_FOR_DELIVERY;
            case "delivered" -> WrapperStatus.DELIVERED;
            case "expired" -> WrapperStatus.EXPIRED;
            case "undelivered", "exception" -> WrapperStatus.EXCEPTION;
            default -> {
                log.warn("Unknown TrackingMore status '{}', defaulting to PENDING", trackingMoreStatus);
                yield WrapperStatus.PENDING;
            }
        };
    }

    /**
     * Check if a TrackingMore status indicates a final state.
     * 
     * @param trackingMoreStatus the status from TrackingMore API
     * @return true if no further updates expected
     */
    public boolean isFinalStatus(String trackingMoreStatus) {
        if (trackingMoreStatus == null) {
            return false;
        }
        
        return switch (trackingMoreStatus.toLowerCase().trim()) {
            case "delivered", "expired" -> true;
            default -> false;
        };
    }
}
