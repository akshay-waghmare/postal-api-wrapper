# MailIt Postal Wrapper - Client User Guide

Welcome to the MailIt Postal Wrapper service. This guide will help you integrate with our tracking system to monitor your shipments across multiple carriers through a single, unified API.

## 1. Introduction

The MailIt Postal Wrapper simplifies shipment tracking by providing a single interface for multiple carriers (like USPS, India Post, China EMS, etc.). Instead of integrating with each carrier individually, you send your tracking numbers to us, and we handle the rest.

**Key Features:**
*   **Unified Statuses**: We map carrier-specific statuses to standard ones (e.g., `InTransit`, `Delivered`, `Exception`).
*   **Batch Processing**: Add or check up to 40 shipments in a single request.
*   **Rate Limiting**: Fair usage policies to ensure system stability.

## 2. Getting Access

To use the API, you need an **API Key**.
*   Contact the MailIt Admin team to request your key.
*   You will be assigned a **Rate Limit Plan** (e.g., Free, Pro, Enterprise) which determines your daily request limits.

**Important**: Keep your API Key secure. Do not share it publicly or commit it to client-side code.

## 3. Authentication

All API requests must include your API Key in the HTTP Header.

**Header Name**: `X-API-Key`
**Value**: `sk_live_...` (your unique key)

**Example (cURL):**
```bash
curl -H "X-API-Key: sk_live_your_key_here" ...
```

## 4. Tracking Shipments

### A. Add Shipments to Track
You can add one or more shipments (up to 40) at a time.

**Endpoint**: `POST /api/v1/trackings`

**Request Body:**
```json
{
  "shipments": [
    {
      "trackingNumber": "9400100000000000000000",
      "courier": "usps"
    },
    {
      "trackingNumber": "LE123456789CN",
      "courier": "china-ems"
    }
  ]
}
```

**Response:**
The system will return a list of successfully created trackings and any that failed (e.g., due to unsupported courier).

### B. Check Shipment Status
To get the latest status of a specific shipment.

**Endpoint**: `GET /api/v1/trackings/{trackingId}`

**Response Example:**
```json
{
  "trackingId": "trk_12345...",
  "trackingNumber": "9400100000000000000000",
  "status": "InTransit",
  "courier": "usps",
  "latestEvent": "Arrived at Regional Facility",
  "lastUpdated": "2023-10-27T10:00:00Z"
}
```

### C. Check Multiple Statuses (Batch)
To get details for up to 40 shipments at once.

**Endpoint**: `POST /api/v1/trackings/batch-get`

**Request Body:**
```json
{
  "trackingIds": ["trk_123...", "trk_456..."]
}
```

## 5. Standard Statuses

We normalize carrier statuses into the following categories:
*   **Pending**: Tracking created, waiting for carrier update.
*   **InfoReceived**: Carrier has received shipping information.
*   **InTransit**: Shipment is on the way.
*   **OutForDelivery**: Shipment is out for delivery.
*   **Delivered**: Successfully delivered.
*   **Exception**: Delivery failed or returned to sender.
*   **Expired**: Tracking stopped after a long period of inactivity.

## 6. Rate Limits

Your API usage is subject to rate limits based on your plan.
*   **Requests per Day**: Total number of API calls allowed.
*   **Trackings per Batch**: Maximum 40 items per batch request.

If you exceed your limit, you will receive a `429 Too Many Requests` error.

## 7. Support

For technical support, API key rotation, or plan upgrades, please contact:
*   **Email**: support@mailit.com
*   **Admin Portal**: [Link to Admin Portal if applicable]

---
*Generated for MailIt Clients - v1.0*
