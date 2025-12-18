package com.mailit.wrapper.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration for TrackingMore API client.
 * 
 * <p>Creates a pre-configured RestClient with:
 * <ul>
 *   <li>Base URL and authentication header</li>
 *   <li>Timeout settings</li>
 *   <li>Correlation ID propagation</li>
 *   <li>Request/response logging</li>
 * </ul>
 */
@Slf4j
@Configuration
public class TrackingMoreConfig {

    @Value("${trackingmore.api.base-url:https://api.trackingmore.com/v4}")
    private String baseUrl;

    @Value("${trackingmore.api.key:}")
    private String apiKey;

    @Value("${trackingmore.api.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${trackingmore.api.timeout.read:30000}")
    private int readTimeout;

    @Bean
    public RestClient trackingMoreRestClient(RestClient.Builder builder) {
        log.info("Configuring TrackingMore client: baseUrl={}, connectTimeout={}ms, readTimeout={}ms",
                baseUrl, connectTimeout, readTimeout);

        return builder
                .baseUrl(baseUrl)
                .defaultHeader("Tracking-Api-Key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .requestInterceptor(correlationIdInterceptor())
                .requestInterceptor(loggingInterceptor())
                .build();
    }

    /**
     * Interceptor that propagates correlation ID to upstream requests.
     */
    private ClientHttpRequestInterceptor correlationIdInterceptor() {
        return (request, body, execution) -> {
            String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
            if (correlationId != null) {
                request.getHeaders().add("X-Request-Id", correlationId);
            }
            return execution.execute(request, body);
        };
    }

    /**
     * Interceptor for logging requests and responses.
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            long startTime = System.currentTimeMillis();
            
            log.debug("TrackingMore request: {} {}", request.getMethod(), request.getURI());
            
            var response = execution.execute(request, body);
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("TrackingMore response: status={}, duration={}ms", 
                    response.getStatusCode(), duration);
            
            return response;
        };
    }
}
