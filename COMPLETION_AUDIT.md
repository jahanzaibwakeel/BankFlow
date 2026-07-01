# Completion Audit

## Implemented

- Java 21/Spring Boot 3 Maven project.
- Clean package structure: controller, service, repository, domain, DTO, config, security, exception, observability.
- JWT auth, BCrypt password hashing, `CUSTOMER` and `ADMIN` roles.
- Rotating refresh tokens, logout, and logout-all session revocation.
- Accounts with `ACTIVE`, `FROZEN`, `CLOSED` status.
- Deposits, withdrawals, internal transfers, transfer history, account ledger history.
- Double-entry ledger entries for transfers.
- PostgreSQL-backed idempotency keys for transfer requests.
- Pessimistic account row locking for concurrent money movement.
- Centralized validation and exception handling.
- Consistent success, error, and pagination response formats.
- Admin audit log listing, transfer review, and account status updates.
- Threshold-based high-value transfer review with pending queue, searchable admin filters, approval, and rejection.
- Admin ledger reconciliation for account/ledger drift and unbalanced internal transfers.
- Flyway schema and demo seed migrations with constraints, foreign keys, and indexes.
- Redis-backed distributed rate limiting with in-memory local fallback.
- OpenAPI/Swagger UI.
- Actuator health/readiness/liveness.
- Request correlation IDs and structured log pattern.
- CORS and rate limiting.
- Dockerfile, Docker Compose with PostgreSQL and Redis, `.env.example`, and GitHub Actions CI.
- CI uploads Surefire test reports and JaCoCo coverage reports as workflow artifacts.
- REST Client and Postman API collections.
- Unit tests, MockMvc integration tests, Testcontainers PostgreSQL tests, concurrency test, refresh-token tests, reconciliation tests.
- JaCoCo coverage report generation.
- Complete documentation set and recruiter resume bullets.

## Verification Performed In This Workspace

- Source/package/filesystem audit completed.
- Secret/TODO/floating-point scan completed.
- Docker availability checked.
- `docker compose config` passed.
- Postman collection added for manual endpoint verification once the app is running.
- Maven Wrapper generated and pinned to Maven 3.9.9.
- `mvnw.cmd -version` passed after a Windows wrapper null-path fix.
- `mvn test` passed: 14 tests discovered, 9 executed, 5 Testcontainers tests skipped because Docker is not reachable from the JVM in this environment.
- `mvn -DskipTests package` passed and produced the Spring Boot jar.
- `docker build --progress=plain -t bankflow-api:local .` passed.
- `docker compose up -d --build` passed; PostgreSQL, Redis, and app containers became healthy.
- `GET /actuator/health/readiness` returned `{"status":"UP"}`.
- `POST /api/auth/login` succeeded for the seeded customer.
- `GET /api/accounts` succeeded with the customer JWT and returned the seeded accounts.
- `POST /api/auth/login` succeeded for the seeded admin.
- `GET /api/admin/transfers/review-summary` succeeded with the admin JWT.
- Redis contained `bankflow:rate-limit:*` keys after authenticated smoke requests, confirming the Compose app used Redis rate limiting.

## Verification Blocked By Environment

- `java` is installed at `C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`, but this Codex shell does not inherit it on PATH.
- Host `mvn` is not installed globally; portable Maven was used from `C:\tmp\apache-maven-3.9.9`, and the repo now includes Maven Wrapper.
- Testcontainers cannot access Docker from the JVM in this environment. Docker CLI and Docker Compose work, so integration tests are configured to skip only when Docker is unavailable to Testcontainers.

## Commands To Run On A Java/Docker-Ready Machine

```bash
mvn test
mvn jacoco:report
mvn -DskipTests package
docker build --progress=plain -t bankflow-api:local .
docker compose up --build
curl http://localhost:8080/actuator/health/readiness
```
