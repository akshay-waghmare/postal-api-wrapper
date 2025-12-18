package com.mailit.wrapper.model.dto.response;

import java.time.Instant;

/**
 * Summary view of a tracking for list responses.
 * 
 * @param trackingId wrapper tracking ID
 * @param trackingNumber carrier tracking number
 * @param courierCode courier identifier
 * @param status current tracking status
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 */
public record TrackingSummaryDto(
        String trackingId,
        String trackingNumber,
        String courierCode,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
