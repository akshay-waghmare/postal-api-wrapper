# Admin API Contract: MailIt Postal Wrapper

**Date**: 2025-12-18
**Version**: 1.0.0
**Base URL**: `http://<host>:9000`

---

## Admin API (Internal)

**Authentication**: None (Network protected)

### 1. Create Client

**Endpoint**: `POST /admin/clients`

**Request Body**:
```json
{
  "name": "string (required)",
  "plan": "FREE | PRO | ENTERPRISE (optional, default FREE)"
}
```

**Response (201 Created)**:
```json
{
  "id": long,
  "name": "string",
  "apiKeyPrefix": "string",
  "apiKey": "string (full key, only returned here)",
  "plan": "string",
  "active": boolean,
  "expiresAt": "timestamp"
}
```

### 2. List Clients

**Endpoint**: `GET /admin/clients`

**Response (200 OK)**:
```json
[
  {
    "id": long,
    "name": "string",
    "apiKeyPrefix": "string",
    "plan": "string",
    "active": boolean,
    "expiresAt": "timestamp"
  }
]
```

### 3. Rotate API Key

**Endpoint**: `POST /admin/clients/{clientId}/rotate-key`

**Response (200 OK)**:
Same as **1. Create Client** (returns new full API key).

### 4. Update Client Status

**Endpoint**: `PATCH /admin/clients/{clientId}/status`

**Request Body**:
```json
{
  "active": boolean,
  "expiresAt": "timestamp (optional)"
}
```

**Response (200 OK)**:
Same as **2. List Clients** (single object).

### 5. Update Client Plan

**Endpoint**: `PATCH /admin/clients/{clientId}/plan`

**Request Body**:
```json
{
  "plan": "FREE | PRO | ENTERPRISE"
}
```

**Response (200 OK)**:
Same as **2. List Clients** (single object).

### 6. Revoke API Key

**Endpoint**: `DELETE /admin/clients/{clientId}/api-key`

**Response**: `204 No Content`
