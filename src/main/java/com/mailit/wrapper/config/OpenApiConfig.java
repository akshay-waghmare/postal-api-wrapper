package com.mailit.wrapper.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the MailIt Wrapper API.
 * 
 * <p>Configures API documentation including security schemes,
 * server information, and API metadata.</p>
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    private static final String API_KEY_SCHEME_NAME = "ApiKeyAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server()
                                .url("https://api.mailit.com")
                                .description("Production server")
                ))
                .components(new Components()
                        .addSecuritySchemes(API_KEY_SCHEME_NAME, apiKeySecurityScheme()))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME_NAME));
    }

    private Info apiInfo() {
        return new Info()
                .title("MailIt Tracking API")
                .description("""
                        Multi-tenant tracking platform wrapping TrackingMore API.
                        
                        ## Features
                        - Batch tracking creation (up to 40 per request)
                        - Query trackings with filters and pagination
                        - Normalized status responses
                        - Rate limiting per client plan
                        
                        ## Authentication
                        All endpoints require an API key in the `X-API-Key` header.
                        
                        ## Rate Limits
                        | Plan | Requests/Day | Trackings/Batch |
                        |------|--------------|-----------------|
                        | Free | 100 | 10 |
                        | Starter | 1,000 | 40 |
                        | Pro | 10,000 | 40 |
                        | Enterprise | Unlimited | 40 |
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("MailIt Support")
                        .email("support@mailit.com")
                        .url("https://mailit.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://mailit.com/terms"));
    }

    private SecurityScheme apiKeySecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("API key for authentication. Format: `sk_live_xxxxx` or `sk_test_xxxxx`");
    }
}
