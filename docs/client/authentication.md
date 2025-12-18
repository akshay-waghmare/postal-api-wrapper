# Authentication

All public APIs require an API key.

## Header

```http
X-API-Key: sk_live_...
```

## Security Notes

*   ⚠️ **Never** expose API keys in frontend code
*   Rotate keys immediately if leaked
*   One API key per client
