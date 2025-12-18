package com.mailit.wrapper.model.trackingmore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from TrackingMore API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackingMoreResponse {
    
    /**
     * Response metadata.
     */
    private Meta meta;
    
    /**
     * Response data (structure varies by endpoint).
     */
    private Object data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        private int code;
        private String type;
        private String message;
    }
    
    /**
     * Check if the response indicates success.
     */
    public boolean isSuccess() {
        return meta != null && meta.getCode() >= 200 && meta.getCode() < 300;
    }
    
    /**
     * Get error message if response indicates failure.
     */
    public String getErrorMessage() {
        if (meta == null) {
            return "Unknown error";
        }
        return meta.getMessage();
    }
    
    /**
     * Get error code.
     */
    public int getErrorCode() {
        return meta != null ? meta.getCode() : -1;
    }
}
