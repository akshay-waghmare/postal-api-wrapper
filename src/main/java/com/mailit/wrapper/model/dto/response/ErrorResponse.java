package com.mailit.wrapper.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Structured error response returned by the API.
 * 
 * <p>All errors follow this consistent format for client-side handling.</p>
 * 
 * @param code machine-readable error code (e.g., "VALIDATION_ERROR")
 * @param message human-readable error description
 * @param correlationId request correlation ID for support tracing
 * @param details optional additional error details
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        String correlationId,
        Object details
) {
    /**
     * Create an error response without details.
     */
    public ErrorResponse(String code, String message) {
        this(code, message, null, null);
    }

    /**
     * Create an error response with correlation ID.
     */
    public ErrorResponse(String code, String message, String correlationId) {
        this(code, message, correlationId, null);
    }

    /**
     * Create an error response with validation details.
     */
    public static ErrorResponse validationError(List<String> errors, String correlationId) {
        return new ErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed",
                correlationId,
                errors
        );
    }

    /**
     * Create a rate limit error response with details.
     */
    public static ErrorResponse rateLimitError(int limit, int remaining, Instant resetAt, String correlationId) {
        return new ErrorResponse(
                "RATE_LIMIT_EXCEEDED",
                "Request quota exceeded. Upgrade your plan or retry later.",
                correlationId,
                new RateLimitDetails(limit, remaining, resetAt)
        );
    }

    /**
     * Rate limit error details.
     */
    public record RateLimitDetails(int limit, int remaining, Instant resetAt) {}
}
