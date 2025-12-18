package com.mailit.wrapper.model.trackingmore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracking item returned from TrackingMore API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackingMoreTrackingItem {
    
    /**
     * TrackingMore's internal ID.
     */
    private String id;
    
    /**
     * The tracking number.
     */
    @JsonProperty("tracking_number")
    private String trackingNumber;
    
    /**
     * Courier code.
     */
    @JsonProperty("courier_code")
    private String courierCode;
    
    /**
     * Current delivery status (e.g., "delivered", "transit", "pending").
     */
    @JsonProperty("delivery_status")
    private String deliveryStatus;
    
    /**
     * Delivery sub-status (e.g., "delivered001", "transit002").
     */
    @JsonProperty("substatus")
    private String substatus;
    
    /**
     * Order ID (if provided).
     */
    @JsonProperty("order_id")
    private String orderId;
    
    /**
     * Origin country ISO2 code.
     */
    @JsonProperty("origin_country")
    private String originCountry;
    
    /**
     * Origin city.
     */
    @JsonProperty("origin_city")
    private String originCity;
    
    /**
     * Destination country ISO2 code.
     */
    @JsonProperty("destination_country")
    private String destinationCountry;
    
    /**
     * Destination city.
     */
    @JsonProperty("destination_city")
    private String destinationCity;
    
    /**
     * Latest checkpoint time.
     */
    @JsonProperty("latest_checkpoint_time")
    private String latestCheckpointTime;
    
    /**
     * Latest event description.
     */
    @JsonProperty("latest_event")
    private String latestEvent;
    
    /**
     * Transit time in days.
     */
    @JsonProperty("transit_time")
    private Integer transitTime;
    
    /**
     * Signed by (for delivered packages).
     */
    @JsonProperty("signed_by")
    private String signedBy;
    
    /**
     * Created timestamp.
     */
    @JsonProperty("created_at")
    private String createdAt;
    
    /**
     * Updated timestamp.
     */
    @JsonProperty("update_at")
    private String updateAt;
    
    /**
     * Origin tracking info with checkpoints.
     */
    @JsonProperty("origin_info")
    private TrackingInfo originInfo;

    /**
     * Destination tracking info with checkpoints.
     */
    @JsonProperty("destination_info")
    private TrackingInfo destinationInfo;
    
    /**
     * Get all tracking events from origin and destination info.
     */
    public List<TrackingCheckpoint> getAllCheckpoints() {
        List<TrackingCheckpoint> checkpoints = new ArrayList<>();
        if (originInfo != null && originInfo.getTrackinfo() != null) {
            checkpoints.addAll(originInfo.getTrackinfo());
        }
        if (destinationInfo != null && destinationInfo.getTrackinfo() != null) {
            checkpoints.addAll(destinationInfo.getTrackinfo());
        }
        return checkpoints;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackingInfo {
        @JsonProperty("courier_code")
        private String courierCode;
        
        @JsonProperty("courier_phone")
        private String courierPhone;
        
        @JsonProperty("weblink")
        private String weblink;
        
        @JsonProperty("tracking_link")
        private String trackingLink;
        
        @JsonProperty("trackinfo")
        private List<TrackingCheckpoint> trackinfo;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackingCheckpoint {
        @JsonProperty("checkpoint_date")
        private String checkpointDate;
        
        @JsonProperty("checkpoint_delivery_status")
        private String checkpointDeliveryStatus;
        
        @JsonProperty("checkpoint_delivery_substatus")
        private String checkpointDeliverySubstatus;
        
        @JsonProperty("tracking_detail")
        private String trackingDetail;
        
        @JsonProperty("location")
        private String location;
        
        @JsonProperty("country_iso2")
        private String countryIso2;
        
        @JsonProperty("state")
        private String state;
        
        @JsonProperty("city")
        private String city;
        
        @JsonProperty("zip")
        private String zip;
    }
}
