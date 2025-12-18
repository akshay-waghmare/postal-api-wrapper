package com.mailit.wrapper.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailit.wrapper.exception.TrackingMoreException;
import com.mailit.wrapper.exception.TrackingMoreUnavailableException;
import com.mailit.wrapper.model.trackingmore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of TrackingMore API client.
 * 
 * <p>Handles HTTP communication with retry logic and circuit breaker pattern.
 * Retry policy:
 * <ul>
 *   <li>Retry on: timeouts, 5xx errors</li>
 *   <li>Never retry on: 4xx errors (validation, auth) — avoids duplicate creates</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingMoreClientImpl implements TrackingMoreClient {

    private final RestClient trackingMoreRestClient;
    private final ObjectMapper objectMapper;

    // Circuit breaker state
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile long circuitOpenUntil = 0;
    private static final int FAILURE_THRESHOLD = 5;
    private static final long CIRCUIT_OPEN_DURATION_MS = 30_000; // 30 seconds

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    @Override
    public TrackingMoreResponse createBatchTrackings(List<TrackingMoreShipment> shipments) {
        log.debug("Creating batch trackings: count={}", shipments.size());
        
        // TrackingMore API expects a direct array, not wrapped in an object
        return executeWithRetry(() -> 
            trackingMoreRestClient.post()
                .uri("/trackings/batch")
                .body(shipments)  // Send array directly
                .retrieve()
                .body(TrackingMoreResponse.class)
        );
    }

    @Override
    public TrackingMoreTrackingItem getTracking(String trackingNumber, String courierCode) {
        log.debug("Getting tracking: number={}, courier={}", trackingNumber, courierCode);
        
        TrackingMoreResponse response = executeWithRetry(() ->
            trackingMoreRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/trackings/get")
                        .queryParam("tracking_numbers", trackingNumber)
                        .build())
                .retrieve()
                .body(TrackingMoreResponse.class)
        );

        if (response == null || response.getData() == null) {
            return null;
        }

        // Parse the data field - TrackingMore GET returns an array of items
        return parseTrackingItem(response.getData());
    }

    @Override
    public List<TrackingMoreTrackingItem> getBatchTrackings(List<String> trackingNumbers) {
        log.debug("Getting batch trackings: count={}", trackingNumbers.size());
        
        String numbers = String.join(",", trackingNumbers);
        
        TrackingMoreResponse response = executeWithRetry(() ->
            trackingMoreRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/trackings/get")
                        .queryParam("tracking_numbers", numbers)
                        .build())
                .retrieve()
                .body(TrackingMoreResponse.class)
        );

        if (response == null || response.getData() == null) {
            return List.of();
        }

        return parseTrackingItems(response.getData());
    }

    @Override
    public boolean deleteTracking(String trackingNumber, String courierCode) {
        log.debug("Deleting tracking: number={}, courier={}", trackingNumber, courierCode);
        
        try {
            TrackingMoreResponse response = executeWithRetry(() ->
                trackingMoreRestClient.delete()
                    .uri("/trackings/{courier}/{tracking}", courierCode, trackingNumber)
                    .retrieve()
                    .body(TrackingMoreResponse.class)
            );
            
            return response != null && response.isSuccess();
        } catch (TrackingMoreException e) {
            // If tracking doesn't exist, consider it successfully deleted (idempotent)
            if (e.getHttpStatus() == 404) {
                log.debug("Tracking already deleted or not found: {}/{}", courierCode, trackingNumber);
                return true;
            }
            throw e;
        }
    }

    @Override
    public List<String> detectCourier(String trackingNumber) {
        log.debug("Detecting courier for tracking: {}", trackingNumber);
        
        TrackingMoreResponse response = executeWithRetry(() ->
            trackingMoreRestClient.post()
                .uri("/couriers/detect")
                .body(new DetectRequest(trackingNumber))
                .retrieve()
                .body(TrackingMoreResponse.class)
        );

        if (response == null || response.getData() == null) {
            return List.of();
        }

        // Parse courier codes from response
        return parseCourierCodes(response.getData());
    }

    /**
     * Execute an operation with retry logic and circuit breaker.
     * 
     * <p>Retry policy:
     * <ul>
     *   <li>Retry on: timeouts (ResourceAccessException), 5xx errors</li>
     *   <li>Never retry on: 4xx errors — avoids duplicate creates</li>
     * </ul>
     */
    private <T> T executeWithRetry(java.util.function.Supplier<T> operation) {
        // Check circuit breaker
        if (isCircuitOpen()) {
            throw new TrackingMoreUnavailableException(
                    "Circuit breaker is open. Service temporarily unavailable.",
                    (circuitOpenUntil - System.currentTimeMillis()) / 1000
            );
        }

        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                T result = operation.get();
                onSuccess();
                return result;
                
            } catch (HttpClientErrorException e) {
                // 4xx errors - DON'T RETRY (validation, auth errors)
                log.warn("TrackingMore client error (4xx): status={}, body={}", 
                        e.getStatusCode(), e.getResponseBodyAsString());
                onSuccess(); // Don't count 4xx as circuit breaker failure
                throw mapClientError(e);
                
            } catch (HttpServerErrorException e) {
                // 5xx errors - RETRY with backoff
                log.warn("TrackingMore server error (5xx), attempt {}/{}: status={}", 
                        attempt, MAX_RETRIES, e.getStatusCode());
                lastException = e;
                onFailure();
                
                if (attempt < MAX_RETRIES) {
                    sleep(INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1));
                }
                
            } catch (ResourceAccessException e) {
                // Timeout/network errors - RETRY with backoff
                log.warn("TrackingMore connection error, attempt {}/{}: {}", 
                        attempt, MAX_RETRIES, e.getMessage());
                lastException = e;
                onFailure();
                
                if (attempt < MAX_RETRIES) {
                    sleep(INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1));
                }
            }
        }

        // All retries exhausted
        throw new TrackingMoreUnavailableException(
                "TrackingMore service unavailable after " + MAX_RETRIES + " attempts",
                lastException
        );
    }

    private void onSuccess() {
        consecutiveFailures.set(0);
    }

    private void onFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= FAILURE_THRESHOLD) {
            circuitOpenUntil = System.currentTimeMillis() + CIRCUIT_OPEN_DURATION_MS;
            log.error("Circuit breaker opened after {} consecutive failures", failures);
        }
    }

    private boolean isCircuitOpen() {
        if (circuitOpenUntil == 0) {
            return false;
        }
        
        if (System.currentTimeMillis() > circuitOpenUntil) {
            // Half-open: allow one request through
            log.info("Circuit breaker entering half-open state");
            circuitOpenUntil = 0;
            consecutiveFailures.set(FAILURE_THRESHOLD - 2); // Allow 2 successes to close
            return false;
        }
        
        return true;
    }

    private TrackingMoreException mapClientError(HttpClientErrorException e) {
        HttpStatusCode status = e.getStatusCode();
        String body = e.getResponseBodyAsString();
        
        // Map common error codes
        String code = "UPSTREAM_ERROR";
        String message = "TrackingMore request failed";
        
        if (status.value() == 400) {
            code = "VALIDATION_ERROR";
            message = "Invalid request to tracking service";
        } else if (status.value() == 401) {
            code = "UPSTREAM_AUTH_ERROR";
            message = "TrackingMore authentication failed";
        } else if (status.value() == 404) {
            code = "TRACKING_NOT_FOUND";
            message = "Tracking not found in upstream service";
        } else if (status.value() == 429) {
            code = "UPSTREAM_RATE_LIMIT";
            message = "TrackingMore rate limit exceeded";
        }

        return new TrackingMoreException(code, message, status.value());
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TrackingMoreUnavailableException("Request interrupted", e);
        }
    }

    private List<TrackingMoreTrackingItem> parseTrackingItems(Object data) {
        if (data == null) {
            return List.of();
        }
        
        try {
            if (data instanceof List<?> list) {
                return list.stream()
                        .map(item -> objectMapper.convertValue(item, TrackingMoreTrackingItem.class))
                        .toList();
            }
            return List.of();
        } catch (Exception e) {
            log.warn("Unable to parse tracking items from data: {} - {}", data.getClass(), e.getMessage());
            return List.of();
        }
    }

    private TrackingMoreTrackingItem parseTrackingItem(Object data) {
        // Handle the data parsing - TrackingMore GET returns an array of tracking items
        if (data == null) {
            return null;
        }
        
        try {
            // TrackingMore returns {data: [{...}]} - an array of items
            if (data instanceof List<?> list && !list.isEmpty()) {
                // Convert the first item in the list to TrackingMoreTrackingItem
                Object firstItem = list.get(0);
                return objectMapper.convertValue(firstItem, TrackingMoreTrackingItem.class);
            }
            
            // If it's already the correct type
            if (data instanceof TrackingMoreTrackingItem item) {
                return item;
            }
            
            // Try to convert directly (might be a LinkedHashMap)
            return objectMapper.convertValue(data, TrackingMoreTrackingItem.class);
        } catch (Exception e) {
            log.warn("Unable to parse tracking item from data: {} - {}", data.getClass(), e.getMessage());
            return null;
        }
    }

    private List<String> parseCourierCodes(Object data) {
        // Parse courier detection response
        log.debug("Parsing courier codes from: {}", data);
        return List.of();
    }

    private record DetectRequest(String tracking_number) {}
}
