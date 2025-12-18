package com.mailit.wrapper.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for batch tracking creation.
 * 
 * @param shipments list of shipments to track (max 40 per request)
 */
public record CreateTrackingRequest(
        @NotEmpty(message = "At least one shipment is required")
        @Size(max = 40, message = "Maximum 40 shipments per request")
        @Valid
        List<ShipmentDto> shipments
) {
    /**
     * Create a request with a single shipment.
     */
    public static CreateTrackingRequest single(ShipmentDto shipment) {
        return new CreateTrackingRequest(List.of(shipment));
    }
    
    /**
     * Get the number of shipments in this request.
     */
    public int size() {
        return shipments != null ? shipments.size() : 0;
    }
}
