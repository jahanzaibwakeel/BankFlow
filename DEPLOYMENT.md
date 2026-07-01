# Deployment

## Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

The app waits for PostgreSQL health before starting. The app health check uses:

```text
/actuator/health/readiness
```

Compose exposes PostgreSQL on host port `55432` to avoid common local `5432` conflicts.

## Production Notes

- Set a strong `JWT_SECRET`.
- Start production with `SPRING_PROFILES_ACTIVE=prod`.
- Use managed PostgreSQL with backups and PITR.
- Restrict CORS origins.
- Terminate TLS at a reverse proxy or platform ingress.
- Export Actuator metrics to your observability stack.
- Use separate secrets per environment.
- Tune `JWT_REFRESH_TOKEN_DAYS` and `JWT_REFRESH_TOKEN_CLEANUP_CRON` for your session policy.
