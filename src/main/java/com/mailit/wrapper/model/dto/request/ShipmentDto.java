package com.mailit.wrapper.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Single shipment data for tracking creation.
 * 
 * @param trackingNumber the carrier tracking number (required)
 * @param courier courier code (e.g., "usps", "india-post")
 * @param orderId optional client order reference
 * @param originCountry ISO 3166-1 alpha-2 origin country code
 * @param destinationCountry ISO 3166-1 alpha-2 destination country code
 */
public record ShipmentDto(
        @NotBlank(message = "Tracking number is required")
        String trackingNumber,
        
        @NotBlank(message = "Courier code is required")
        @Pattern(regexp = "^[a-z0-9-]+$", 
                 message = "Courier code must be lowercase alphanumeric with hyphens")
        String courier,
        
        String orderId,
        
        @Size(min = 2, max = 2, message = "Origin country must be 2-character ISO code")
        String originCountry,
        
        @Size(min = 2, max = 2, message = "Destination country must be 2-character ISO code")
        String destinationCountry
) {
    /**
     * Create a ShipmentDto with only required fields.
     */
    public ShipmentDto(String trackingNumber, String courier) {
        this(trackingNumber, courier, null, null, null);
    }
}
