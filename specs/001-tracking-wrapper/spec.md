# Feature Specification: TrackingMore Wrapper API

**Feature Branch**: `001-tracking-wrapper`  
**Created**: 2025-12-18  
**Status**: Draft  
**Input**: Create a Spring Boot wrapper API around TrackingMore API that abstracts the upstream service, provides client authentication, enables vendor switching, and supports monetization.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client Creates Shipment Trackings (Priority: P1)

As an API client (e-commerce platform), I want to register shipment tracking numbers for monitoring so that my customers can track their packages without me integrating directly with TrackingMore.

**Why this priority**: This is the core value proposition - clients must be able to create trackings to get any value from the service. Without this, the entire system is non-functional.

**Independent Test**: Can be fully tested by calling POST `/api/v1/trackings` with valid shipment data and verifying the response contains tracking IDs and success confirmation.

**Acceptance Scenarios**:

1. **Given** I have a valid API key and shipment data, **When** I POST to `/api/v1/trackings` with up to 40 tracking numbers, **Then** I receive a success response with tracking IDs for each shipment.
2. **Given** I submit a batch with one invalid tracking number, **When** the API processes the request, **Then** valid trackings are created and invalid ones are returned in the `failed` array with error reasons. **A single failed shipment must not block other valid shipments in the same batch.**
3. **Given** I submit more than 40 tracking numbers in one request, **When** the API validates the input, **Then** I receive a 400 Bad Request error before any upstream call is made.

---

### User Story 2 - Client Queries Tracking Status (Priority: P1)

As an API client, I want to retrieve current tracking status for shipments so that I can display real-time updates to my customers.

**Why this priority**: Querying status is equally critical - clients need to read tracking data to provide value to their end users. This completes the essential read/write cycle.

**Independent Test**: Can be tested by calling GET `/api/v1/trackings?trackingNumbers=ABC123` and verifying the normalized response contains status, events, and metadata.

**Acceptance Scenarios**:

1. **Given** I have created trackings, **When** I query by tracking numbers, **Then** I receive normalized tracking data with current status, last event, and timestamps.
2. **Given** I query with pagination parameters (page=1, limit=50), **When** the API fetches results, **Then** I receive up to 50 results and pagination metadata.
3. **Given** I filter by delivery status (e.g., `status=delivered`), **When** the API processes the query, **Then** only trackings matching that status are returned.
4. **Given** I query a non-existent tracking number, **When** the API looks it up, **Then** I receive an empty result set (not an error).

---

### User Story 3 - Client Retrieves Single Tracking Details (Priority: P2)

As an API client, I want to fetch detailed information for a specific tracking ID so that I can display comprehensive package journey information.

**Why this priority**: This provides richer detail for individual shipments but is not essential for MVP - clients can use the batch query endpoint filtered to one tracking number.

**Independent Test**: Can be tested by calling GET `/api/v1/trackings/{trackingId}` and verifying full tracking history is returned.

**Acceptance Scenarios**:

1. **Given** I have a valid tracking ID, **When** I GET `/api/v1/trackings/{trackingId}`, **Then** I receive complete tracking details including all events, origin/destination, and timestamps.
2. **Given** I request a tracking ID that doesn't belong to my client account, **When** the API checks ownership, **Then** I receive a 404 Not Found error.
3. **Given** I request a non-existent tracking ID, **When** the API processes the request, **Then** I receive a 404 Not Found error.

---

### User Story 4 - Client Deletes Tracking (Priority: P3)

As an API client, I want to delete trackings I no longer need to monitor so that I can manage my tracking quota and clean up completed shipments.

**Why this priority**: Useful for cleanup but not essential for core functionality. Clients can simply stop querying trackings they don't care about.

**Independent Test**: Can be tested by calling DELETE `/api/v1/trackings/{trackingId}` and verifying the tracking is marked as deleted and no longer appears in queries.

**Acceptance Scenarios**:

1. **Given** I have a valid tracking ID, **When** I DELETE `/api/v1/trackings/{trackingId}`, **Then** the tracking is soft-deleted locally and I receive a confirmation response. **Note**: Upstream TrackingMore deletion is skipped if the tracking is already `delivered` or `expired` to avoid unnecessary API calls and errors.
2. **Given** I delete a tracking, **When** I subsequently query for it, **Then** it does not appear in results.
3. **Given** I attempt to delete a tracking belonging to another client, **When** the API checks ownership, **Then** I receive a 403 Forbidden error.

---

### User Story 5 - Administrator Manages Client API Keys (Priority: P2)

As a system administrator, I want to generate and manage API keys for clients so that I can onboard new customers and control access.

**Why this priority**: Required for multi-tenant operation but can be manually managed initially (e.g., directly in database) for MVP.

**Independent Test**: Can be tested by creating a new client record with an API key and verifying that key authenticates successfully.

**Acceptance Scenarios**:

1. **Given** I create a new client record, **When** I generate an API key, **Then** the client can authenticate using that key in the `X-API-Key` header.
2. **Given** I revoke a client's API key, **When** they attempt to use it, **Then** they receive a 401 Unauthorized error.
3. **Given** I assign a rate limit plan to a client, **When** they exceed their quota, **Then** subsequent requests return 429 Too Many Requests.

---

### Edge Cases

- **What happens when TrackingMore API is unavailable?** 
  - For **POST** requests: Return 503 Service Unavailable with a clear error message, log the incident, and implement retry logic with exponential backoff (up to 3 attempts).
  - For **GET** requests: If cached data exists (even if stale), optionally return 200 OK with `meta.warning: "Upstream unavailable, data may be stale"` and the cached tracking data. This prevents total service disruption.
- **What happens when a client's API key is revoked mid-request?** The request should complete but subsequent requests with that key should fail with 401.
- **How does the system handle concurrent requests for the same tracking number from different clients?** Each client maintains their own tracking record (client-scoped isolation).
- **What happens if TrackingMore changes their response schema?** The wrapper's response mapper should gracefully handle unknown fields (log warnings but don't fail).
- **What happens if a tracking number is valid but the courier code is wrong?** The system should attempt auto-correction if enabled, or return a validation error in the `failed` array.
- **How are rate limits enforced across multiple API servers?** Use a distributed rate limiter (e.g., Redis-backed) to ensure consistency.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST accept batch tracking creation requests with up to 40 tracking numbers per request.
- **FR-002**: System MUST validate all input data (tracking numbers, courier codes, country codes) before forwarding to TrackingMore.
- **FR-003**: System MUST normalize TrackingMore API responses into a consistent, simplified JSON schema for clients.
- **FR-004**: System MUST persist tracking records with mappings between wrapper tracking IDs and TrackingMore tracking IDs.
- **FR-005**: System MUST authenticate all client requests via API key in the `X-API-Key` header.
- **FR-006**: System MUST enforce rate limits per client based on their assigned plan (e.g., 100 requests/day, max 40 trackings per batch).
- **FR-007**: System MUST log all API requests, responses, and errors for auditing and debugging.
- **FR-008**: System MUST map TrackingMore errors to appropriate HTTP status codes and structured error responses.
- **FR-009**: System MUST support querying trackings by tracking numbers, delivery status, date ranges, and pagination.
- **FR-010**: System MUST implement ownership validation - clients can only access their own tracking records.
- **FR-011**: System MUST support soft deletion of tracking records (mark as deleted, don't purge).
- **FR-012**: System MUST store the TrackingMore API key securely (externalized configuration, not hardcoded).
- **FR-013**: System MUST generate unique wrapper tracking IDs (e.g., `trk_9f3a2`) that are URL-safe and collision-resistant.
- **FR-014**: System MUST support ISO 3166-1 alpha-2 country codes for origin and destination.
- **FR-015**: System MUST validate courier codes against a known list (ideally fetched from TrackingMore's courier list API).

### Key Entities

- **Client**: Represents an API consumer with an API key, rate limit plan, and billing information. Key attributes: `client_id`, `name`, `api_key`, `plan`, `created_at`.
- **Tracking**: Represents a shipment tracking record. Key attributes: `id` (wrapper tracking ID), `client_id`, `tracking_number`, `courier_code`, `trackingmore_id`, `origin_country`, `destination_country`, `status`, `created_at`, `updated_at`, `deleted_at`.
- **TrackingEvent** (optional): Represents individual events in a shipment's journey. Key attributes: `tracking_id`, `event_description`, `location`, `timestamp`. Relationship: Many events belong to one tracking.
- **RateLimitPlan**: Defines rate limit tiers. Key attributes: `plan_name`, `requests_per_day`, `max_trackings_per_batch`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Clients can successfully create up to 40 trackings in a single API call with <2 second response time (p95).
- **SC-002**: The wrapper successfully normalizes 100% of TrackingMore tracking statuses into the 8 standard statuses using the following mapping:

  | TrackingMore Status | Wrapper Status       |
  |---------------------|----------------------|
  | pending             | pending              |
  | notfound            | not_found            |
  | transit             | in_transit           |
  | pickup              | out_for_delivery     |
  | delivered           | delivered            |
  | expired             | expired              |
  | undelivered         | exception            |
  | exception           | exception            |
  | inforeceived        | pending              |

- **SC-003**: 99.5% of authenticated requests return correct responses (success or properly formatted errors).
- **SC-004**: All client requests and upstream TrackingMore calls are logged with correlation IDs for traceability.
- **SC-005**: Rate limiting enforcement has 99.9% accuracy (no false positives or negatives).
- **SC-006**: System can handle 100 concurrent clients making requests without performance degradation.
- **SC-007**: Clients never see raw TrackingMore error responses - all errors are normalized to the wrapper's error schema.

## Architecture Overview

### High-Level Flow

```
Client Application
       |
       | (X-API-Key: client_xxx)
       v
+-----------------------+
| Wrapper API           |
| (Spring Boot)         |
|                       |
| - Authentication      |
| - Rate Limiting       |
| - Validation          |
| - Mapping/Normalize   |
| - Persistence         |
+-----------------------+
       |
       | (Tracking-Api-Key: <internal>)
       v
TrackingMore API
```

### Database Schema

**clients**
```
id              BIGINT PRIMARY KEY AUTO_INCREMENT
name            VARCHAR(255) NOT NULL
api_key_prefix  VARCHAR(16) NOT NULL              -- e.g., sk_live_ab12cd
api_key_hash    VARCHAR(60) UNIQUE NOT NULL       -- bcrypt hash
plan            VARCHAR(50) DEFAULT 'free'
created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

INDEX idx_api_key_prefix (api_key_prefix)        -- for log lookups
```

**trackings**
```
id                    BIGINT PRIMARY KEY AUTO_INCREMENT
tracking_id           VARCHAR(32) UNIQUE NOT NULL    -- e.g., trk_9f3a2
client_id             BIGINT NOT NULL (FK -> clients.id)
tracking_number       VARCHAR(255) NOT NULL
courier_code          VARCHAR(100) NOT NULL
trackingmore_id       VARCHAR(255) UNIQUE            -- TrackingMore's internal ID
origin_country        CHAR(2)
destination_country   CHAR(2)
status                VARCHAR(50)                    -- normalized status
order_id              VARCHAR(255)
created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP
updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
deleted_at            TIMESTAMP NULL                 -- soft delete

INDEX idx_client_id (client_id)
INDEX idx_tracking_number (tracking_number)
INDEX idx_status (status)
```

**tracking_events** (optional - for caching events)
```
id              BIGINT PRIMARY KEY AUTO_INCREMENT
tracking_id     BIGINT NOT NULL (FK -> trackings.id)
event           TEXT NOT NULL
location        VARCHAR(255)
timestamp       TIMESTAMP NOT NULL

INDEX idx_tracking_id (tracking_id)
```

### API Endpoint Summary

| Method | Endpoint                          | Description                          |
|--------|-----------------------------------|--------------------------------------|
| POST   | `/api/v1/trackings`               | Create batch trackings               |
| GET    | `/api/v1/trackings`               | Query trackings with filters         |
| GET    | `/api/v1/trackings/{trackingId}`  | Get single tracking details          |
| DELETE | `/api/v1/trackings/{trackingId}`  | Delete (soft) a tracking             |

### Request/Response Examples

**POST /api/v1/trackings**

Request:
```json
{
  "shipments": [
    {
      "trackingNumber": "JQ048740244IN",
      "courier": "india-post",
      "orderId": "ORD-123",
      "originCountry": "IN",
      "destinationCountry": "IN"
    }
  ]
}
```

Response:
```json
{
  "success": true,
  "created": [
    {
      "trackingId": "trk_9f3a2b8c",
      "trackingNumber": "JQ048740244IN",
      "status": "created"
    }
  ],
  "failed": []
}
```

**GET /api/v1/trackings?trackingNumbers=JQ048740244IN&status=in_transit**

Response:
```json
{
  "data": [
    {
      "trackingId": "trk_9f3a2b8c",
      "trackingNumber": "JQ048740244IN",
      "courier": "india-post",
      "status": "in_transit",
      "lastEvent": "Package in transit to destination",
      "lastUpdated": "2025-12-18T10:12:33Z",
      "origin": "IN",
      "destination": "IN"
    }
  ],
  "meta": {
    "page": 1,
    "limit": 50,
    "total": 1
  }
}
```

## Non-Functional Requirements

- **Performance**: p95 response time for create operations must be <2 seconds (includes upstream TrackingMore call).
- **Scalability**: System must handle 100 concurrent clients without degradation.
- **Security**: 
  - Client API keys must be stored as **hashed values** (bcrypt) with a **plain-text prefix** (first 6-8 characters, e.g., `sk_live_ab12cd****`). This allows key identification in logs without exposing the full key (industry standard, Stripe-style).
  - TrackingMore API key must be externalized (environment variable or secrets manager, never hardcoded).
  - All API communication must use HTTPS only.
- **Reliability**: Implement retry logic for transient TrackingMore API failures (up to 3 retries with exponential backoff).
- **Observability**: 
  - All requests must be logged with **correlation IDs** (either client-supplied via `X-Request-Id` header or auto-generated if missing).
  - Logs must include: correlation ID, client API key prefix, endpoint, HTTP status, response time, and upstream TrackingMore latency.
  - Metrics should track: request counts per client, error rates (4xx/5xx), upstream latency (p50/p95/p99), and rate limit hits.
- **Maintainability**: Code must be modular with clear separation between Controllers, Services, Repositories, and External API clients.

## Constraints

- **Upstream Limits**: TrackingMore allows max 40 tracking numbers per batch create request.
- **Data Retention**: TrackingMore only stores tracking data for 180 days - wrapper should respect this or implement its own archival strategy.
- **Courier Auto-Correction**: TrackingMore can auto-correct courier codes if enabled - wrapper should expose this as an option.

## Out of Scope (for MVP)

- Admin UI for managing clients (can be done directly in database initially).
- Webhook notifications when tracking status changes.
- Caching of tracking results (every query goes to TrackingMore).
- Advanced analytics or reporting dashboards.
- Multi-region deployment or CDN integration.

## Dependencies

- TrackingMore API (external dependency - requires API key and active account).
- Database (MySQL, PostgreSQL, or H2 for local development).
- Redis (if implementing distributed rate limiting - optional for MVP).

## Risks & Mitigation

| Risk                                      | Impact | Mitigation                                                                 |
|-------------------------------------------|--------|---------------------------------------------------------------------------|
| TrackingMore API changes breaking schema  | High   | Implement defensive parsing, version API responses, monitor for changes   |
| TrackingMore rate limits exhausted        | High   | Implement rate limiting per client, monitor usage, upgrade plan if needed |
| Security breach of client API keys        | High   | Hash keys in database, use HTTPS only, implement key rotation             |
| Database performance degradation          | Medium | Index critical columns, implement pagination, consider read replicas      |
| TrackingMore service downtime             | Medium | Implement circuit breaker, return cached data if available, retry logic   |

## Open Questions (Resolved)

| Question | Decision | Phase |
|----------|----------|-------|
| **Q1**: Should the wrapper cache tracking results? | **Yes** - Cache GET results for 2-5 minutes to reduce upstream API calls and improve resilience during TrackingMore outages. | Phase 2 (post-MVP) |
| **Q2**: Should clients update tracking metadata locally? | **Yes** - Allow wrapper-only metadata fields (e.g., internal notes, tags) that don't forward to TrackingMore. | Phase 2 |
| **Q3**: What is the billing model? | **Hybrid** - Per tracking created + monthly subscription plans (free tier: 100 trackings/month, paid tiers scale up). | MVP (manual billing), Phase 2 (automated) |
| **Q4**: Should we support webhooks? | **Yes** - But Phase 3 only. Webhooks forward TrackingMore status updates to client-registered endpoints. | Phase 3 |
| **Q5**: Multiple TrackingMore accounts? | **No** - Single master TrackingMore account for MVP. Multi-account support can be added later if needed for load balancing or vendor diversification. | MVP (single), Phase 4 (multi) |

## Future Enhancements (Nice-to-Have)

- **Idempotency for Create**: Support `Idempotency-Key` header to prevent duplicate tracking creation if client retries (Phase 2).
- **Client Dashboard**: Admin UI for managing clients, viewing usage, and generating reports (Phase 3).
- **Webhook Notifications**: Forward TrackingMore status updates to client-registered webhook endpoints (Phase 3).
- **Advanced Caching**: Implement Redis-based caching with TTL and cache invalidation on status updates (Phase 2).
- **Multi-Region Deployment**: Deploy to multiple regions for lower latency (Phase 4).
