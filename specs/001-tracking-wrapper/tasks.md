# Tasks: TrackingMore Wrapper API

**Input**: Design documents from `/specs/001-tracking-wrapper/`  
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅  
**Date**: 2025-12-18

**Tests**: Tests are included as per standard practice for this Spring Boot API project.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Based on plan.md structure - single Spring Boot project:
- `src/main/java/com/mailit/wrapper/` - Java source
- `src/main/resources/` - Configuration and migrations
- `src/test/java/com/mailit/wrapper/` - Tests

---

## Phase 1: Setup (Project Initialization)

**Purpose**: Initialize Spring Boot project with dependencies and basic configuration

- [X] T001 Create Spring Boot project with Maven/Gradle, Java 17, and required dependencies (Spring Web, Data JPA, Validation, Lombok) in pom.xml or build.gradle
- [X] T002 [P] Create main application class in src/main/java/com/mailit/wrapper/MailItWrapperApplication.java
- [X] T003 [P] Configure application.yml with Spring profiles (dev, prod) in src/main/resources/application.yml
- [X] T004 [P] Configure H2 database for development in src/main/resources/application-dev.yml
- [X] T005 [P] Configure PostgreSQL for production in src/main/resources/application-prod.yml
- [X] T006 [P] Configure Flyway migration settings in application.yml
- [X] T007 [P] Configure OpenAPI/Swagger documentation in src/main/java/com/mailit/wrapper/config/OpenApiConfig.java
- [X] T008 [P] Configure Logback with MDC pattern for correlation IDs in src/main/resources/logback-spring.xml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Database Schema & Migrations

- [X] T009 Create V1__create_clients_table.sql migration in src/main/resources/db/migration/V1__create_clients_table.sql
- [X] T010 Create V2__create_trackings_table.sql migration in src/main/resources/db/migration/V2__create_trackings_table.sql

### Domain Enums

- [X] T011 [P] Create WrapperStatus enum (pending, not_found, in_transit, out_for_delivery, delivered, exception, expired, returned) in src/main/java/com/mailit/wrapper/model/WrapperStatus.java
- [X] T012 [P] Create RateLimitPlan enum (FREE, STARTER, PRO, ENTERPRISE) in src/main/java/com/mailit/wrapper/model/RateLimitPlan.java

### JPA Entities

- [X] T013 Create Client JPA entity with Stripe-style API key fields (api_key_prefix, api_key_hash) in src/main/java/com/mailit/wrapper/model/entity/Client.java
- [X] T014 Create Tracking JPA entity with soft delete (@Where annotation) in src/main/java/com/mailit/wrapper/model/entity/Tracking.java (depends on T013)

### Repositories

- [X] T015 [P] Create ClientRepository interface with findByApiKeyHash method in src/main/java/com/mailit/wrapper/repository/ClientRepository.java
- [X] T016 [P] Create TrackingRepository interface with client-scoped query methods in src/main/java/com/mailit/wrapper/repository/TrackingRepository.java

### Core Infrastructure

- [X] T017 Create CorrelationIdFilter (OncePerRequestFilter) for X-Request-Id handling and MDC in src/main/java/com/mailit/wrapper/config/CorrelationIdFilter.java
- [X] T018 Create ApiKeyAuthFilter (OncePerRequestFilter) for X-API-Key authentication in src/main/java/com/mailit/wrapper/config/ApiKeyAuthFilter.java (depends on T015)
- [X] T019 Create AuthService for API key validation (hash lookup, client retrieval) in src/main/java/com/mailit/wrapper/service/AuthService.java (depends on T015)
- [X] T020 Create RateLimitService with Bucket4j (in-memory for dev, pluggable for Redis) in src/main/java/com/mailit/wrapper/service/RateLimitService.java
  - Note: Redis-backed implementation can be added later without changing RateLimitService interface

### Exception Handling

- [X] T021 [P] Create custom exception classes (TrackingMoreException, AuthenticationException, RateLimitException, TrackingNotFoundException) in src/main/java/com/mailit/wrapper/exception/
- [X] T022 Create GlobalExceptionHandler (@ControllerAdvice) mapping exceptions to structured JSON in src/main/java/com/mailit/wrapper/exception/GlobalExceptionHandler.java (depends on T021)

### DTOs

- [X] T023 [P] Create ErrorResponse record in src/main/java/com/mailit/wrapper/model/dto/response/ErrorResponse.java
- [X] T024 [P] Create PaginationMeta record in src/main/java/com/mailit/wrapper/model/dto/response/PaginationMeta.java

### Utilities

- [X] T025 [P] Create TrackingIdGenerator (generates trk_xxx IDs) in src/main/java/com/mailit/wrapper/util/TrackingIdGenerator.java
- [X] T026 [P] Create StatusMapper (TrackingMore status → WrapperStatus enum) in src/main/java/com/mailit/wrapper/util/StatusMapper.java

### TrackingMore Client Infrastructure

- [X] T027 Create TrackingMoreConfig (@Configuration) with RestClient bean and externalized API key in src/main/java/com/mailit/wrapper/config/TrackingMoreConfig.java
- [X] T028 [P] Create TrackingMore request/response DTOs in src/main/java/com/mailit/wrapper/model/trackingmore/ (TrackingMoreRequest.java, TrackingMoreResponse.java, TrackingMoreShipment.java)
- [X] T029 Create TrackingMoreClient interface in src/main/java/com/mailit/wrapper/client/TrackingMoreClient.java
- [X] T030 Create TrackingMoreClientImpl with RestClient, retry logic, and circuit breaker in src/main/java/com/mailit/wrapper/client/TrackingMoreClientImpl.java (depends on T027, T028, T029)
  - Retry only on: timeouts, 5xx errors
  - Never retry on: 4xx errors (validation, auth) — avoids duplicate creates

### Filter Registration

- [X] T031 Register filters (CorrelationIdFilter, ApiKeyAuthFilter) with correct order in src/main/java/com/mailit/wrapper/config/FilterConfig.java (depends on T017, T018)

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Client Creates Shipment Trackings (Priority: P1) 🎯 MVP

**Goal**: Enable API clients to register up to 40 shipment tracking numbers in a single batch request

**Independent Test**: Call POST `/api/v1/trackings` with valid shipment data and verify response contains tracking IDs and success confirmation

### Tests for User Story 1

- [ ] T032 [P] [US1] Create contract test for POST /api/v1/trackings in src/test/java/com/mailit/wrapper/controller/TrackingControllerCreateTest.java
- [ ] T033 [P] [US1] Create unit test for TrackingService.createTrackings in src/test/java/com/mailit/wrapper/service/TrackingServiceCreateTest.java
- [ ] T034 [P] [US1] Create mock test for TrackingMoreClient.createBatchTrackings in src/test/java/com/mailit/wrapper/client/TrackingMoreClientTest.java

### DTOs for User Story 1

- [X] T035 [P] [US1] Create ShipmentDto record (trackingNumber, courier, orderId, originCountry, destinationCountry) with validation annotations in src/main/java/com/mailit/wrapper/model/dto/request/ShipmentDto.java
- [X] T036 [P] [US1] Create CreateTrackingRequest record (List<ShipmentDto> shipments, max 40) with validation in src/main/java/com/mailit/wrapper/model/dto/request/CreateTrackingRequest.java
- [X] T037 [P] [US1] Create CreatedTrackingDto record in src/main/java/com/mailit/wrapper/model/dto/response/CreatedTrackingDto.java
- [X] T038 [P] [US1] Create FailedTrackingDto record in src/main/java/com/mailit/wrapper/model/dto/response/FailedTrackingDto.java
- [X] T039 [P] [US1] Create BatchCreateResponse record (success, created, failed arrays) in src/main/java/com/mailit/wrapper/model/dto/response/BatchCreateResponse.java

### Service Layer for User Story 1

- [X] T040 [US1] Create TrackingService interface with createTrackings method in src/main/java/com/mailit/wrapper/service/TrackingService.java
- [X] T041 [US1] Implement TrackingServiceImpl.createTrackings with partial failure handling (valid shipments succeed, invalid returned in failed array) in src/main/java/com/mailit/wrapper/service/TrackingServiceImpl.java (depends on T040, T030, T016, T025, T026)

### Controller for User Story 1

- [X] T042 [US1] Create TrackingController with POST /api/v1/trackings endpoint in src/main/java/com/mailit/wrapper/controller/TrackingController.java (depends on T040, T036, T039)

**Checkpoint**: User Story 1 (batch tracking creation) is fully functional and testable

---

## Phase 4: User Story 2 - Client Queries Tracking Status (Priority: P1) 🎯 MVP

**Goal**: Enable API clients to retrieve current tracking status with filtering and pagination

**Independent Test**: Call GET `/api/v1/trackings?trackingNumbers=ABC123` and verify normalized response with status, events, and metadata

### Tests for User Story 2

- [ ] T043 [P] [US2] Create contract test for GET /api/v1/trackings with filters in src/test/java/com/mailit/wrapper/controller/TrackingControllerQueryTest.java
- [ ] T044 [P] [US2] Create unit test for TrackingService.getTrackings with pagination in src/test/java/com/mailit/wrapper/service/TrackingServiceQueryTest.java

### DTOs for User Story 2

- [X] T045 [P] [US2] Create TrackingDto record (trackingId, trackingNumber, courier, status, lastEvent, lastUpdated, origin, destination, orderId) in src/main/java/com/mailit/wrapper/model/dto/response/TrackingDto.java
- [X] T046 [P] [US2] Create TrackingListResponse record (data array, meta pagination) in src/main/java/com/mailit/wrapper/model/dto/response/TrackingListResponse.java

### Repository Extensions for User Story 2

- [X] T047 [US2] Add TrackingRepository methods for filtered queries (by trackingNumbers, status) with pagination in src/main/java/com/mailit/wrapper/repository/TrackingRepository.java

### Service Layer for User Story 2

- [X] T048 [US2] Add TrackingService.getTrackings method signature to interface in src/main/java/com/mailit/wrapper/service/TrackingService.java
- [X] T049 [US2] Implement TrackingServiceImpl.getTrackings with filtering and pagination in src/main/java/com/mailit/wrapper/service/TrackingServiceImpl.java (depends on T047, T048)

### Controller for User Story 2

- [X] T050 [US2] Add GET /api/v1/trackings endpoint to TrackingController with query params (trackingNumbers, status, page, limit) in src/main/java/com/mailit/wrapper/controller/TrackingController.java (depends on T048, T046)

**Checkpoint**: User Stories 1 AND 2 (create and query) are fully functional - **MVP complete**

---

## Phase 5: User Story 3 - Client Retrieves Single Tracking Details (Priority: P2)

**Goal**: Enable API clients to fetch detailed information for a specific tracking ID

**Independent Test**: Call GET `/api/v1/trackings/{trackingId}` and verify full tracking history is returned

### Tests for User Story 3

- [ ] T051 [P] [US3] Create contract test for GET /api/v1/trackings/{trackingId} in src/test/java/com/mailit/wrapper/controller/TrackingControllerGetOneTest.java
- [ ] T052 [P] [US3] Create unit test for TrackingService.getTrackingById with ownership validation in src/test/java/com/mailit/wrapper/service/TrackingServiceGetOneTest.java

### DTOs for User Story 3

- [X] T053 [P] [US3] Create TrackingDetailResponse record (full details including createdAt) in src/main/java/com/mailit/wrapper/model/dto/response/TrackingDetailResponse.java

### Service Layer for User Story 3

- [X] T054 [US3] Add TrackingService.getTrackingById method signature to interface in src/main/java/com/mailit/wrapper/service/TrackingService.java
- [X] T055 [US3] Implement TrackingServiceImpl.getTrackingById with ownership check (return 404 if not owned) in src/main/java/com/mailit/wrapper/service/TrackingServiceImpl.java (depends on T054, T016)

### Controller for User Story 3

- [X] T056 [US3] Add GET /api/v1/trackings/{trackingId} endpoint to TrackingController in src/main/java/com/mailit/wrapper/controller/TrackingController.java (depends on T054, T053)
- [X] T057 [US3] Add POST /api/v1/trackings/batch-get endpoint to TrackingController for batch retrieval

**Checkpoint**: User Story 3 (single tracking details) is fully functional

---

## Phase 6: User Story 4 - Client Deletes Tracking (Priority: P3)

**Goal**: Enable API clients to soft-delete trackings they no longer need to monitor

**Independent Test**: Call DELETE `/api/v1/trackings/{trackingId}` and verify tracking no longer appears in queries

### Tests for User Story 4

- [ ] T057 [P] [US4] Create contract test for DELETE /api/v1/trackings/{trackingId} in src/test/java/com/mailit/wrapper/controller/TrackingControllerDeleteTest.java
- [ ] T058 [P] [US4] Create unit test for TrackingService.deleteTracking with ownership/permission validation in src/test/java/com/mailit/wrapper/service/TrackingServiceDeleteTest.java

### DTOs for User Story 4

- [X] T059 [P] [US4] Create DeleteTrackingResponse record (success, trackingId, message) in src/main/java/com/mailit/wrapper/model/dto/response/DeleteTrackingResponse.java

### Service Layer for User Story 4

- [X] T060 [US4] Add TrackingService.deleteTracking method signature to interface in src/main/java/com/mailit/wrapper/service/TrackingService.java
- [X] T061 [US4] Implement TrackingServiceImpl.deleteTracking with soft delete, ownership check (403 if not owned), skip upstream delete if delivered/expired in src/main/java/com/mailit/wrapper/service/TrackingServiceImpl.java (depends on T060, T016)
  - If already soft-deleted → return idempotent success (clients love idempotent deletes)

### Controller for User Story 4

- [X] T062 [US4] Add DELETE /api/v1/trackings/{trackingId} endpoint to TrackingController in src/main/java/com/mailit/wrapper/controller/TrackingController.java (depends on T060, T059)

**Checkpoint**: User Story 4 (delete tracking) is fully functional

**Checkpoint**: User Story 4 (delete tracking) is fully functional

---

## Phase 7: User Story 5 - Administrator Manages Client API Keys (Priority: P2)

**Goal**: Enable system administrators to create clients and generate API keys

**Independent Test**: Create a new client with API key and verify authentication succeeds

### Tests for User Story 5

- [ ] T063 [P] [US5] Create unit test for ClientService.createClient with API key generation in src/test/java/com/mailit/wrapper/service/ClientServiceTest.java
- [ ] T064 [P] [US5] Create integration test for full authentication flow in src/test/java/com/mailit/wrapper/integration/AuthenticationIntegrationTest.java

### Service Layer for User Story 5

- [X] T065 [US5] Create ClientService interface with createClient, revokeApiKey methods in src/main/java/com/mailit/wrapper/service/ClientService.java
- [X] T066 [US5] Implement ClientServiceImpl with Stripe-style API key generation (sk_live_xxx prefix + bcrypt hash) in src/main/java/com/mailit/wrapper/service/ClientServiceImpl.java (depends on T065, T015)

### Admin Endpoints (Optional for MVP)

- [X] T067 [US5] Create AdminController with POST /admin/clients endpoint (INTERNAL / NON-PUBLIC — exclude from Swagger if globally enabled) in src/main/java/com/mailit/wrapper/controller/AdminController.java (depends on T065)

**Checkpoint**: User Story 5 (client management) is functional for internal admin use

---

## Phase 8: Integration Tests

**Purpose**: End-to-end tests covering full request flows

- [ ] T068 [P] Create integration test for batch tracking creation flow in src/test/java/com/mailit/wrapper/integration/CreateTrackingIntegrationTest.java
- [ ] T069 [P] Create integration test for query and pagination flow in src/test/java/com/mailit/wrapper/integration/QueryTrackingIntegrationTest.java
- [ ] T070 [P] Create integration test for rate limiting enforcement in src/test/java/com/mailit/wrapper/integration/RateLimitIntegrationTest.java
- [ ] T071 [P] Create integration test for error handling (invalid API key, validation errors) in src/test/java/com/mailit/wrapper/integration/ErrorHandlingIntegrationTest.java

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, cleanup, and final validation

- [X] T072 [P] Add Javadoc to all public service methods in src/main/java/com/mailit/wrapper/service/
- [X] T073 [P] Add @Operation, @ApiResponse OpenAPI annotations to all controller endpoints in src/main/java/com/mailit/wrapper/controller/
- [X] T074 [P] Create README.md with setup instructions, API overview, and development guide
- [X] T075 [P] Create sample data SQL script for local development in src/main/resources/db/data/sample-data.sql
- [X] T076 Run quickstart.md validation - verify all documented API calls work as expected
- [X] T077 Perform final code cleanup and ensure consistent formatting
- [X] T078 Verify all constitutional principles are satisfied (error handling, type safety, abstraction, testability, documentation)

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)
    ↓
Phase 2 (Foundational) ← BLOCKS all user stories
    ↓
    ├── Phase 3 (US1: Create) ← MVP
    ├── Phase 4 (US2: Query) ← MVP
    ├── Phase 5 (US3: Get One)
    ├── Phase 6 (US4: Delete)
    └── Phase 7 (US5: Admin)
    ↓
Phase 8 (Integration Tests)
    ↓
Phase 9 (Polish)
```

### User Story Dependencies

| User Story | Depends On | Independent? |
|------------|------------|--------------|
| US1 (Create Trackings) | Foundational only | ✅ Yes |
| US2 (Query Trackings) | Foundational only | ✅ Yes |
| US3 (Get Single Tracking) | Foundational only | ✅ Yes |
| US4 (Delete Tracking) | Foundational only | ✅ Yes |
| US5 (Admin API Keys) | Foundational only | ✅ Yes |

**Note**: All user stories can be worked on in parallel once Phase 2 (Foundational) is complete.

### Within Each User Story

1. Tests MUST be written first and FAIL before implementation
2. DTOs before services
3. Service interfaces before implementations
4. Service implementations before controllers
5. Story complete before moving to next priority

### Parallel Opportunities per Phase

**Phase 1 (Setup)**:
- T002, T003, T004, T005, T006, T007, T008 can all run in parallel

**Phase 2 (Foundational)**:
- T011, T012 (enums) in parallel
- T015, T016 (repositories) in parallel after entities
- T021, T023, T024, T025, T026, T028 in parallel
- T017, T018 (filters) in parallel (but T018 depends on T015)

**Phase 3-7 (User Stories)**:
- All test tasks [P] within each story can run in parallel
- All DTO tasks [P] within each story can run in parallel
- Different user stories can run in parallel with different team members

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Wave 1: Migrations
T009, T010

# Wave 2: Enums (parallel)
T011 & T012

# Wave 3: Entities (sequential - T014 depends on T013)
T013, then T014

# Wave 4: Repositories, DTOs, Utils, Exceptions (parallel)
T015 & T016 & T021 & T023 & T024 & T025 & T026 & T028

# Wave 5: Services (depends on repositories)
T017 & T019 & T020

# Wave 6: Auth filter (depends on T015, T019)
T018

# Wave 7: TrackingMore client
T027, T029, then T030

# Wave 8: Exception handler, filter registration
T022, T031
```

---

## MVP Scope (Recommended)

For fastest time-to-value, implement in this order:

1. **Phase 1**: Setup (T001-T008)
2. **Phase 2**: Foundational (T009-T031)
3. **Phase 3**: User Story 1 - Create Trackings (T032-T042)
4. **Phase 4**: User Story 2 - Query Trackings (T043-T050)

**MVP Complete**: At task T050, you have a functional API that can:
- Authenticate clients via API key
- Create batch trackings (up to 40)
- Query trackings with filters and pagination
- Handle errors gracefully
- Enforce rate limits

**Post-MVP**: Phases 5-9 add single tracking details, delete, admin features, and polish.

---

## Task Summary

| Phase | Focus | Tasks | Parallel Tasks |
|-------|-------|-------|----------------|
| 1 | Setup | T001-T008 (8) | 7 |
| 2 | Foundational | T009-T031 (23) | 14 |
| 3 | US1: Create | T032-T042 (11) | 8 |
| 4 | US2: Query | T043-T050 (8) | 4 |
| 5 | US3: Get One | T051-T056 (6) | 3 |
| 6 | US4: Delete | T057-T062 (6) | 3 |
| 7 | US5: Admin | T063-T067 (5) | 2 |
| 8 | Integration | T068-T071 (4) | 4 |
| 9 | Polish | T072-T078 (7) | 5 |
| **Total** | | **78 tasks** | **50 parallelizable** |

---

**Tasks Generated**: 2025-12-18  
**Based On**: spec.md (5 user stories), plan.md (architecture), data-model.md (2 entities), contracts/ (4 endpoints)  
**Ready for Implementation**: YES
