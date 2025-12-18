package com.mailit.wrapper.model.dto.response;

/**
 * Failed tracking in batch response.
 * 
 * @param trackingNumber the tracking number that failed
 * @param courier the courier code
 * @param error description of why it failed
 */
public record FailedTrackingDto(
        String trackingNumber,
        String courier,
        String error
) {
    /**
     * Create a failure response for validation error.
     */
    public static FailedTrackingDto validationError(String trackingNumber, String courier, String error) {
        return new FailedTrackingDto(trackingNumber, courier, error);
    }
    
    /**
     * Create a failure response for upstream error.
     */
    public static FailedTrackingDto upstreamError(String trackingNumber, String courier, String error) {
        return new FailedTrackingDto(trackingNumber, courier, "Upstream error: " + error);
    }
    
    /**
     * Create a failure response for duplicate tracking.
     */
    public static FailedTrackingDto duplicate(String trackingNumber, String courier) {
        return new FailedTrackingDto(trackingNumber, courier, "Tracking already exists");
    }
}
