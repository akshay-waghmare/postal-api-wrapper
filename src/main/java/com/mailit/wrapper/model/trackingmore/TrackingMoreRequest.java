package com.mailit.wrapper.model.trackingmore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for TrackingMore batch create API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingMoreRequest {
    
    /**
     * List of trackings to create (max 40).
     */
    private List<TrackingMoreShipment> trackings;
}
