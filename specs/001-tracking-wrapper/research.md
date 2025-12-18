# Research: TrackingMore Wrapper API

**Feature**: 001-tracking-wrapper  
**Date**: 2025-12-18  
**Phase**: 0 (Outline & Research)

## Research Tasks

### 1. API Key Storage Strategy
**Decision**: Use Stripe-style API key storage (prefix + hash)

**Rationale**:
- **Security**: Never store plaintext API keys; use bcrypt to hash the full key
- **Observability**: Store first 6-8 characters as `api_key_prefix` for log correlation without exposing secrets
- **Industry Standard**: Stripe, GitHub, and other SaaS platforms use this pattern

**Implementation Details**:
- Database schema: `api_key_prefix VARCHAR(16)` + `api_key_hash VARCHAR(60)`
- Key format: `sk_live_<random>` or `sk_test_<random>` (16+ char random suffix)
- On generation: return full key to client once; never show again
- On authentication: hash incoming key, compare with stored `api_key_hash`

**Alternatives Considered**:
- ❌ Plaintext storage: Rejected due to security risk
- ❌ Encryption: Rejected because if encryption key is compromised, all keys are exposed; hashing is one-way

**References**:
- Stripe API key design: https://stripe.com/docs/keys
- OWASP Password Storage: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html

---

### 2. Rate Limiting Strategy
**Decision**: Use Bucket4j (Token Bucket algorithm) with in-memory store for MVP; Redis for production

**Rationale**:
- **Accuracy**: Token bucket prevents burst abuse while allowing short-term spikes
- **Spring Integration**: Bucket4j integrates seamlessly with Spring Boot
- **Scalability**: Redis backend enables distributed rate limiting across multiple app instances

**Implementation Details**:
- Store rate limit config per client in `clients` table (e.g., `requests_per_day`, `trackings_per_batch`)
- Use `@RateLimiter` annotation or Filter to check limits before Controller execution
- Return `429 Too Many Requests` with `Retry-After` header when limit exceeded

**Alternatives Considered**:
- ❌ Guava RateLimiter: Not distributed, doesn't work in multi-instance deployments
- ❌ Spring Cloud Gateway: Overkill for simple API; adds complexity

**References**:
- Bucket4j documentation: https://github.com/bucket4j/bucket4j
- Token Bucket algorithm: https://en.wikipedia.org/wiki/Token_bucket

---

### 3. HTTP Client for TrackingMore API
**Decision**: Use Spring Boot 3's RestClient (synchronous) for MVP

**Rationale**:
- **Simplicity**: RestClient is the new recommended HTTP client in Spring Boot 3.2+
- **Type Safety**: Strongly typed request/response handling with Jackson
- **Testability**: Easy to mock with `@MockBean`
- **Blocking is OK**: Our API is I/O-bound anyway; async complexity not needed for MVP

**Implementation Details**:
```java
@Bean
public RestClient trackingMoreRestClient(RestClient.Builder builder) {
    return builder
        .baseUrl("https://api.trackingmore.com/v4")
        .defaultHeader("Tracking-Api-Key", trackingMoreApiKey)
        .defaultHeader("Content-Type", "application/json")
        .build();
}
```

**Alternatives Considered**:
- ❌ WebClient (reactive): Adds complexity without clear benefit for our use case
- ❌ RestTemplate: Deprecated in Spring Boot 3+
- ❌ OkHttp / Apache HttpClient: Less Spring-idiomatic

**References**:
- RestClient announcement: https://spring.io/blog/2023/07/13/new-in-spring-6-1-restclient
- Spring Boot 3 HTTP Clients: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html

---

### 4. Database Migration Strategy
**Decision**: Use Flyway for schema versioning

**Rationale**:
- **Version Control**: SQL migrations tracked in Git (e.g., `V1__initial_schema.sql`)
- **Repeatability**: Consistent schema across dev/staging/prod environments
- **Spring Integration**: Auto-detects and runs migrations on startup
- **Simplicity**: SQL-based migrations are readable and auditable

**Implementation Details**:
- Place migrations in `src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql` (e.g., `V1__create_clients_table.sql`)
- Flyway creates `flyway_schema_history` table to track applied migrations

**Alternatives Considered**:
- ❌ Liquibase: More complex; XML/YAML definitions less readable than raw SQL
- ❌ JPA `ddl-auto=update`: Not safe for production; can't track changes

**References**:
- Flyway documentation: https://flywaydb.org/documentation/
- Spring Boot Flyway integration: https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway

---

### 5. Correlation ID Propagation
**Decision**: Use Logback MDC (Mapped Diagnostic Context) + Spring Filter

**Rationale**:
- **Traceability**: Every log entry includes correlation ID for request tracing
- **Client Support**: Accept `X-Request-Id` from clients; auto-generate if missing
- **Standard Practice**: Used by AWS, Google Cloud, and other cloud platforms

**Implementation Details**:
```java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String correlationId = request.getHeader("X-Request-Id");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
```

Logback pattern: `[%X{correlationId}] %msg%n`

**Alternatives Considered**:
- ❌ Spring Cloud Sleuth: Deprecated in favor of Micrometer Tracing (overkill for MVP)
- ❌ Manual parameter passing: Error-prone and verbose

**References**:
- MDC documentation: http://logback.qos.ch/manual/mdc.html
- Correlation ID best practices: https://www.baeldung.com/spring-boot-correlation-id

---

### 6. Error Handling & Exception Mapping
**Decision**: Use `@ControllerAdvice` with custom exception hierarchy

**Rationale**:
- **Centralized**: Single place to handle all exceptions across controllers
- **Consistent**: All errors return structured JSON (code, message, details)
- **Type Safety**: Custom exceptions (e.g., `AuthenticationException`) map to specific HTTP codes

**Implementation Details**:
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(401)
            .body(new ErrorResponse("UNAUTHORIZED", ex.getMessage()));
    }
    
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException ex) {
        return ResponseEntity.status(429)
            .header("Retry-After", "3600")
            .body(new ErrorResponse("RATE_LIMIT_EXCEEDED", ex.getMessage()));
    }
    
    // Handle TrackingMore upstream errors
    @ExceptionHandler(TrackingMoreException.class)
    public ResponseEntity<ErrorResponse> handleUpstream(TrackingMoreException ex) {
        // Map TrackingMore error codes to wrapper codes
        return ResponseEntity.status(ex.getHttpStatus())
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }
}
```

**Alternatives Considered**:
- ❌ Exception handling in each controller: Duplicated code, inconsistent responses
- ❌ Spring's default error handler: Returns HTML, not JSON

**References**:
- Spring @ControllerAdvice: https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc
- REST API error design: https://www.baeldung.com/rest-api-error-handling-best-practices

---

### 7. Status Normalization Mapping
**Decision**: Use enum-based mapper with explicit TrackingMore → Wrapper mappings

**Rationale**:
- **Type Safety**: Java enums prevent invalid status values
- **Clarity**: Explicit mapping table (see spec.md SC-002) documents transformations
- **Extensibility**: Easy to add new statuses without breaking existing code

**Implementation Details**:
```java
public enum WrapperStatus {
    PENDING, NOT_FOUND, IN_TRANSIT, OUT_FOR_DELIVERY, 
    DELIVERED, EXCEPTION, EXPIRED, RETURNED;
}

@Component
public class StatusMapper {
    public WrapperStatus map(String trackingMoreStatus) {
        return switch (trackingMoreStatus.toLowerCase()) {
            case "pending", "inforeceived" -> WrapperStatus.PENDING;
            case "notfound" -> WrapperStatus.NOT_FOUND;
            case "transit" -> WrapperStatus.IN_TRANSIT;
            case "pickup" -> WrapperStatus.OUT_FOR_DELIVERY;
            case "delivered" -> WrapperStatus.DELIVERED;
            case "expired" -> WrapperStatus.EXPIRED;
            case "undelivered", "exception" -> WrapperStatus.EXCEPTION;
            default -> {
                log.warn("Unknown TrackingMore status: {}", trackingMoreStatus);
                yield WrapperStatus.PENDING; // Safe fallback
            }
        };
    }
}
```

**Alternatives Considered**:
- ❌ String constants: No compile-time safety
- ❌ Direct passthrough: Leaks TrackingMore semantics to clients

---

### 8. Soft Delete Implementation
**Decision**: Use `deleted_at TIMESTAMP` column with `@Where` annotation

**Rationale**:
- **Audit Trail**: Deleted trackings remain in database for compliance/debugging
- **JPA Integration**: Hibernate `@Where` clause automatically filters soft-deleted records
- **Skip Upstream Deletion**: Don't call TrackingMore delete API if tracking is already delivered/expired (avoids errors)

**Implementation Details**:
```java
@Entity
@Table(name = "trackings")
@Where(clause = "deleted_at IS NULL")
public class Tracking {
    // ...
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
```

Service logic:
```java
public void deleteTracking(String trackingId, Long clientId) {
    Tracking tracking = findByIdAndClientId(trackingId, clientId);
    
    // Skip upstream delete if final state
    if (!tracking.getStatus().isFinalState()) {
        trackingMoreClient.delete(tracking.getTrackingmoreId());
    }
    
    tracking.softDelete();
    trackingRepository.save(tracking);
}
```

**Alternatives Considered**:
- ❌ Hard delete: Loses audit trail, can't recover from mistakes
- ❌ `is_deleted` boolean: Less informative than timestamp

**References**:
- Soft delete patterns: https://www.baeldung.com/spring-jpa-soft-delete

---

## Summary of Decisions

| Area | Technology/Pattern | Phase |
|------|-------------------|-------|
| API Key Storage | Stripe-style (prefix + bcrypt hash) | MVP |
| Rate Limiting | Bucket4j + in-memory (Redis later) | MVP |
| HTTP Client | Spring RestClient | MVP |
| Database Migrations | Flyway | MVP |
| Correlation IDs | Logback MDC + Filter | MVP |
| Error Handling | @ControllerAdvice + custom exceptions | MVP |
| Status Mapping | Enum-based mapper | MVP |
| Soft Delete | `deleted_at` timestamp + @Where | MVP |
| Database (dev) | H2 in-memory | MVP |
| Database (prod) | PostgreSQL | MVP |
| Documentation | SpringDoc OpenAPI (Swagger) | MVP |
| Testing | JUnit 5 + Mockito + @WebMvcTest | MVP |

## Open Items for Phase 1

None - all research complete. Ready to proceed to Phase 1 (data modeling and contracts).
