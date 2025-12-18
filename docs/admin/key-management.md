# Key Management

## Rotate API Key (Security Incident)

**When to Use**

*   API key leaked
*   Suspicious traffic
*   Scheduled rotation

**HTTP Request**
`POST /admin/clients/{clientId}/rotate-key`

**Result**

*   Old key invalid immediately
*   New key returned once

## Revoke API Key (Permanent)

**HTTP Request**
`DELETE /admin/clients/{clientId}/api-key`

> ⚠️ **Irreversible**
>
> Use only when relationship ends
