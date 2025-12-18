package com.mailit.wrapper.exception;

/**
 * Exception thrown when a requested tracking is not found.
 */
public class TrackingNotFoundException extends WrapperException {
    
    private static final String CODE = "TRACKING_NOT_FOUND";
    private static final int HTTP_STATUS = 404;

    public TrackingNotFoundException(String trackingId) {
        super(CODE, "Tracking with ID '" + trackingId + "' not found or does not belong to your account", HTTP_STATUS);
    }
}
