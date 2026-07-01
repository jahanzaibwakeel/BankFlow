# Troubleshooting

## Maven not found

Install Maven 3.9+ or use a Java IDE with Maven support.

## Docker Compose port conflict

Change the host port in `docker-compose.yml` if `55432` or `8080` is already in use.

## Login fails for demo users

Recreate the database volume so Flyway can reseed:

```bash
docker compose down -v
docker compose up --build
```

## Testcontainers fails

Ensure Docker Desktop is running and your user can run Docker commands.

## JWT errors

Set `JWT_SECRET` to a long random value. All running instances in the same environment must share the same secret.
