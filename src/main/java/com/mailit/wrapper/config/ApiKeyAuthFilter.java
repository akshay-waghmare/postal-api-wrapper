package com.mailit.wrapper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailit.wrapper.model.dto.response.ErrorResponse;
import com.mailit.wrapper.model.entity.Client;
import com.mailit.wrapper.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter that authenticates API requests via X-API-Key header.
 * 
 * <p>Validates the API key and stores the authenticated client
 * in the request attributes for downstream use.</p>
 * 
 * <p>This is a lightweight alternative to full Spring Security,
 * suitable for simple API key authentication.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Run after CorrelationIdFilter
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String CLIENT_ATTRIBUTE = "authenticatedClient";
    public static final String API_KEY_PREFIX_MDC_KEY = "apiKeyPrefix";

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String apiKey = request.getHeader(API_KEY_HEADER);
        
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Missing API key in request to {}", request.getRequestURI());
            sendUnauthorized(response, "Missing API key. Include X-API-Key header.");
            return;
        }
        
        Optional<Client> clientOpt = authService.validateApiKey(apiKey);
        
        if (clientOpt.isEmpty()) {
            log.warn("Invalid API key attempt: prefix={}", extractPrefix(apiKey));
            sendUnauthorized(response, "Invalid API key.");
            return;
        }
        
        Client client = clientOpt.get();
        
        // Store client for downstream use
        request.setAttribute(CLIENT_ATTRIBUTE, client);
        
        // Add API key prefix to MDC for log correlation
        MDC.put(API_KEY_PREFIX_MDC_KEY, client.getApiKeyPrefix());
        
        log.debug("Authenticated client: id={}, name={}, plan={}", 
                client.getId(), client.getName(), client.getPlan());
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(API_KEY_PREFIX_MDC_KEY);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip authentication for these paths
        return path.startsWith("/actuator")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/api-docs")
            || path.startsWith("/h2-console")
            || path.equals("/")
            || path.equals("/health");
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        ErrorResponse error = new ErrorResponse("UNAUTHORIZED", message, correlationId);
        
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String extractPrefix(String apiKey) {
        if (apiKey == null || apiKey.length() < 14) {
            return "invalid";
        }
        // Return first 14 chars (e.g., "sk_live_ab12cd")
        return apiKey.substring(0, Math.min(14, apiKey.length()));
    }
}
