# CI/CD

GitHub Actions workflow: `.github/workflows/ci.yml`.

Pipeline steps:

1. Checkout source.
2. Install Java 21 with Maven cache.
3. Run `mvn -B test` and generate JaCoCo coverage.
4. Upload Surefire test reports.
5. Upload JaCoCo coverage report.
6. Build app with `mvn -B -DskipTests package`.
7. Build Docker image with `docker build`.

The workflow is intentionally simple and recruiter-readable, while still validating unit tests, Testcontainers integration tests, application packaging, and image build.
