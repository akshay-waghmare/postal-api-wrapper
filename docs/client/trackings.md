# Trackings API

## Create Batch Trackings (India Post)

Register multiple India Post consignments for tracking.

**HTTP Request**
`POST /api/v1/trackings`

**Authentication**
`X-API-Key: sk_live_...`

**Request Body**

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `shipments` | array | Yes | Max 40 shipments |
| `shipments[].trackingNumber` | string | Yes | India Post tracking number |
| `shipments[].courier` | string | Yes | Must be `india-post` |
| `shipments[].orderId` | string | No | Client reference |
| `shipments[].originCountry` | string | No | ISO2 code |
| `shipments[].destinationCountry` | string | No | ISO2 code |

**Example Request (India Post)**

```json
{
  "shipments": [
    {
      "trackingNumber": "JQ048740244IN",
      "courier": "india-post",
      "orderId": "ORDER-IND-001",
      "originCountry": "IN",
      "destinationCountry": "IN"
    }
  ]
}
```

**Success Response**

Status: `201 Created`

```json
{
  "success": true,
  "created": [
    {
      "trackingId": "trk_8d92jslq1",
      "trackingNumber": "JQ048740244IN",
      "status": "created"
    }
  ],
  "failed": []
}
```

**Error Example**

```json
{
  "timestamp": "2025-12-18T10:15:30",
  "status": 400,
  "message": "Courier code is required"
}
```

**Notes (India Post)**

*   India Post scans may take time to appear
*   First update may arrive several hours after booking
*   Use batch-get for dashboards

## Get Tracking Details (Single – India Post)

**HTTP Request**
`GET /api/v1/trackings/{trackingId}`

**Example Response (Delivered – India Post)**

```json
{
  "trackingId": "trk_8d92jslq1",
  "trackingNumber": "JQ048740244IN",
  "courierCode": "india-post",
  "status": "DELIVERED",
  "latestEvent": "Item Delivered",
  "latestCheckpointTime": "2025-12-17T15:10:00",
  "createdAt": "2025-12-12T08:30:00",
  "updatedAt": "2025-12-17T15:10:00",
  "events": [
    {
      "date": "2025-12-12T09:10:00",
      "status": "INFO_RECEIVED",
      "description": "Item Booked",
      "location": "Mumbai GPO"
    },
    {
      "date": "2025-12-14T21:45:00",
      "status": "IN_TRANSIT",
      "description": "Item Dispatched",
      "location": "Mumbai NSH"
    },
    {
      "date": "2025-12-17T15:10:00",
      "status": "DELIVERED",
      "description": "Item Delivered",
      "location": "Pune HO"
    }
  ]
}
```

## Get Batch Tracking Details (India Post)

**HTTP Request**
`POST /api/v1/trackings/batch-get`

**Example Request**

```json
{
  "trackingIds": [
    "trk_8d92jslq1",
    "trk_72kajs91p"
  ]
}
```

**Example Response**

```json
[
  {
    "trackingId": "trk_8d92jslq1",
    "trackingNumber": "JQ048740244IN",
    "status": "DELIVERED"
  },
  {
    "trackingId": "trk_72kajs91p",
    "trackingNumber": "JQ048740255IN",
    "status": "IN_TRANSIT"
  }
]
```
