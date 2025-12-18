package com.mailit.wrapper.exception;

import java.time.Instant;

/**
 * Exception thrown when client exceeds their rate limit.
 */
public class RateLimitException extends WrapperException {
    
    private static final String CODE = "RATE_LIMIT_EXCEEDED";
    private static final int HTTP_STATUS = 429;

    private final int limit;
    private final int remaining;
    private final Instant resetAt;
    private final long retryAfterSeconds;

    public RateLimitException(int limit, Instant resetAt) {
        super(CODE, "Request quota exceeded. Upgrade your plan or retry later.", HTTP_STATUS);
        this.limit = limit;
        this.remaining = 0;
        this.resetAt = resetAt;
        this.retryAfterSeconds = Math.max(0, resetAt.getEpochSecond() - Instant.now().getEpochSecond());
    }

    public RateLimitException(String message, int limit, Instant resetAt) {
        super(CODE, message, HTTP_STATUS);
        this.limit = limit;
        this.remaining = 0;
        this.resetAt = resetAt;
        this.retryAfterSeconds = Math.max(0, resetAt.getEpochSecond() - Instant.now().getEpochSecond());
    }

    public int getLimit() {
        return limit;
    }

    public int getRemaining() {
        return remaining;
    }

    public Instant getResetAt() {
        return resetAt;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
