# Architecture

BankFlow API follows layered Spring Boot architecture:

- `controller`: HTTP mapping, validation, pagination, response envelopes.
- `service`: business rules, transactions, ledger posting, authorization checks.
- `repository`: Spring Data JPA access and pessimistic locking queries.
- `domain`: JPA entities and enums.
- `security`: JWT, user principal, user details.
- `config`: OpenAPI, Spring Security, CORS, rate limiting.
- `observability`: request correlation IDs.

Money movement is handled inside `@Transactional` service methods. Balance-changing operations lock account rows with `PESSIMISTIC_WRITE`, validate account status and ownership, update balances with `BigDecimal`, persist a `Transfer`, and write matching ledger entries.

Transfers are idempotent using the `Idempotency-Key` request header. The key is scoped by user and operation and stores a SHA-256 request hash plus the created transfer id. Replays with the same payload return the original transfer; replays with a different payload are rejected.

Authentication uses short-lived JWT access tokens plus rotating refresh tokens. Refresh tokens are stored only as SHA-256 hashes and are revoked on rotation or logout.

Admins can run ledger reconciliation to compare account balances against ledger-derived balances and detect internal transfers whose debit/credit entries do not net to zero.

Controllers intentionally stay thin. They translate authentication into a user id and delegate all business behavior to services.
