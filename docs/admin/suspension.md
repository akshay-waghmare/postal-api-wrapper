# Client Suspension

## Suspend Client (No Data Loss)

**Use Case**

*   Payment overdue
*   Temporary policy violation

**HTTP Request**
`PATCH /admin/clients/{clientId}/status`

```json
{
  "active": false
}
```

**Result**

*   ✔ All tracking data preserved
*   ✔ Access can be restored instantly
