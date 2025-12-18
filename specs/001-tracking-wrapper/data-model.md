# Data Model: TrackingMore Wrapper API

**Feature**: 001-tracking-wrapper  
**Date**: 2025-12-18  
**Phase**: 1 (Design & Contracts)

## Entity Relationship Diagram

```
┌─────────────────┐           ┌──────────────────────┐
│     Client      │ 1      N  │      Tracking        │
├─────────────────┤───────────├──────────────────────┤
│ id (PK)         │           │ id (PK)              │
│ name            │           │ tracking_id (UK)     │
│ api_key_prefix  │           │ client_id (FK)       │
│ api_key_hash    │           │ tracking_number      │
│ plan            │           │ courier_code         │
│ created_at      │           │ trackingmore_id (UK) │
│ updated_at      │           │ origin_country       │
└─────────────────┘           │ destination_country  │
                              │ status               │
                              │ order_id             │
                              │ created_at           │
                              │ updated_at           │
                              │ deleted_at           │
                              └──────────────────────┘
```

## Entities

### 1. Client

**Purpose**: Represents an API consumer (e.g., e-commerce platform, logistics company) who uses the wrapper API to track shipments.

**Attributes**:
- `id` (BIGINT, PK, AUTO_INCREMENT): Internal database identifier
- `name` (VARCHAR(255), NOT NULL): Client's business name (e.g., "ACME E-Commerce")
- `api_key_prefix` (VARCHAR(16), NOT NULL): First 6-8 characters of API key for log correlation (e.g., "sk_live_ab12cd")
- `api_key_hash` (VARCHAR(60), UNIQUE, NOT NULL): bcrypt hash of full API key
- `plan` (VARCHAR(50), DEFAULT 'free'): Rate limit tier (e.g., "free", "starter", "pro")
- `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Account creation time
- `updated_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP): Last modification time

**Validation Rules**:
- `name` must be unique (business constraint, not enforced at DB level for MVP)
- `api_key_prefix` format: `^sk_(live|test)_[a-z0-9]{6,8}$`
- `api_key_hash` must be valid bcrypt hash (60 characters)
- `plan` must be one of: "free", "starter", "pro", "enterprise"

**Relationships**:
- One Client has many Trackings (1:N)

**Indexes**:
- Primary key on `id` (auto)
- Unique index on `api_key_hash` for authentication lookups
- Index on `api_key_prefix` for log correlation

---

### 2. Tracking

**Purpose**: Represents a shipment tracking record created by a client. Maps wrapper tracking IDs to TrackingMore tracking IDs and stores normalized status.

**Attributes**:
- `id` (BIGINT, PK, AUTO_INCREMENT): Internal database identifier
- `tracking_id` (VARCHAR(32), UNIQUE, NOT NULL): Wrapper-generated tracking ID (e.g., "trk_9f3a2b8c") exposed to clients
- `client_id` (BIGINT, NOT NULL, FK → clients.id): Owner of this tracking
- `tracking_number` (VARCHAR(255), NOT NULL): Original shipment tracking number (e.g., "JQ048740244IN")
- `courier_code` (VARCHAR(100), NOT NULL): Courier identifier (e.g., "india-post", "usps")
- `trackingmore_id` (VARCHAR(255), UNIQUE, NULLABLE): TrackingMore's internal ID (from their API response)
- `origin_country` (CHAR(2), NULLABLE): ISO 3166-1 alpha-2 origin country (e.g., "IN", "US")
- `destination_country` (CHAR(2), NULLABLE): ISO 3166-1 alpha-2 destination country
- `status` (VARCHAR(50), NULLABLE): Normalized wrapper status (enum: pending, not_found, in_transit, out_for_delivery, delivered, exception, expired, returned)
- `order_id` (VARCHAR(255), NULLABLE): Client's order identifier (business reference)
- `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): When tracking was created
- `updated_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP): Last status update
- `deleted_at` (TIMESTAMP, NULLABLE): Soft delete timestamp (null if active)

**Validation Rules**:
- `tracking_id` format: `^trk_[a-z0-9]{8,16}$` (URL-safe, collision-resistant)
- `tracking_number` must be at least 1 character
- `courier_code` format: `^[a-z0-9-]+$` (lowercase alphanumeric + hyphens)
- `origin_country`, `destination_country` must be valid ISO 3166-1 alpha-2 codes (if provided)
- `status` must be one of the enum values

**Relationships**:
- Many Trackings belong to one Client (N:1)

**Indexes**:
- Primary key on `id` (auto)
- Unique index on `tracking_id` for client-facing queries
- Unique index on `trackingmore_id` for upstream sync (nullable unique)
- Index on `client_id` for filtering by client
- Index on `tracking_number` for lookups
- Index on `status` for filtering queries
- Compound index on `(client_id, deleted_at)` for efficient client-scoped queries

**State Transitions**:
```
pending → not_found
        → in_transit → out_for_delivery → delivered
                    → exception
                    → expired
                    → returned
```

---

## Enums

### WrapperStatus
Normalized tracking statuses exposed to clients.

| Value | Description | TrackingMore Equivalents |
|-------|-------------|--------------------------|
| `pending` | Tracking created, no info yet | pending, inforeceived |
| `not_found` | Carrier has no record | notfound |
| `in_transit` | Package in transit | transit |
| `out_for_delivery` | Out for delivery | pickup |
| `delivered` | Successfully delivered | delivered |
| `exception` | Delivery problem | exception, undelivered |
| `expired` | Tracking expired | expired |
| `returned` | Returned to sender | (future) |

### RateLimitPlan
Client rate limit tiers.

| Plan | Requests/Day | Max Trackings/Batch | Cost |
|------|--------------|---------------------|------|
| `free` | 100 | 10 | $0 |
| `starter` | 1,000 | 40 | $29/month |
| `pro` | 10,000 | 40 | $99/month |
| `enterprise` | Unlimited | 40 | Custom |

---

## JPA Entity Examples

### Client.java
```java
@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "api_key_prefix", nullable = false, length = 16)
    @Pattern(regexp = "^sk_(live|test)_[a-z0-9]{6,8}$")
    private String apiKeyPrefix;
    
    @Column(name = "api_key_hash", nullable = false, unique = true, length = 60)
    private String apiKeyHash;
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RateLimitPlan plan = RateLimitPlan.FREE;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Tracking> trackings;
}
```

### Tracking.java
```java
@Entity
@Table(name = "trackings")
@Where(clause = "deleted_at IS NULL")  // Hibernate soft delete filter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tracking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tracking_id", nullable = false, unique = true, length = 32)
    @Pattern(regexp = "^trk_[a-z0-9]{8,16}$")
    private String trackingId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @Column(name = "tracking_number", nullable = false)
    @NotBlank
    private String trackingNumber;
    
    @Column(name = "courier_code", nullable = false, length = 100)
    @Pattern(regexp = "^[a-z0-9-]+$")
    private String courierCode;
    
    @Column(name = "trackingmore_id", unique = true)
    private String trackingmoreId;
    
    @Column(name = "origin_country", length = 2)
    @Size(min = 2, max = 2)
    private String originCountry;
    
    @Column(name = "destination_country", length = 2)
    @Size(min = 2, max = 2)
    private String destinationCountry;
    
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private WrapperStatus status;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
    
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
```

---

## Database Migrations (Flyway)

### V1__create_clients_table.sql
```sql
CREATE TABLE clients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    api_key_prefix VARCHAR(16) NOT NULL,
    api_key_hash VARCHAR(60) NOT NULL UNIQUE,
    plan VARCHAR(50) NOT NULL DEFAULT 'free',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_clients_api_key_prefix ON clients(api_key_prefix);
```

### V2__create_trackings_table.sql
```sql
CREATE TABLE trackings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tracking_id VARCHAR(32) NOT NULL UNIQUE,
    client_id BIGINT NOT NULL,
    tracking_number VARCHAR(255) NOT NULL,
    courier_code VARCHAR(100) NOT NULL,
    trackingmore_id VARCHAR(255) UNIQUE,
    origin_country CHAR(2),
    destination_country CHAR(2),
    status VARCHAR(50),
    order_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

CREATE INDEX idx_trackings_client_id ON trackings(client_id);
CREATE INDEX idx_trackings_tracking_number ON trackings(tracking_number);
CREATE INDEX idx_trackings_status ON trackings(status);
CREATE INDEX idx_trackings_client_deleted ON trackings(client_id, deleted_at);
```

---

## Data Constraints Summary

| Constraint | Enforcement |
|------------|-------------|
| API key uniqueness | Database UNIQUE constraint |
| Tracking ID uniqueness | Database UNIQUE constraint |
| Client ownership | Application-level (Service layer checks `client_id`) |
| Soft delete filtering | Hibernate `@Where` annotation |
| Status enum validation | Application-level (Service layer) |
| Country code format | Application-level (Bean Validation) |
| Cascade delete | Database FK with ON DELETE CASCADE |

---

## Future Extensions (Out of MVP Scope)

- **TrackingEvent** table: Store full event history (origin_info, destination_info from TrackingMore)
- **AuditLog** table: Track all API calls (endpoint, client_id, timestamp, response_code)
- **Webhook** table: Store client webhook URLs for status update notifications
- **Cache** table: Store TrackingMore responses with TTL for read optimization
