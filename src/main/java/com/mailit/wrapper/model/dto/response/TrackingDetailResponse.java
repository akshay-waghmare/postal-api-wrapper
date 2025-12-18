package com.mailit.wrapper.model.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Detailed tracking response including event history.
 * 
 * @param trackingId wrapper tracking ID
 * @param trackingNumber carrier tracking number
 * @param courierCode courier identifier
 * @param status current tracking status
 * @param substatus more specific status code
 * @param orderId client order reference (optional)
 * @param originCountry origin country ISO2 code
 * @param destinationCountry destination country ISO2 code
 * @param transitTime transit time in days
 * @param latestEvent latest event description
 * @param latestCheckpointTime latest checkpoint timestamp
 * @param signedBy who signed for the package (if delivered)
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 * @param events list of tracking events
 */
public record TrackingDetailResponse(
        String trackingId,
        String trackingNumber,
        String courierCode,
        String status,
        String substatus,
        String orderId,
        String originCountry,
        String destinationCountry,
        Integer transitTime,
        String latestEvent,
        String latestCheckpointTime,
        String signedBy,
        Instant createdAt,
        Instant updatedAt,
        List<TrackingEventDto> events
) {}
