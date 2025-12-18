# Quickstart Guide: TrackingMore Wrapper API

**Feature**: 001-tracking-wrapper  
**Date**: 2025-12-18  
**Audience**: Developers integrating with the wrapper API

---

## Overview

The MailIt TrackingMore Wrapper API allows you to track shipments from multiple carriers without directly integrating with TrackingMore. Key benefits:

- **Single Integration**: One API for all tracking needs
- **Vendor Independence**: Switch tracking providers without client-side changes
- **Simplified Schema**: Normalized responses across all carriers
- **Built-in Rate Limiting**: Fair usage enforcement
- **Full Audit Trail**: Complete request logging for compliance

---

## Prerequisites

- API key (contact support to get `sk_live_xxx` or `sk_test_xxx`)
- HTTP client (curl, Postman, or your language's HTTP library)
- Basic understanding of REST APIs

---

## Getting Started

### 1. Authenticate Your Requests

All API requests require your API key in the `X-API-Key` header:

```bash
curl -H "X-API-Key: sk_live_abc123def456" \
     https://api.mailit.com/api/v1/trackings
```

**Important**:
- Never share your API key or commit it to version control
- Use environment variables: `export MAILIT_API_KEY=sk_live_xxx`
- Test mode keys (`sk_test_xxx`) work only in sandbox environment

---

### 2. Create Your First Tracking

**Scenario**: You shipped a package via India Post and want to track it.

**Request**:
```bash
curl -X POST https://api.mailit.com/api/v1/trackings \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_abc123def456" \
  -d '{
    "shipments": [
      {
        "trackingNumber": "JQ048740244IN",
        "courier": "india-post",
        "orderId": "ORD-12345",
        "originCountry": "IN",
        "destinationCountry": "US"
      }
    ]
  }'
```

**Response**:
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

**What Happened**:
1. Wrapper validated your request
2. Created a tracking record in the database
3. Called TrackingMore API to register the tracking
4. Returned your wrapper tracking ID: `trk_9f3a2b8c`

**Save this `trackingId`** - you'll use it to query status later.

---

### 3. Query Tracking Status

**Scenario**: Check the current status of your shipment.

**Request**:
```bash
curl -X GET "https://api.mailit.com/api/v1/trackings?trackingNumbers=JQ048740244IN" \
  -H "X-API-Key: sk_live_abc123def456"
```

**Response**:
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
    }
  ],
  "meta": {
    "page": 1,
    "limit": 50,
    "total": 1,
    "totalPages": 1
  }
}
```

**Status Values**:
- `pending`: Awaiting carrier info
- `in_transit`: Package is moving
- `out_for_delivery`: Arriving today
- `delivered`: Successfully delivered
- `exception`: Problem occurred
- `not_found`: Carrier has no record
- `expired`: Tracking expired

---

### 4. Batch Create (Multiple Shipments)

**Scenario**: You process multiple orders and want to track all of them at once.

**Request**:
```bash
curl -X POST https://api.mailit.com/api/v1/trackings \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_abc123def456" \
  -d '{
    "shipments": [
      {
        "trackingNumber": "JQ048740244IN",
        "courier": "india-post",
        "orderId": "ORD-12345"
      },
      {
        "trackingNumber": "UB209300714LV",
        "courier": "usps",
        "orderId": "ORD-12346"
      },
      {
        "trackingNumber": "LX123456789CN",
        "courier": "china-post",
        "orderId": "ORD-12347"
      }
    ]
  }'
```

**Response (Partial Success)**:
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
  "failed": [
    {
      "trackingNumber": "LX123456789CN",
      "courier": "china-post",
      "error": "Invalid tracking number format"
    }
  ]
}
```

**Note**: Batch requests support up to 40 shipments. If any fail validation, the others are still created.

---

### 5. Get Detailed Tracking Info

**Scenario**: Get full details for a specific tracking.

**Request**:
```bash
curl -X GET "https://api.mailit.com/api/v1/trackings/trk_9f3a2b8c" \
  -H "X-API-Key: sk_live_abc123def456"
```

**Response**:
```json
{
  "trackingId": "trk_9f3a2b8c",
  "trackingNumber": "JQ048740244IN",
  "courier": "india-post",
  "status": "delivered",
  "lastEvent": "Delivered to recipient",
  "lastUpdated": "2025-12-18T15:30:00Z",
  "origin": "IN",
  "destination": "US",
  "orderId": "ORD-12345",
  "createdAt": "2025-12-15T08:00:00Z"
}
```

---

### 6. Filter by Status

**Scenario**: Find all delivered packages.

**Request**:
```bash
curl -X GET "https://api.mailit.com/api/v1/trackings?status=delivered&page=1&limit=100" \
  -H "X-API-Key: sk_live_abc123def456"
```

---

### 7. Delete a Tracking

**Scenario**: You no longer need to monitor a shipment.

**Request**:
```bash
curl -X DELETE "https://api.mailit.com/api/v1/trackings/trk_9f3a2b8c" \
  -H "X-API-Key: sk_live_abc123def456"
```

**Response**:
```json
{
  "success": true,
  "trackingId": "trk_9f3a2b8c",
  "message": "Tracking deleted successfully"
}
```

**Note**: This is a soft delete - the tracking is marked as deleted in our system but can be recovered if needed. Queries will no longer return this tracking.

---

## Error Handling

### Rate Limit Exceeded
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

**Solution**: Wait until `resetAt` or upgrade your plan.

---

### Invalid API Key
```json
{
  "code": "UNAUTHORIZED",
  "message": "Invalid or missing API key"
}
```

**Solution**: Check that your API key is correct and included in the `X-API-Key` header.

---

### Validation Error
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    "shipments[0].trackingNumber: must not be blank",
    "shipments[1].courier: must match pattern ^[a-z0-9-]+$"
  ]
}
```

**Solution**: Fix the validation errors listed in `details`.

---

## Best Practices

### 1. Use Correlation IDs
Include an `X-Request-Id` header for easier debugging:

```bash
curl -H "X-Request-Id: my-order-12345" \
     -H "X-API-Key: sk_live_xxx" \
     https://api.mailit.com/api/v1/trackings
```

If an error occurs, reference this ID when contacting support.

---

### 2. Handle Partial Failures
Always check both `created` and `failed` arrays in batch responses:

```javascript
const response = await fetch('/api/v1/trackings', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-API-Key': process.env.MAILIT_API_KEY
  },
  body: JSON.stringify({ shipments })
});

const result = await response.json();

result.created.forEach(tracking => {
  console.log(`Created: ${tracking.trackingId}`);
});

result.failed.forEach(error => {
  console.error(`Failed: ${error.trackingNumber} - ${error.error}`);
});
```

---

### 3. Implement Retry Logic
If you get a `503 Service Unavailable`, retry with exponential backoff:

```python
import time
import requests

def create_tracking_with_retry(shipments, max_retries=3):
    for attempt in range(max_retries):
        response = requests.post(
            'https://api.mailit.com/api/v1/trackings',
            headers={'X-API-Key': os.getenv('MAILIT_API_KEY')},
            json={'shipments': shipments}
        )
        
        if response.status_code == 503:
            wait_time = 2 ** attempt  # Exponential backoff: 1s, 2s, 4s
            time.sleep(wait_time)
            continue
        
        return response.json()
    
    raise Exception("Service unavailable after retries")
```

---

### 4. Cache Tracking Data
To reduce API calls, cache tracking results for 2-5 minutes:

```javascript
const cache = new Map();

async function getTracking(trackingId) {
  const cached = cache.get(trackingId);
  if (cached && Date.now() - cached.timestamp < 300000) {  // 5 min TTL
    return cached.data;
  }
  
  const response = await fetch(`/api/v1/trackings/${trackingId}`, {
    headers: { 'X-API-Key': process.env.MAILIT_API_KEY }
  });
  
  const data = await response.json();
  cache.set(trackingId, { data, timestamp: Date.now() });
  return data;
}
```

---

## Code Examples

### Node.js (with fetch)
```javascript
const MAILIT_API_KEY = process.env.MAILIT_API_KEY;
const BASE_URL = 'https://api.mailit.com/api/v1';

async function createTracking(trackingNumber, courier, orderId) {
  const response = await fetch(`${BASE_URL}/trackings`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': MAILIT_API_KEY
    },
    body: JSON.stringify({
      shipments: [{ trackingNumber, courier, orderId }]
    })
  });
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  
  return response.json();
}

// Usage
const result = await createTracking('JQ048740244IN', 'india-post', 'ORD-12345');
console.log('Tracking ID:', result.created[0].trackingId);
```

---

### Python (with requests)
```python
import os
import requests

MAILIT_API_KEY = os.getenv('MAILIT_API_KEY')
BASE_URL = 'https://api.mailit.com/api/v1'

def create_tracking(tracking_number, courier, order_id):
    response = requests.post(
        f'{BASE_URL}/trackings',
        headers={
            'Content-Type': 'application/json',
            'X-API-Key': MAILIT_API_KEY
        },
        json={
            'shipments': [{
                'trackingNumber': tracking_number,
                'courier': courier,
                'orderId': order_id
            }]
        }
    )
    response.raise_for_status()
    return response.json()

# Usage
result = create_tracking('JQ048740244IN', 'india-post', 'ORD-12345')
print('Tracking ID:', result['created'][0]['trackingId'])
```

---

### Java (with Spring Boot RestClient)
```java
@Service
public class MailItClient {
    
    private final RestClient restClient;
    
    public MailItClient(@Value("${mailit.api.key}") String apiKey) {
        this.restClient = RestClient.builder()
            .baseUrl("https://api.mailit.com/api/v1")
            .defaultHeader("X-API-Key", apiKey)
            .build();
    }
    
    public BatchCreateResponse createTracking(String trackingNumber, String courier, String orderId) {
        CreateTrackingRequest request = new CreateTrackingRequest(
            List.of(new ShipmentDto(trackingNumber, courier, orderId, null, null))
        );
        
        return restClient.post()
            .uri("/trackings")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(BatchCreateResponse.class);
    }
}
```

---

## Rate Limits

| Plan | Requests/Day | Trackings/Batch | Cost |
|------|--------------|-----------------|------|
| Free | 100 | 10 | $0 |
| Starter | 1,000 | 40 | $29/month |
| Pro | 10,000 | 40 | $99/month |
| Enterprise | Unlimited | 40 | Custom |

To upgrade your plan, contact [support@mailit.com](mailto:support@mailit.com).

---

## Support

- **API Documentation**: https://api.mailit.com/docs (Swagger UI)
- **Email**: support@mailit.com
- **Status Page**: https://status.mailit.com

When contacting support, include:
- Your API key prefix (e.g., `sk_live_ab12cd`)
- Correlation ID from error response
- Request timestamp

---

## Next Steps

1. **Test in Sandbox**: Use `sk_test_xxx` keys with `https://sandbox.mailit.com/api/v1`
2. **Set Up Webhooks** (Phase 3): Get notified when tracking status changes
3. **Monitor Usage**: View API usage dashboard at https://dashboard.mailit.com
4. **Upgrade Plan**: Scale up as your tracking volume grows

Happy tracking! 🚀
