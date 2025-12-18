# Client API Contract: MailIt Postal Wrapper

**Date**: 2025-12-18
**Version**: 1.0.0
**Base URL**: `http://<host>:9000`

---

## Public API (Client)

**Authentication**:
*   Header: `X-API-Key: sk_live_...`

### 1. Create Batch Trackings

**Endpoint**: `POST /api/v1/trackings`

**Description**: Create multiple tracking records (max 40 per request).

**Request Body**:
```json
{
  "shipments": [
    {
      "trackingNumber": "string (required)",
      "courier": "string (required)",
      "orderId": "string (optional)",
      "originCountry": "string (ISO2, optional)",
      "destinationCountry": "string (ISO2, optional)"
    }
  ]
}
```

**Response (201 Created / 400 Bad Request)**:
```json
{
  "success": boolean,
  "created": [
    {
      "trackingId": "string",
      "trackingNumber": "string",
      "status": "created"
    }
  ],
  "failed": [
    {
      "trackingNumber": "string",
      "courier": "string",
      "error": "string"
    }
  ]
}
```

### 2. Get Batch Tracking Details

**Endpoint**: `POST /api/v1/trackings/batch-get`

**Description**: Retrieve details for multiple trackings (max 40).

**Request Body**:
```json
{
  "trackingIds": [
    "string",
    "string"
  ]
}
```

**Response (200 OK)**:
```json
[
  {
    "trackingId": "string",
    "trackingNumber": "string",
    "courierCode": "string",
    "status": "string",
    "substatus": "string",
    "orderId": "string",
    "originCountry": "string",
    "destinationCountry": "string",
    "transitTime": integer,
    "latestEvent": "string",
    "latestCheckpointTime": "string",
    "signedBy": "string",
    "createdAt": "timestamp",
    "updatedAt": "timestamp",
    "events": [
      {
        "date": "string",
        "status": "string",
        "substatus": "string",
        "description": "string",
        "location": "string"
      }
    ]
  }
]
```

### 3. List Trackings

**Endpoint**: `GET /api/v1/trackings`

**Query Parameters**:
*   `status`: string (optional)
*   `page`: integer (default 0)
*   `size`: integer (default 20)

**Response (200 OK)**:
```json
{
  "trackings": [
    {
      "trackingId": "string",
      "trackingNumber": "string",
      "courierCode": "string",
      "status": "string",
      "createdAt": "timestamp",
      "updatedAt": "timestamp"
    }
  ],
  "pagination": {
    "page": integer,
    "limit": integer,
    "total": long,
    "totalPages": integer
  }
}
```

### 4. Get Tracking Details

**Endpoint**: `GET /api/v1/trackings/{trackingId}`

**Response (200 OK)**:
Same as single object in **2. Get Batch Tracking Details**.
