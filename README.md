# BankFlow API

Production-style Java 21/Spring Boot 3 banking backend with JWT auth, customer accounts, deposits, withdrawals, internal transfers, double-entry ledger entries, idempotency keys, audit logs, admin review workflows, PostgreSQL/Flyway, Docker, CI, and tests.

## GitHub Repository Description

Secure Spring Boot 3 banking and double-entry ledger API with JWT auth, PostgreSQL/Flyway, idempotent transfers, row-level concurrency protection, audit logs, Testcontainers, Docker, and CI/CD.

## Quick Start

```bash
cp .env.example .env
docker compose up --build
```

API: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
Health: `http://localhost:8080/actuator/health/readiness`

## Demo Credentials

Customer: `customer@bankflow.dev` / `Customer123!`  
Admin: `admin@bankflow.dev` / `Admin123!`

## Local Commands

```bash
mvn test
mvn -DskipTests package
docker build -t bankflow-api:local .
docker compose up --build
```

## Main Endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/logout-all`
- `POST /api/accounts`
- `GET /api/accounts`
- `GET /api/accounts/{id}`
- `POST /api/accounts/{id}/deposit`
- `POST /api/accounts/{id}/withdraw`
- `POST /api/transfers` with `Idempotency-Key`
- `GET /api/transfers`
- `GET /api/accounts/{id}/ledger`
- `GET /api/admin/audit-logs`
- `PATCH /api/admin/transfers/{id}/review`
- `PATCH /api/admin/accounts/{id}/status?status=FROZEN`
- `GET /api/admin/reconciliation`

See [API.md](API.md), [ARCHITECTURE.md](ARCHITECTURE.md), and [TESTING.md](TESTING.md).
See [COMPLETION_AUDIT.md](COMPLETION_AUDIT.md) for the final implementation and verification audit.
See [JAVA_DEVELOPER_SETUP.md](docs/JAVA_DEVELOPER_SETUP.md) for Windows Java/Maven setup and local verification commands.

## API Collections

- REST Client collection: [bankflow-api.http](http/bankflow-api.http)
- Postman collection: [bankflow.postman_collection.json](collections/bankflow.postman_collection.json)

## Known Limitations

Redis is documented as an optional extension but not required for this project; idempotency and rate limiting are implemented locally/persistently with PostgreSQL plus an in-memory request limiter. External payment rails, KYC, statements, and real notification delivery are out of scope.
