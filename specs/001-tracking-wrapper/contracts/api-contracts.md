# API Contracts: TrackingMore Wrapper API

**Feature**: 001-tracking-wrapper  
**Date**: 2025-12-18  
**Phase**: 1 (Design & Contracts)  
**Base URL**: `https://api.mailit.com/api/v1`

---

## Authentication

All endpoints require authentication via API key.

**Header**:
```
X-API-Key: sk_live_abc123def456
```

**Responses**:
- `401 Unauthorized`: Invalid or missing API key
- `429 Too Many Requests`: Rate limit exceeded

---

## 1. Create Batch Trackings

**Endpoint**: `POST /trackings`

**Description**: Create multiple tracking records (max 40 per request). Partial failures are supported - valid trackings are created even if some fail validation.

### Request

**Headers**:
```
Content-Type: application/json
X-API-Key: sk_live_abc123def456
X-Request-Id: optional-correlation-id
```

**Body**:
```json
{
  "shipments": [
    {
      "trackingNumber": "JQ048740244IN",
      "courier": "india-post",
      "orderId": "ORD-12345",
      "originCountry": "IN",
      "destinationCountry": "US"
    },
    {
      "trackingNumber": "UB209300714LV",
      "courier": "usps",
      "orderId": "ORD-12346",
      "originCountry": "US",
      "destinationCountry": "US"
    }
  ]
}
```

**Field Definitions**:
- `shipments` (array, required): List of shipments to track
  - `trackingNumber` (string, required): Tracking number from carrier (min 1 char)
  - `courier` (string, required): Courier code (e.g., "india-post", "usps") - lowercase, alphanumeric + hyphens
  - `orderId` (string, optional): Client's internal order ID
  - `originCountry` (string, optional): ISO 3166-1 alpha-2 code (2 chars, e.g., "IN")
  - `destinationCountry` (string, optional): ISO 3166-1 alpha-2 code (2 chars)

**Validation**:
- Max 40 shipments per request
- `trackingNumber` must not be empty
- `courier` must match pattern: `^[a-z0-9-]+$`
- Country codes must be exactly 2 characters (if provided)

### Response (Success)

**Status**: `200 OK`

```json
{
  "success": true,
  "created": [
    {
      "trackingId": "trk_9f3a2b8c",
      "trackingNumber": "JQ048740244IN",
      "status": "created"
    },
    {
      "trackingId": "trk_7e2d1a9f",
      "trackingNumber": "UB209300714LV",
      "status": "created"
    }
  ],
  "failed": []
}
```

### Response (Partial Failure)

**Status**: `200 OK` (some succeeded)

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
  "failed": [
    {
      "trackingNumber": "INVALID123",
      "courier": "unknown-carrier",
      "error": "Invalid courier code"
    }
  ]
}
```

### Response (Validation Error)

**Status**: `400 Bad Request`

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    "shipments: must not be empty",
    "shipments[0].trackingNumber: must not be blank"
  ]
}
```

### Response (Rate Limit)

**Status**: `429 Too Many Requests`

**Headers**:
```
Retry-After: 3600
```

```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Daily request limit exceeded. Upgrade your plan or retry after 3600 seconds.",
  "details": {
    "limit": 100,
    "remaining": 0,
    "resetAt": "2025-12-19T00:00:00Z"
  }
}
```

---

## 2. Get Trackings (Query)

**Endpoint**: `GET /trackings`

**Description**: Retrieve tracking records with filtering and pagination.

### Request

**Headers**:
```
X-API-Key: sk_live_abc123def456
X-Request-Id: optional-correlation-id
```

**Query Parameters**:
- `trackingNumbers` (string, optional): Comma-separated list of tracking numbers (max 40)
  - Example: `trackingNumbers=JQ048740244IN,UB209300714LV`
- `status` (string, optional): Filter by status (pending, not_found, in_transit, out_for_delivery, delivered, exception, expired, returned)
- `page` (integer, optional, default: 1): Page number (1-based)
- `limit` (integer, optional, default: 50, max: 200): Results per page

**Example**:
```
GET /trackings?trackingNumbers=JQ048740244IN,UB209300714LV&status=in_transit&page=1&limit=50
```

### Response (Success)

**Status**: `200 OK`

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
      "destination": "US",
      "orderId": "ORD-12345"
    },
    {
      "trackingId": "trk_7e2d1a9f",
      "trackingNumber": "UB209300714LV",
      "courier": "usps",
      "status": "delivered",
      "lastEvent": "Delivered to recipient",
      "lastUpdated": "2025-12-17T15:30:00Z",
      "origin": "US",
      "destination": "US",
      "orderId": "ORD-12346"
    }
  ],
  "meta": {
    "page": 1,
    "limit": 50,
    "total": 2,
    "totalPages": 1
  }
}
```

### Response (Empty Result)

**Status**: `200 OK`

```json
{
  "data": [],
  "meta": {
    "page": 1,
    "limit": 50,
    "total": 0,
    "totalPages": 0
  }
}
```

### Response (Upstream Unavailable with Stale Cache)

**Status**: `200 OK`

**Headers**:
```
X-Data-Source: cache
X-Cache-Age: 120
```

```json
{
  "data": [
    {
      "trackingId": "trk_9f3a2b8c",
      "trackingNumber": "JQ048740244IN",
      "courier": "india-post",
      "status": "in_transit",
      "lastEvent": "Package in transit to destination",
      "lastUpdated": "2025-12-18T08:00:00Z",
      "origin": "IN",
      "destination": "US",
      "orderId": "ORD-12345"
    }
  ],
  "meta": {
    "page": 1,
    "limit": 50,
    "total": 1,
    "totalPages": 1,
    "warning": "Upstream service unavailable, data may be stale"
  }
}
```

---

## 3. Get Single Tracking

**Endpoint**: `GET /trackings/{trackingId}`

**Description**: Retrieve detailed information for a specific tracking by wrapper tracking ID.

### Request

**Headers**:
```
X-API-Key: sk_live_abc123def456
X-Request-Id: optional-correlation-id
```

**Path Parameters**:
- `trackingId` (string, required): Wrapper tracking ID (e.g., "trk_9f3a2b8c")

**Example**:
```
GET /trackings/trk_9f3a2b8c
```

### Response (Success)

**Status**: `200 OK`

```json
{
  "trackingId": "trk_9f3a2b8c",
  "trackingNumber": "JQ048740244IN",
  "courier": "india-post",
  "status": "in_transit",
  "lastEvent": "Package in transit to destination",
  "lastUpdated": "2025-12-18T10:12:33Z",
  "origin": "IN",
  "destination": "US",
  "orderId": "ORD-12345",
  "createdAt": "2025-12-15T08:00:00Z"
}
```

### Response (Not Found)

**Status**: `404 Not Found`

```json
{
  "code": "TRACKING_NOT_FOUND",
  "message": "Tracking with ID 'trk_9f3a2b8c' not found or does not belong to your account"
}
```

---

## 4. Delete Tracking

**Endpoint**: `DELETE /trackings/{trackingId}`

**Description**: Soft-delete a tracking record. Upstream TrackingMore deletion is skipped if tracking is in a final state (delivered, expired).

### Request

**Headers**:
```
X-API-Key: sk_live_abc123def456
X-Request-Id: optional-correlation-id
```

**Path Parameters**:
- `trackingId` (string, required): Wrapper tracking ID

**Example**:
```
DELETE /trackings/trk_9f3a2b8c
```

### Response (Success)

**Status**: `200 OK`

```json
{
  "success": true,
  "trackingId": "trk_9f3a2b8c",
  "message": "Tracking deleted successfully"
}
```

### Response (Not Found)

**Status**: `404 Not Found`

```json
{
  "code": "TRACKING_NOT_FOUND",
  "message": "Tracking with ID 'trk_9f3a2b8c' not found"
}
```

### Response (Forbidden)

**Status**: `403 Forbidden`

```json
{
  "code": "FORBIDDEN",
  "message": "You do not have permission to delete this tracking"
}
```

---

## Common Error Responses

### 401 Unauthorized
```json
{
  "code": "UNAUTHORIZED",
  "message": "Invalid or missing API key"
}
```

### 429 Too Many Requests
```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Request quota exceeded",
  "details": {
    "limit": 100,
    "remaining": 0,
    "resetAt": "2025-12-19T00:00:00Z"
  }
}
```

### 500 Internal Server Error
```json
{
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred. Please contact support with correlation ID.",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 503 Service Unavailable
```json
{
  "code": "UPSTREAM_UNAVAILABLE",
  "message": "Tracking service is temporarily unavailable. Please retry later.",
  "retryAfter": 60
}
```

---

## Status Enum Values

| Status | Description |
|--------|-------------|
| `pending` | Tracking created, awaiting carrier info |
| `not_found` | Carrier has no record of this tracking number |
| `in_transit` | Package is in transit |
| `out_for_delivery` | Out for delivery to recipient |
| `delivered` | Successfully delivered |
| `exception` | Delivery exception or problem |
| `expired` | Tracking has expired |
| `returned` | Returned to sender |

---

## Rate Limiting

Rate limits are enforced per client based on their plan:

| Plan | Requests/Day | Trackings/Batch |
|------|--------------|-----------------|
| Free | 100 | 10 |
| Starter | 1,000 | 40 |
| Pro | 10,000 | 40 |
| Enterprise | Unlimited | 40 |

When rate limit is exceeded:
- HTTP Status: `429 Too Many Requests`
- Header: `Retry-After: {seconds}`
- Response includes `resetAt` timestamp

---

## Versioning

- Current version: `v1`
- All endpoints are prefixed with `/api/v1`
- Breaking changes will result in a new version (e.g., `/api/v2`)
- Non-breaking changes (new fields, new endpoints) do not require version bump

---

## Correlation IDs

Clients can provide a correlation ID via the `X-Request-Id` header for request tracing. If not provided, the server will generate one. The correlation ID is included in error responses and logs.

Example:
```
Request:
X-Request-Id: my-unique-request-123

Response (error):
{
  "code": "INTERNAL_ERROR",
  "message": "...",
  "correlationId": "my-unique-request-123"
}
```
