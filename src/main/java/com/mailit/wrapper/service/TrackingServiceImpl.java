package com.mailit.wrapper.service;

import com.mailit.wrapper.client.TrackingMoreClient;
import com.mailit.wrapper.exception.ForbiddenException;
import com.mailit.wrapper.exception.TrackingNotFoundException;
import com.mailit.wrapper.model.WrapperStatus;
import com.mailit.wrapper.model.dto.request.CreateTrackingRequest;
import com.mailit.wrapper.model.dto.request.ShipmentDto;
import com.mailit.wrapper.model.dto.response.*;
import com.mailit.wrapper.model.entity.Client;
import com.mailit.wrapper.model.entity.Tracking;
import com.mailit.wrapper.model.trackingmore.TrackingMoreBatchData;
import com.mailit.wrapper.model.trackingmore.TrackingMoreResponse;
import com.mailit.wrapper.model.trackingmore.TrackingMoreShipment;
import com.mailit.wrapper.model.trackingmore.TrackingMoreTrackingItem;
import com.mailit.wrapper.repository.TrackingRepository;
import com.mailit.wrapper.util.StatusMapper;
import com.mailit.wrapper.util.TrackingIdGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of tracking operations.
 */
@Service
public class TrackingServiceImpl implements TrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(TrackingServiceImpl.class);
    
    private final TrackingRepository trackingRepository;
    private final TrackingMoreClient trackingMoreClient;
    private final TrackingIdGenerator trackingIdGenerator;
    private final StatusMapper statusMapper;
    private final ObjectMapper objectMapper;
    
    public TrackingServiceImpl(
            TrackingRepository trackingRepository,
            TrackingMoreClient trackingMoreClient,
            TrackingIdGenerator trackingIdGenerator,
            StatusMapper statusMapper,
            ObjectMapper objectMapper) {
        this.trackingRepository = trackingRepository;
        this.trackingMoreClient = trackingMoreClient;
        this.trackingIdGenerator = trackingIdGenerator;
        this.statusMapper = statusMapper;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional
    public BatchCreateResponse createTrackings(Client client, CreateTrackingRequest request) {
        List<CreatedTrackingDto> created = new ArrayList<>();
        List<FailedTrackingDto> failed = new ArrayList<>();
        
        // Convert to TrackingMore format
        List<TrackingMoreShipment> shipments = new ArrayList<>();
        for (ShipmentDto dto : request.shipments()) {
            // Check for duplicates
            if (trackingRepository.existsByClientAndTrackingNumberAndCourierCode(
                    client, dto.trackingNumber(), dto.courier())) {
                failed.add(FailedTrackingDto.duplicate(dto.trackingNumber(), dto.courier()));
                continue;
            }
            
            shipments.add(new TrackingMoreShipment(
                    dto.trackingNumber(),
                    dto.courier(),
                    dto.orderId(),
                    dto.originCountry(),
                    dto.destinationCountry()
            ));
        }
        
        if (shipments.isEmpty()) {
            return BatchCreateResponse.allFailed(failed);
        }
        
        try {
            // Call TrackingMore API
            TrackingMoreResponse response = trackingMoreClient.createBatchTrackings(shipments);
            
            // Parse the batch response data structure
            if (response.getData() != null) {
                TrackingMoreBatchData batchData = objectMapper.convertValue(
                        response.getData(), TrackingMoreBatchData.class);
                
                // Process successful trackings
                if (batchData.getSuccess() != null) {
                    for (TrackingMoreBatchData.TrackingMoreBatchItem item : batchData.getSuccess()) {
                        created.add(saveTracking(client, request, item.getTrackingNumber(), 
                                item.getCourierCode(), item.getId()));
                    }
                }
                
                // Process errors - handle "already exists" as success by using the returned ID
                if (batchData.getError() != null) {
                    for (TrackingMoreBatchData.TrackingMoreBatchError error : batchData.getError()) {
                        if (error.getErrorCode() == 4101) {
                            // "Tracking No. already exists" - treat as success, use the ID
                            created.add(saveTracking(client, request, error.getTrackingNumber(), 
                                    error.getCourierCode(), error.getId()));
                            logger.info("Tracking {} already exists in TrackingMore, using existing ID: {}", 
                                    error.getTrackingNumber(), error.getId());
                        } else {
                            // Real error
                            failed.add(FailedTrackingDto.upstreamError(
                                    error.getTrackingNumber(),
                                    error.getCourierCode(),
                                    error.getErrorMessage()
                            ));
                        }
                    }
                }
            }
            
            // Handle any errors from the response
            if (response.getMeta() != null && response.getMeta().getCode() != 200) {
                logger.warn("TrackingMore returned error: {}", response.getMeta().getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Failed to create trackings with TrackingMore", e);
            // Mark all remaining shipments as failed
            for (TrackingMoreShipment shipment : shipments) {
                boolean alreadyProcessed = created.stream()
                        .anyMatch(c -> c.trackingNumber().equals(shipment.getTrackingNumber()));
                if (!alreadyProcessed) {
                    failed.add(FailedTrackingDto.upstreamError(
                            shipment.getTrackingNumber(), 
                            shipment.getCourierCode(), 
                            e.getMessage()
                    ));
                }
            }
        }
        
        return BatchCreateResponse.partial(created, failed);
    }
    
    /**
     * Saves a tracking to the database and returns the created DTO.
     */
    private CreatedTrackingDto saveTracking(Client client, CreateTrackingRequest request,
            String trackingNumber, String courierCode, String trackingmoreId) {
        String trackingId = trackingIdGenerator.generate();
        
        // Find matching shipment DTO for orderId
        Optional<ShipmentDto> matchingDto = request.shipments().stream()
                .filter(s -> s.trackingNumber().equals(trackingNumber))
                .findFirst();
        
        // Save to database
        Tracking tracking = new Tracking();
        tracking.setTrackingId(trackingId);
        tracking.setClient(client);
        tracking.setTrackingNumber(trackingNumber);
        tracking.setCourierCode(courierCode);
        tracking.setTrackingmoreId(trackingmoreId);
        tracking.setStatus(WrapperStatus.PENDING);
        matchingDto.ifPresent(dto -> {
            tracking.setOrderId(dto.orderId());
            tracking.setOriginCountry(dto.originCountry());
            tracking.setDestinationCountry(dto.destinationCountry());
        });
        
        trackingRepository.save(tracking);
        logger.info("Created tracking {} for client {}", trackingId, client.getId());
        
        return CreatedTrackingDto.created(trackingId, trackingNumber);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TrackingListResponse listTrackings(Client client, String status, Pageable pageable) {
        Page<Tracking> page;
        
        if (status != null && !status.isEmpty()) {
            WrapperStatus wrapperStatus = WrapperStatus.valueOf(status.toUpperCase());
            page = trackingRepository.findByClientAndStatus(client, wrapperStatus, pageable);
        } else {
            page = trackingRepository.findByClient(client, pageable);
        }
        
        List<TrackingSummaryDto> trackings = page.getContent().stream()
                .map(this::toSummaryDto)
                .toList();
        
        PaginationMeta pagination = new PaginationMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
        
        return new TrackingListResponse(trackings, pagination);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TrackingDetailResponse getTracking(Client client, String trackingId) {
        Tracking tracking = findTrackingForClient(client, trackingId);
        
        // Fetch latest events from TrackingMore
        List<TrackingEventDto> events = new ArrayList<>();
        TrackingMoreTrackingItem upstreamTracking = null;
        
        try {
            upstreamTracking = trackingMoreClient.getTracking(
                    tracking.getTrackingNumber(), tracking.getCourierCode());
            
            if (upstreamTracking != null) {
                // Update status if changed
                WrapperStatus newStatus = statusMapper.map(upstreamTracking.getDeliveryStatus());
                if (newStatus != tracking.getStatus()) {
                    tracking.setStatus(newStatus);
                    trackingRepository.save(tracking);
                }
                
                // Map events from checkpoints
                events = upstreamTracking.getAllCheckpoints().stream()
                        .map(cp -> new TrackingEventDto(
                                cp.getCheckpointDate(),
                                cp.getCheckpointDeliveryStatus(),
                                cp.getCheckpointDeliverySubstatus(),
                                cp.getTrackingDetail(),
                                cp.getLocation()
                        ))
                        .toList();
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch upstream tracking for {}: {}", 
                    trackingId, e.getMessage());
        }
        
        return new TrackingDetailResponse(
                tracking.getTrackingId(),
                tracking.getTrackingNumber(),
                tracking.getCourierCode(),
                tracking.getStatus().name(),
                upstreamTracking != null ? upstreamTracking.getSubstatus() : null,
                tracking.getOrderId(),
                tracking.getOriginCountry(),
                tracking.getDestinationCountry(),
                upstreamTracking != null ? upstreamTracking.getTransitTime() : null,
                upstreamTracking != null ? upstreamTracking.getLatestEvent() : null,
                upstreamTracking != null ? upstreamTracking.getLatestCheckpointTime() : null,
                upstreamTracking != null ? upstreamTracking.getSignedBy() : null,
                tracking.getCreatedAt(),
                tracking.getUpdatedAt(),
                events
        );
    }
    
    @Override
    @Transactional
    public List<TrackingDetailResponse> getBatchTrackingDetails(Client client, List<String> trackingIds) {
        // 1. Find all trackings
        List<Tracking> trackings = trackingRepository.findByClientAndTrackingIdIn(client, trackingIds);
        
        if (trackings.isEmpty()) {
            return List.of();
        }
        
        // 2. Extract tracking numbers for upstream call
        List<String> trackingNumbers = trackings.stream()
                .map(Tracking::getTrackingNumber)
                .toList();
                
        // 3. Fetch from upstream
        List<TrackingMoreTrackingItem> upstreamItems = trackingMoreClient.getBatchTrackings(trackingNumbers);
        
        // 4. Map upstream items by tracking number for easy lookup
        Map<String, TrackingMoreTrackingItem> upstreamMap = upstreamItems.stream()
                .collect(Collectors.toMap(
                        TrackingMoreTrackingItem::getTrackingNumber,
                        Function.identity(),
                        (existing, replacement) -> existing // Handle duplicates if any
                ));
                
        // 5. Build response and update statuses
        List<TrackingDetailResponse> responses = new ArrayList<>();
        
        for (Tracking tracking : trackings) {
            TrackingMoreTrackingItem upstream = upstreamMap.get(tracking.getTrackingNumber());
            List<TrackingEventDto> events = new ArrayList<>();
            
            if (upstream != null) {
                // Update status if changed
                WrapperStatus newStatus = statusMapper.map(upstream.getDeliveryStatus());
                if (newStatus != tracking.getStatus()) {
                    tracking.setStatus(newStatus);
                    trackingRepository.save(tracking);
                }
                
                // Map events
                events = upstream.getAllCheckpoints().stream()
                        .map(cp -> new TrackingEventDto(
                                cp.getCheckpointDate(),
                                cp.getCheckpointDeliveryStatus(),
                                cp.getCheckpointDeliverySubstatus(),
                                cp.getTrackingDetail(),
                                cp.getLocation()
                        ))
                        .toList();
            }
            
            responses.add(new TrackingDetailResponse(
                    tracking.getTrackingId(),
                    tracking.getTrackingNumber(),
                    tracking.getCourierCode(),
                    tracking.getStatus().name(),
                    upstream != null ? upstream.getSubstatus() : null,
                    tracking.getOrderId(),
                    tracking.getOriginCountry(),
                    tracking.getDestinationCountry(),
                    upstream != null ? upstream.getTransitTime() : null,
                    upstream != null ? upstream.getLatestEvent() : null,
                    upstream != null ? upstream.getLatestCheckpointTime() : null,
                    upstream != null ? upstream.getSignedBy() : null,
                    tracking.getCreatedAt(),
                    tracking.getUpdatedAt(),
                    events
            ));
        }
        
        return responses;
    }

    @Override
    @Transactional
    public void deleteTracking(Client client, String trackingId) {
        Optional<Tracking> optionalTracking = trackingRepository.findByTrackingId(trackingId);
        
        if (optionalTracking.isEmpty()) {
            // Idempotent - already deleted or never existed
            logger.debug("Tracking {} not found for delete, treating as success", trackingId);
            return;
        }
        
        Tracking tracking = optionalTracking.get();
        
        // Verify ownership
        if (!tracking.getClient().getId().equals(client.getId())) {
            throw new ForbiddenException("Access denied to tracking: " + trackingId);
        }
        
        // Soft delete
        tracking.softDelete();
        trackingRepository.save(tracking);
        
        // Best effort delete from TrackingMore
        try {
            trackingMoreClient.deleteTracking(tracking.getTrackingNumber(), tracking.getCourierCode());
        } catch (Exception e) {
            logger.warn("Failed to delete tracking from TrackingMore: {}", e.getMessage());
        }
        
        logger.info("Soft deleted tracking {} for client {}", trackingId, client.getId());
    }
    
    private Tracking findTrackingForClient(Client client, String trackingId) {
        Tracking tracking = trackingRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new TrackingNotFoundException(trackingId));
        
        if (!tracking.getClient().getId().equals(client.getId())) {
            throw new ForbiddenException("Access denied to tracking: " + trackingId);
        }
        
        return tracking;
    }
    
    private TrackingSummaryDto toSummaryDto(Tracking tracking) {
        return new TrackingSummaryDto(
                tracking.getTrackingId(),
                tracking.getTrackingNumber(),
                tracking.getCourierCode(),
                tracking.getStatus().name(),
                tracking.getCreatedAt(),
                tracking.getUpdatedAt()
        );
    }
}
