package com.mailit.wrapper.exception;

import com.mailit.wrapper.model.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler mapping exceptions to structured JSON responses.
 * 
 * <p>All errors are returned with consistent format including correlation ID
 * for support tracing.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CORRELATION_ID_KEY = "correlationId";

    /**
     * Handle authentication failures.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                getCorrelationId()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle rate limit exceeded.
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitException(RateLimitException ex) {
        log.warn("Rate limit exceeded: limit={}, resetAt={}", ex.getLimit(), ex.getResetAt());
        
        ErrorResponse error = ErrorResponse.rateLimitError(
                ex.getLimit(),
                ex.getRemaining(),
                ex.getResetAt(),
                getCorrelationId()
        );
        
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(error);
    }

    /**
     * Handle tracking not found.
     */
    @ExceptionHandler(TrackingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTrackingNotFoundException(TrackingNotFoundException ex) {
        log.debug("Tracking not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                getCorrelationId()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle forbidden access.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex) {
        log.warn("Forbidden access: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                getCorrelationId()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle TrackingMore client errors (4xx from upstream).
     */
    @ExceptionHandler(TrackingMoreException.class)
    public ResponseEntity<ErrorResponse> handleTrackingMoreException(TrackingMoreException ex) {
        log.error("TrackingMore error: code={}, message={}", ex.getUpstreamCode(), ex.getUpstreamMessage());
        
        ErrorResponse error = new ErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                getCorrelationId()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    /**
     * Handle TrackingMore service unavailable (5xx, timeout, network).
     */
    @ExceptionHandler(TrackingMoreUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleTrackingMoreUnavailableException(TrackingMoreUnavailableException ex) {
        log.error("TrackingMore unavailable: {}", ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                getCorrelationId(),
                new RetryDetails(ex.getRetryAfterSeconds())
        );
        
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(error);
    }

    /**
     * Handle Bean Validation errors (from @Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());
        
        log.debug("Validation failed: {}", errors);
        
        ErrorResponse error = ErrorResponse.validationError(errors, getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle constraint violations (from @Validated).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toList());
        
        log.debug("Constraint violation: {}", errors);
        
        ErrorResponse error = ErrorResponse.validationError(errors, getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(MissingServletRequestParameterException ex) {
        log.debug("Missing parameter: {}", ex.getParameterName());
        
        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Missing required parameter: " + ex.getParameterName(),
                getCorrelationId()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle type mismatch errors.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.debug("Type mismatch for parameter {}: {}", ex.getName(), ex.getValue());
        
        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue(),
                getCorrelationId()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle malformed JSON.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.debug("Malformed request body: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Malformed request body. Please check JSON syntax.",
                getCorrelationId()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle all other exceptions (fallback).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please contact support with correlation ID.",
                getCorrelationId()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String getCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        return correlationId != null ? correlationId : "unknown";
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private record RetryDetails(long retryAfter) {}
}
