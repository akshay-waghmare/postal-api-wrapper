package com.mailit.wrapper.controller;

import com.mailit.wrapper.model.dto.request.CreateTrackingRequest;
import com.mailit.wrapper.model.dto.request.BatchGetRequest;
import com.mailit.wrapper.model.dto.response.*;
import com.mailit.wrapper.model.entity.Client;
import com.mailit.wrapper.service.TrackingService;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for tracking operations.
 */
@RestController
@RequestMapping("/api/v1/trackings")
@Tag(name = "Trackings", description = "Shipment tracking operations")
@SecurityRequirement(name = "apiKey")
public class TrackingController {
    
    private static final String CLIENT_ATTRIBUTE = "authenticatedClient";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    
    private final TrackingService trackingService;
    
    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }
    
    @PostMapping
    @Operation(
            summary = "Create shipment trackings",
            description = "Create up to 40 shipment trackings in a single batch. " +
                    "Partial success is supported - some trackings may fail while others succeed."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Trackings created (full or partial success)",
            content = @Content(schema = @Schema(implementation = BatchCreateResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing API key",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ResponseEntity<BatchCreateResponse> createTrackings(
            @Valid @RequestBody CreateTrackingRequest request,
            HttpServletRequest httpRequest) {
        
        Client client = getAuthenticatedClient(httpRequest);
        BatchCreateResponse response = trackingService.createTrackings(client, request);
        
        HttpStatus status = response.success() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/batch-get")
    @Operation(
            summary = "Get batch tracking details",
            description = "Get detailed information for multiple trackings in a single request (max 40)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tracking details retrieved",
            content = @Content(schema = @Schema(implementation = TrackingDetailResponse.class))
    )
    public ResponseEntity<List<TrackingDetailResponse>> getBatchTrackingDetails(
            @Valid @RequestBody BatchGetRequest request,
            HttpServletRequest httpRequest) {
        
        Client client = getAuthenticatedClient(httpRequest);
        List<TrackingDetailResponse> response = trackingService.getBatchTrackingDetails(client, request.trackingIds());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(
            summary = "List trackings",
            description = "Get a paginated list of trackings for the authenticated client."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Trackings retrieved successfully",
            content = @Content(schema = @Schema(implementation = TrackingListResponse.class))
    )
    public ResponseEntity<TrackingListResponse> listTrackings(
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        
        Client client = getAuthenticatedClient(httpRequest);
        
        int pageSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        TrackingListResponse response = trackingService.listTrackings(client, status, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{trackingId}")
    @Operation(
            summary = "Get tracking details",
            description = "Get detailed tracking information including event history."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tracking retrieved successfully",
            content = @Content(schema = @Schema(implementation = TrackingDetailResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Tracking not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ResponseEntity<TrackingDetailResponse> getTracking(
            @PathVariable String trackingId,
            HttpServletRequest httpRequest) {
        
        Client client = getAuthenticatedClient(httpRequest);
        TrackingDetailResponse response = trackingService.getTracking(client, trackingId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{trackingId}")
    @Operation(
            summary = "Delete tracking",
            description = "Soft delete a tracking. This operation is idempotent - " +
                    "deleting an already deleted tracking returns success."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Tracking deleted (or already deleted)"
    )
    public ResponseEntity<Void> deleteTracking(
            @PathVariable String trackingId,
            HttpServletRequest httpRequest) {
        
        Client client = getAuthenticatedClient(httpRequest);
        trackingService.deleteTracking(client, trackingId);
        return ResponseEntity.noContent().build();
    }
    
    private Client getAuthenticatedClient(HttpServletRequest request) {
        return (Client) request.getAttribute(CLIENT_ATTRIBUTE);
    }
}
