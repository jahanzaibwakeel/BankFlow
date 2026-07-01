# Database

PostgreSQL is managed with Flyway.

```bash
mvn spring-boot:run
```

Flyway runs automatically at startup.

## Migrations

- `V1__create_bankflow_schema.sql`: users, roles, accounts, transfers, ledger entries, idempotency keys, audit logs, constraints, indexes.
- `V2__seed_demo_data.sql`: demo customer/admin, demo accounts, opening ledger entry, audit seed.
- `V3__add_refresh_tokens.sql`: hashed refresh token storage with revocation and expiry indexes.

## Consistency Rules

- Money columns use `NUMERIC(19,2)`.
- Account balances have a non-negative check constraint.
- Transfer and ledger amounts must be positive.
- Idempotency keys are unique on `(user_id, idempotency_key, operation)`.
- Account row locks protect concurrent deposits, withdrawals, and transfers.
- Ledger rows store `balance_after` for auditability.
- Refresh tokens are stored as hashes, can be revoked, and expire by timestamp.

## Indexes

Indexes exist for `account_id`, `user_id`, transfer account references, `transfer_id`, `created_at`, idempotency creation time, and audit log actor/time.
