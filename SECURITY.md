# Security

BankFlow API uses:

- BCrypt password hashing.
- JWT bearer authentication.
- Rotating refresh tokens stored as SHA-256 hashes.
- Logout and logout-all session revocation.
- Scheduled cleanup for expired refresh tokens.
- Role-based authorization with `CUSTOMER` and `ADMIN`.
- Stateless sessions.
- CORS allow-list from environment config.
- Protected admin routes under `/api/admin/**`.
- Centralized error responses that avoid leaking internals.
- Request correlation IDs via `X-Request-Id`.
- Simple in-memory IP/path rate limiting.

Secrets must be provided through environment variables. Do not use the sample `.env.example` values in production.

Demo passwords are seeded only for local/demo use. In PostgreSQL, Flyway uses `pgcrypto` to generate BCrypt password hashes during migration.
