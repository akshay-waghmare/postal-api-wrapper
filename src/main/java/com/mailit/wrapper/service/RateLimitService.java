package com.mailit.wrapper.service;

import com.mailit.wrapper.exception.RateLimitException;
import com.mailit.wrapper.model.RateLimitPlan;
import com.mailit.wrapper.model.entity.Client;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for rate limiting API requests.
 * 
 * <p>Uses Bucket4j with Token Bucket algorithm. Currently uses in-memory
 * storage suitable for single-instance deployments.</p>
 * 
 * <p>Note: Redis-backed implementation can be added later without changing
 * this interface. Simply create a new implementation class and configure
 * via Spring profiles.</p>
 */
@Slf4j
@Service
public class RateLimitService {

    /**
     * Map of client ID to their rate limit bucket.
     * Buckets are created lazily on first request.
     */
    private final Map<Long, Bucket> clientBuckets = new ConcurrentHashMap<>();

    /**
     * Check if a client can make a request and consume a token.
     * 
     * @param client the authenticated client
     * @throws RateLimitException if rate limit exceeded
     */
    public void checkRateLimit(Client client) {
        if (client.getPlan().isUnlimited()) {
            log.trace("Client {} has unlimited plan, skipping rate limit", client.getId());
            return;
        }

        Bucket bucket = clientBuckets.computeIfAbsent(
                client.getId(),
                id -> createBucket(client.getPlan())
        );

        if (!bucket.tryConsume(1)) {
            Instant resetAt = getNextDayStart();
            log.info("Rate limit exceeded for client: id={}, plan={}", 
                    client.getId(), client.getPlan());
            throw new RateLimitException(
                    client.getPlan().getRequestsPerDay(),
                    resetAt
            );
        }

        log.trace("Rate limit check passed for client: id={}, remaining={}",
                client.getId(), bucket.getAvailableTokens());
    }

    /**
     * Check if a batch size is within the client's plan limit.
     * 
     * @param client the authenticated client
     * @param batchSize the number of items in the batch
     * @throws RateLimitException if batch size exceeds plan limit
     */
    public void checkBatchSize(Client client, int batchSize) {
        int maxBatchSize = client.getPlan().getTrackingsPerBatch();
        
        if (batchSize > maxBatchSize) {
            log.info("Batch size {} exceeds limit {} for client: id={}, plan={}",
                    batchSize, maxBatchSize, client.getId(), client.getPlan());
            throw new RateLimitException(
                    "Batch size " + batchSize + " exceeds your plan limit of " + maxBatchSize,
                    maxBatchSize,
                    Instant.now()
            );
        }
    }

    /**
     * Get remaining requests for a client.
     * 
     * @param client the client
     * @return remaining requests, or -1 if unlimited
     */
    public long getRemainingRequests(Client client) {
        if (client.getPlan().isUnlimited()) {
            return -1;
        }

        Bucket bucket = clientBuckets.get(client.getId());
        if (bucket == null) {
            return client.getPlan().getRequestsPerDay();
        }

        return bucket.getAvailableTokens();
    }

    /**
     * Reset rate limits (typically called at midnight or for testing).
     */
    public void resetLimits() {
        log.info("Resetting all rate limit buckets");
        clientBuckets.clear();
    }

    /**
     * Reset rate limit for a specific client.
     * 
     * @param clientId the client ID
     */
    public void resetLimitForClient(Long clientId) {
        log.info("Resetting rate limit for client: {}", clientId);
        clientBuckets.remove(clientId);
    }

    private Bucket createBucket(RateLimitPlan plan) {
        // Daily limit that refills at midnight
        Bandwidth limit = Bandwidth.classic(
                plan.getRequestsPerDay(),
                Refill.intervally(plan.getRequestsPerDay(), Duration.ofDays(1))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Instant getNextDayStart() {
        return LocalDate.now(ZoneOffset.UTC)
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }
}
