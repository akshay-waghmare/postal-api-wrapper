package com.mailit.wrapper.model.dto.response;

/**
 * Successfully created tracking in batch response.
 * 
 * @param trackingId wrapper-generated tracking ID (e.g., "trk_9f3a2b8c")
 * @param trackingNumber the original tracking number
 * @param status creation status ("created")
 */
public record CreatedTrackingDto(
        String trackingId,
        String trackingNumber,
        String status
) {
    /**
     * Create a success response for a newly created tracking.
     */
    public static CreatedTrackingDto created(String trackingId, String trackingNumber) {
        return new CreatedTrackingDto(trackingId, trackingNumber, "created");
    }
}
