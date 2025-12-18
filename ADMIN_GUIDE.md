# MailIt Wrapper - Administrator Guide

This guide outlines the operational procedures for managing API clients, issuing keys, and handling administrative tasks for the MailIt Postal Wrapper API.

> **⚠️ SECURITY WARNING**: The Admin API (`/admin/*`) is **internal only**. It is not secured by API keys and should be protected by network policies (e.g., VPN, private subnet, or firewall rules) to prevent unauthorized access.

---

## Client Onboarding Workflow

### 1. Receive Client Request
When a new user (merchant or platform) wants access to the API, collect the following information:
- **Client Name**: The name of the business or application (e.g., "Acme Corp").
- **Plan Tier**: The desired rate limit plan (`FREE`, `STARTER`, `PRO`, `ENTERPRISE`).
- **Contact Email**: (Optional) For internal records.

### 2. Issue API Key
Use the `createClient` endpoint to generate a new API key.

**Request**:
```bash
curl -X POST http://localhost:8080/admin/clients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corp",
    "plan": "STARTER"
  }'
```

**Response**:
```json
{
  "clientId": 101,
  "name": "Acme Corp",
  "apiKeyPrefix": "sk_live_acme",
  "apiKey": "sk_live_acme_a1b2c3d4e5f6...",
  "plan": "STARTER"
}
```

### 3. Securely Transmit Key
Copy the `apiKey` from the response.
> **CRITICAL**: This is the **ONLY** time the full API key is visible. It is hashed in the database and cannot be retrieved later.

**Transmission Protocol**:
1.  Send the **Client ID** and **API Key Prefix** via email/chat.
2.  Send the **Full API Key** via a secure channel (e.g., One-Time Secret, Password Manager share, or encrypted message).
3.  Instruct the user to store the key securely in their environment variables.

---

## API Reference (Admin)

### Create Client
Generate a new client identity and API key.

- **URL**: `POST /admin/clients`
- **Body**:
  ```json
  {
    "name": "Client Name",
    "plan": "FREE" 
  }
  ```
  *Plans: `FREE`, `STARTER`, `PRO`, `ENTERPRISE`*

### Rotate API Key
If a client's key is compromised, generate a new one. The old key will be invalidated immediately.

- **URL**: `POST /admin/clients/{clientId}/rotate-key`
- **Response**: Returns the new `apiKey`.

### Update Plan
Change a client's rate limit tier.

- **URL**: `PATCH /admin/clients/{clientId}/plan`
- **Body**:
  ```json
  {
    "plan": "PRO"
  }
  ```

### Revoke Access
Permanently disable a client's API key.

- **URL**: `DELETE /admin/clients/{clientId}/api-key`

---

## Operational Notes

### Rate Limits by Plan
- **FREE**: 60 req/min
- **STARTER**: 120 req/min
- **PRO**: 300 req/min
- **ENTERPRISE**: 600 req/min

### Database Management
Clients are stored in the `clients` table.
- **Soft Deletes**: Currently, the API supports revoking keys (clearing the hash). To fully delete a client, database access is required.
