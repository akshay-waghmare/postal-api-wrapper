package com.mailit.wrapper.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that propagates correlation IDs for request tracing.
 * 
 * <p>Flow:</p>
 * <ol>
 *   <li>Accept {@code X-Request-Id} header if provided by client</li>
 *   <li>Generate UUID if not provided</li>
 *   <li>Add to Logback MDC for all log entries</li>
 *   <li>Echo back in response header</li>
 * </ol>
 * 
 * <p>This filter runs first (highest precedence) to ensure correlation ID
 * is available for all subsequent processing including auth filter.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.trace("Generated correlation ID: {}", correlationId);
        } else {
            log.trace("Using client-provided correlation ID: {}", correlationId);
        }
        
        // Add to MDC for logging
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        
        // Echo back to client
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC to prevent leaks in thread pools
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip for static resources and actuator endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator") 
            || path.startsWith("/swagger-ui") 
            || path.startsWith("/api-docs")
            || path.startsWith("/h2-console");
    }
}
