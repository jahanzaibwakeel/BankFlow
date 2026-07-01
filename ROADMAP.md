# BankFlow Upgrade Roadmap

This roadmap breaks the remaining polish into practical chunks. The goal is to keep each phase small enough to finish, verify, and commit cleanly.

## Phase 1: Repo Readiness And Learning Support

Status: complete.

- Add `.gitignore` so local learning notes, resume notes, secrets, and build outputs are not pushed.
- Keep `resume_bullets.md` local-only.
- Create a local-only Java backend learning guide using BankFlow as the reference project.
- Configure GitHub remote and push the project source/docs/devops assets.
- Keep `COMPLETION_AUDIT.md` honest about verification limits.

## Phase 2: Build Verification And Developer Experience

Status: in progress.

This round:

- Add CI artifacts for JaCoCo coverage and test reports.
- Add a Postman collection in addition to the existing `.http` file.
- Add Java/Maven setup documentation for Windows.
- Keep the local Java backend learning file updated with this round's additions.

Left for next round:

- Install/fix Java 21 and Maven on the local machine.
- Run `mvn test`, `mvn -DskipTests package`, Docker build, and Compose startup.
- Fix any compile or test failures found by the real build.
- Add Maven Wrapper after Maven is available.
- Add screenshots or status badges once CI is green.

## Phase 3: Distributed Production Hardening

Status: later.

- Add Redis-backed distributed rate limiting.
- Optionally move idempotency fast-path/cache metadata to Redis while keeping PostgreSQL as the source of truth.
- Expand admin review with status filters, amount thresholds, pending review queues, and approval/rejection reporting.
- Add reconciliation alerting or scheduled reconciliation reports.
- Add operational dashboards/examples for metrics and health probes.

## What Is Already Complete

- Spring Boot API layers, JWT auth, BCrypt passwords, roles, accounts, deposits, withdrawals, transfers, ledger entries, audit logs.
- PostgreSQL/Flyway schema, constraints, indexes, and demo data.
- Idempotency keys, pessimistic locking, refresh token rotation, logout/session revocation.
- Admin audit logs, account status updates, transfer review, ledger reconciliation.
- Docker, Compose, GitHub Actions CI, OpenAPI, Actuator, tests, and documentation.
