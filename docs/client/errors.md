# Error Handling

The API uses standard HTTP status codes to indicate success or failure.

| Status Code | Description |
| :--- | :--- |
| `200 OK` | Request succeeded |
| `201 Created` | Resource created successfully |
| `400 Bad Request` | Invalid request parameters or validation failed |
| `401 Unauthorized` | Missing or invalid API key |
| `403 Forbidden` | Valid API key but insufficient permissions |
| `404 Not Found` | Resource not found |
| `429 Too Many Requests` | Rate limit exceeded |
| `500 Internal Server Error` | Server error |

**Error Response Format**

```json
{
  "timestamp": "2025-12-18T10:15:30",
  "status": 400,
  "message": "Error description",
  "path": "/api/v1/trackings"
}
```
