package com.mailit.wrapper.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configuration for filter registration and ordering.
 * 
 * <p>Ensures filters are applied in correct order:
 * <ol>
 *   <li>CorrelationIdFilter - Sets up request tracing</li>
 *   <li>ApiKeyAuthFilter - Authenticates requests</li>
 * </ol>
 */
@Configuration
public class FilterConfig {

    /**
     * Register CorrelationIdFilter with highest priority.
     * This ensures correlation ID is available for all subsequent processing.
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(
            CorrelationIdFilter filter) {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("correlationIdFilter");
        return registration;
    }

    /**
     * Register ApiKeyAuthFilter after CorrelationIdFilter.
     */
    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilterRegistration(
            ApiKeyAuthFilter filter) {
        FilterRegistrationBean<ApiKeyAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setName("apiKeyAuthFilter");
        return registration;
    }
}
