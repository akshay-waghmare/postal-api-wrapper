package com.mailit.wrapper.exception;

/**
 * Exception thrown when TrackingMore API returns an error.
 */
public class TrackingMoreException extends WrapperException {
    
    private static final int DEFAULT_HTTP_STATUS = 502; // Bad Gateway

    private final String upstreamCode;
    private final String upstreamMessage;

    public TrackingMoreException(String code, String message) {
        super(code, message, DEFAULT_HTTP_STATUS);
        this.upstreamCode = code;
        this.upstreamMessage = message;
    }

    public TrackingMoreException(String code, String message, int httpStatus) {
        super(code, message, httpStatus);
        this.upstreamCode = code;
        this.upstreamMessage = message;
    }

    public TrackingMoreException(String code, String message, Throwable cause) {
        super(code, message, DEFAULT_HTTP_STATUS, cause);
        this.upstreamCode = code;
        this.upstreamMessage = message;
    }

    public String getUpstreamCode() {
        return upstreamCode;
    }

    public String getUpstreamMessage() {
        return upstreamMessage;
    }
}
