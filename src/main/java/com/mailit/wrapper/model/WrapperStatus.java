package com.mailit.wrapper.model;

/**
 * Normalized tracking status values exposed to API clients.
 * 
 * <p>These statuses abstract away TrackingMore-specific status codes,
 * providing a consistent interface regardless of upstream provider.</p>
 * 
 * @see com.mailit.wrapper.util.StatusMapper
 */
public enum WrapperStatus {
    
    /**
     * Tracking created, awaiting carrier information.
     * Maps from TrackingMore: pending, inforeceived
     */
    PENDING("pending"),
    
    /**
     * Carrier has no record of this tracking number.
     * Maps from TrackingMore: notfound
     */
    NOT_FOUND("not_found"),
    
    /**
     * Package is in transit to destination.
     * Maps from TrackingMore: transit
     */
    IN_TRANSIT("in_transit"),
    
    /**
     * Package is out for delivery to recipient.
     * Maps from TrackingMore: pickup
     */
    OUT_FOR_DELIVERY("out_for_delivery"),
    
    /**
     * Package successfully delivered.
     * Maps from TrackingMore: delivered
     */
    DELIVERED("delivered"),
    
    /**
     * Delivery exception or problem occurred.
     * Maps from TrackingMore: exception, undelivered
     */
    EXCEPTION("exception"),
    
    /**
     * Tracking has expired (past retention period).
     * Maps from TrackingMore: expired
     */
    EXPIRED("expired"),
    
    /**
     * Package returned to sender.
     * Future status for return shipments.
     */
    RETURNED("returned");

    private final String value;

    WrapperStatus(String value) {
        this.value = value;
    }

    /**
     * Returns the JSON-serializable value for this status.
     * 
     * @return lowercase snake_case status string
     */
    public String getValue() {
        return value;
    }

    /**
     * Parse a status string to enum value.
     * 
     * @param value the status string (case-insensitive)
     * @return the matching WrapperStatus
     * @throws IllegalArgumentException if value doesn't match any status
     */
    public static WrapperStatus fromValue(String value) {
        for (WrapperStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
