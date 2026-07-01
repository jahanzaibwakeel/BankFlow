# Testing

Run everything with:

```bash
mvn test
```

Coverage includes:

- Unit tests for transfer service behavior.
- Unit tests for validation rules.
- PostgreSQL Testcontainers integration tests.
- Idempotency replay/conflict behavior.
- Insufficient balance handling.
- Unauthorized request handling.
- Concurrent withdrawal protection.
- Audit log creation through API flow.
- MockMvc endpoint verification.
- Refresh token rotation and logout revocation.
- Admin ledger reconciliation endpoint.
- JaCoCo coverage report generation.

The integration suite starts PostgreSQL with Testcontainers, runs Flyway migrations, authenticates seeded demo users, and exercises real HTTP requests through Spring Security.

Coverage report:

```bash
mvn test
open target/site/jacoco/index.html
```

In GitHub Actions, Surefire test reports and the JaCoCo HTML report are uploaded as workflow artifacts.
