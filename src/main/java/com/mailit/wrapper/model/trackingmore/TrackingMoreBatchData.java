package com.mailit.wrapper.model.trackingmore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Data structure for batch create response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackingMoreBatchData {
    
    /**
     * Successfully created trackings.
     */
    @JsonProperty("success")
    private List<TrackingMoreBatchItem> success = new ArrayList<>();
    
    /**
     * Failed trackings with error details.
     */
    @JsonProperty("error")
    private List<TrackingMoreBatchError> error = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackingMoreBatchItem {
        private String id;
        
        @JsonProperty("tracking_number")
        private String trackingNumber;
        
        @JsonProperty("courier_code")
        private String courierCode;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackingMoreBatchError {
        private String id;
        
        @JsonProperty("tracking_number")
        private String trackingNumber;
        
        @JsonProperty("courier_code")
        private String courierCode;
        
        @JsonProperty("errorCode")
        private int errorCode;
        
        @JsonProperty("errorMessage")
        private String errorMessage;
    }
}
