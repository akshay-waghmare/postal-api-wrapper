# MailIt Postal Wrapper API Guide

This guide provides a comprehensive overview of how to use the MailIt Postal Wrapper API. This service acts as a normalized middleware between your application and postal carriers (via TrackingMore), providing multi-tenancy, rate limiting, and standardized tracking statuses.

## Table of Contents

1. [Authentication](#authentication)
2. [Getting Started](#getting-started)
3. [Core Workflows](#core-workflows)
    - [Creating Trackings](#1-create-trackings)
    - [Retrieving Tracking Details](#2-get-tracking-details)
    - [Retrieving Batch Details](#3-get-batch-tracking-details)
    - [Listing Trackings](#4-list-trackings)
4. [API Reference](#api-reference)
5. [Error Handling](#error-handling)
6. [Rate Limiting](#rate-limiting)

---

## Authentication

The API uses two types of authentication:

1.  **Admin Authentication**: (Currently open/unsecured for development) Used to manage clients.
2.  **Client Authentication**: Uses an API Key (`X-API-Key` header) for all tracking operations.

### API Key Format
- **Live Keys**: `sk_live_<random_string>`
- **Test Keys**: `sk_test_<random_string>` (Not yet implemented)

---

## Getting Started

### Prerequisites
- The application must be running (default: `http://localhost:8080`).
- You need a tool to make HTTP requests (e.g., `curl`, Postman, or your application code).

### Base URL
```
http://localhost:8080
```

---

## Core Workflows

### 1. Create Trackings

Register shipments to be tracked. You can send up to 40 shipments in a single batch.

**Endpoint**: `POST /api/v1/trackings`
**Header**: `X-API-Key: <YOUR_API_KEY>`

**Request**:
```json
{
  "shipments": [
    {
      "trackingNumber": "JQ048712813IN",
      "courier": "india-post",
      "orderId": "ORD-1001"
    },
    {
      "trackingNumber": "9400111899223",
      "courier": "usps",
      "originCountry": "US",
      "destinationCountry": "CA"
    }
  ]
}
```

**Response**:
```json
{
  "success": true,
  "created": [
    {
      "trackingId": "trk_r6u8blyuidng",
      "trackingNumber": "JQ048712813IN",
      "courierCode": "india-post"
    }
  ],
  "failed": []
}
```

---

### 2. Get Tracking Details

Fetch the current status and full history of a specific tracking.

**Endpoint**: `GET /api/v1/trackings/{trackingId}`
**Header**: `X-API-Key: <YOUR_API_KEY>`

**Response**:
```json
{
  "trackingId": "trk_r6u8blyuidng",
  "trackingNumber": "JQ048712813IN",
  "courierCode": "india-post",
  "status": "DELIVERED",
  "substatus": "delivered001",
  "transitTime": 5,
  "latestEvent": "Item Delivered",
  "events": [
    {
      "date": "2025-10-13T13:12:14",
      "status": "delivered",
      "substatus": "delivered001",
      "description": "Item Delivered",
      "location": "Bajpur S.O"
    }
  ]
}
```

### 3. Get Batch Tracking Details

Fetch details for multiple trackings in a single request (max 40).

**Endpoint**: `POST /api/v1/trackings/batch-get`
**Header**: `X-API-Key: <YOUR_API_KEY>`

**Request**:
```json
{
  "trackingIds": [
    "trk_r6u8blyuidng",
    "trk_brhjv3p9ioe0"
  ]
}
```

**Response**:
```json
[
  {
    "trackingId": "trk_r6u8blyuidng",
    "trackingNumber": "JQ048712813IN",
    "status": "DELIVERED",
    ...
  },
  {
    "trackingId": "trk_brhjv3p9ioe0",
    "trackingNumber": "JQ048712827IN",
    "status": "TRANSIT",
    ...
  }
]
```

---

### 4. List Trackings

Retrieve a paginated list of all trackings for your client account.

**Endpoint**: `GET /api/v1/trackings`
**Header**: `X-API-Key: <YOUR_API_KEY>`
**Query Params**:
- `page`: Page number (default 0)
- `size`: Items per page (default 20)
- `status`: Filter by status (e.g., `DELIVERED`, `TRANSIT`)

**Response**:
```json
{
  "trackings": [
    {
      "trackingId": "trk_r6u8blyuidng",
      "trackingNumber": "JQ048712813IN",
      "status": "DELIVERED",
      "courierCode": "india-post",
      "createdAt": "2025-12-18T10:05:00"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## API Reference

### Standardized Statuses
The API normalizes carrier statuses into the following values:

| Status | Description |
|--------|-------------|
| `PENDING` | Tracking created but no info from carrier yet |
| `INFO_RECEIVED` | Carrier has received shipping information |
| `TRANSIT` | Package is moving |
| `PICKUP` | Out for delivery or ready for pickup |
| `DELIVERED` | Successfully delivered |
| `EXCEPTION` | Delivery exception (customs, delay, etc.) |
| `EXPIRED` | Tracking expired (no update for long time) |
| `NOT_FOUND` | Carrier does not recognize the number |

### Common Courier Codes
- `india-post`
- `usps`
- `fedex`
- `dhl`
- `ups`
- `royal-mail`

*(See TrackingMore documentation for full list)*

---

## Error Handling

The API uses standard HTTP status codes:

- `200 OK`: Success
- `201 Created`: Resource created
- `400 Bad Request`: Invalid input (missing fields, bad format)
- `401 Unauthorized`: Missing or invalid API Key
- `403 Forbidden`: Valid key but no permission for this resource
- `404 Not Found`: Resource does not exist
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server-side issue

**Error Response Format**:
```json
{
  "timestamp": "2025-12-18T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Courier code is required",
  "path": "/api/v1/trackings"
}
```

---

## Rate Limiting

Rate limits are applied per API Key based on the client's plan.

| Plan | Requests per Minute |
|------|---------------------|
| `FREE` | 60 |
| `STARTER` | 120 |
| `PRO` | 300 |
| `ENTERPRISE` | 600 |

If you exceed the limit, you will receive a `429 Too Many Requests` response.
