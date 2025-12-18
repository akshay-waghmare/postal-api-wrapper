package com.mailit.wrapper.model.dto.response;

import java.util.List;

/**
 * Response for batch tracking creation.
 * 
 * <p>Supports partial success - some trackings may succeed while others fail.
 * A single failed shipment does not block other valid shipments.</p>
 * 
 * @param success true if at least one tracking was created
 * @param created list of successfully created trackings
 * @param failed list of trackings that failed
 */
public record BatchCreateResponse(
        boolean success,
        List<CreatedTrackingDto> created,
        List<FailedTrackingDto> failed
) {
    /**
     * Create a fully successful response.
     */
    public static BatchCreateResponse allSuccess(List<CreatedTrackingDto> created) {
        return new BatchCreateResponse(true, created, List.of());
    }
    
    /**
     * Create a partial success response.
     */
    public static BatchCreateResponse partial(
            List<CreatedTrackingDto> created, 
            List<FailedTrackingDto> failed) {
        return new BatchCreateResponse(!created.isEmpty(), created, failed);
    }
    
    /**
     * Create a complete failure response.
     */
    public static BatchCreateResponse allFailed(List<FailedTrackingDto> failed) {
        return new BatchCreateResponse(false, List.of(), failed);
    }
    
    /**
     * Get count of successfully created trackings.
     */
    public int createdCount() {
        return created != null ? created.size() : 0;
    }
    
    /**
     * Get count of failed trackings.
     */
    public int failedCount() {
        return failed != null ? failed.size() : 0;
    }
}
