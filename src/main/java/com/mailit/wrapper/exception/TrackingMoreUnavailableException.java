package com.mailit.wrapper.exception;

/**
 * Exception thrown when TrackingMore API is unavailable (5xx, timeout, network).
 */
public class TrackingMoreUnavailableException extends WrapperException {
    
    private static final String CODE = "UPSTREAM_UNAVAILABLE";
    private static final int HTTP_STATUS = 503;

    private final long retryAfterSeconds;

    public TrackingMoreUnavailableException() {
        super(CODE, "Tracking service is temporarily unavailable. Please retry later.", HTTP_STATUS);
        this.retryAfterSeconds = 60;
    }

    public TrackingMoreUnavailableException(String message) {
        super(CODE, message, HTTP_STATUS);
        this.retryAfterSeconds = 60;
    }

    public TrackingMoreUnavailableException(String message, long retryAfterSeconds) {
        super(CODE, message, HTTP_STATUS);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public TrackingMoreUnavailableException(String message, Throwable cause) {
        super(CODE, message, HTTP_STATUS, cause);
        this.retryAfterSeconds = 60;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
