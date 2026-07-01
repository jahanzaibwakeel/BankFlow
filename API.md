# API

All normal responses use:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "requestId": "uuid",
  "timestamp": "2026-06-30T00:00:00Z"
}
```

Errors use:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INSUFFICIENT_BALANCE",
    "message": "Insufficient balance",
    "fields": {}
  },
  "requestId": "uuid",
  "timestamp": "2026-06-30T00:00:00Z"
}
```

Paginated responses return `items`, `page`, `size`, `totalElements`, `totalPages`, and `last`.

## Example Flow

```bash
LOGIN=$(curl -s http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"customer@bankflow.dev","password":"Customer123!"}')

TOKEN=$(echo "$LOGIN" | jq -r '.data.accessToken')
REFRESH_TOKEN=$(echo "$LOGIN" | jq -r '.data.refreshToken')

curl http://localhost:8080/api/accounts \
  -H "Authorization: Bearer $TOKEN"

curl -X POST http://localhost:8080/api/transfers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: demo-001" \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","destinationAccountId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb","amount":"10.00","description":"Savings move"}'

curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

Swagger UI is available at `/swagger-ui.html`.

## Admin Reconciliation

```bash
curl http://localhost:8080/api/admin/reconciliation \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

The reconciliation report verifies account balances against ledger-derived balances and checks internal transfers for equal debit/credit totals.
