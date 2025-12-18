package com.mailit.wrapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the MailIt Wrapper API.
 * 
 * <p>This Spring Boot application provides a wrapper around the TrackingMore API,
 * offering multi-tenant tracking services with rate limiting, authentication,
 * and normalized response schemas.</p>
 * 
 * @author MailIt Team
 * @version 1.0.0
 */
@SpringBootApplication
public class MailItWrapperApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailItWrapperApplication.class, args);
    }

}
