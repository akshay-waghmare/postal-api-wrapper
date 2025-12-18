package com.mailit.wrapper.model.trackingmore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single shipment in TrackingMore request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingMoreShipment {
    
    /**
     * The tracking number from the carrier.
     */
    @JsonProperty("tracking_number")
    private String trackingNumber;
    
    /**
     * The courier/carrier code (e.g., "usps", "india-post").
     */
    @JsonProperty("courier_code")
    private String courierCode;
    
    /**
     * Optional order ID for reference.
     */
    @JsonProperty("order_id")
    private String orderId;
    
    /**
     * ISO 3166-1 alpha-2 origin country code.
     */
    @JsonProperty("origin_country_iso2")
    private String originCountry;
    
    /**
     * ISO 3166-1 alpha-2 destination country code.
     */
    @JsonProperty("destination_country_iso2")
    private String destinationCountry;
}
