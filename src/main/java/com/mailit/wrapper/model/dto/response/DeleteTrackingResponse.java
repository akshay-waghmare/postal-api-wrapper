package com.mailit.wrapper.model.dto.response;

/**
 * Response for delete tracking operation.
 * 
 * <p>Note: The DELETE endpoint returns 204 No Content by default,
 * but this DTO is available for cases where a response body is needed.</p>
 * 
 * @param success whether the deletion was successful
 * @param trackingId the tracking ID that was deleted
 * @param message optional status message
 */
public record DeleteTrackingResponse(
        boolean success,
        String trackingId,
        String message
) {
    /**
     * Create a success response.
     */
    public static DeleteTrackingResponse success(String trackingId) {
        return new DeleteTrackingResponse(true, trackingId, "Tracking deleted successfully");
    }
    
    /**
     * Create an already deleted response (idempotent).
     */
    public static DeleteTrackingResponse alreadyDeleted(String trackingId) {
        return new DeleteTrackingResponse(true, trackingId, "Tracking was already deleted");
    }
}
