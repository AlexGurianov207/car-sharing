# Deployment

## Docker Compose

Docker Compose starts both the application and PostgreSQL:

```powershell
docker compose up --build
```

Stop containers:

```powershell
docker compose down
```

Application URL:

```text
http://localhost:8082/
```

Healthcheck:

```text
http://localhost:8082/actuator/health
```

Services:

- `app` - Spring Boot application
- `db` - PostgreSQL 16

Useful checks:

```powershell
docker compose ps
docker logs carsharing-app
docker logs carsharing-db
```

## Environment Variables

For Docker Compose, you can create a local `.env` file if you want to override defaults:

```text
SERVER_PORT=8082
POSTGRES_DB=car_sharing
DB_USERNAME=postgres
DB_PASSWORD=postgres
ADMIN_LOGIN=admin
ADMIN_PASSWORD=Admin123!
USER_INITIAL_CREDENTIAL=user-initial-credential
SPRING_PROFILES_ACTIVE=docker
```

For Render deployment, `render.yaml` creates PostgreSQL and passes database values to the app.
Set secret values in Render for:

```text
ADMIN_PASSWORD=<admin-password>
USER_INITIAL_CREDENTIAL=<initial-user-password>
```

If the platform provides a `PORT` variable, the application will use it automatically.

## CI/CD

GitHub Actions workflows:

- `.github/workflows/sonar.yml` runs Maven verify and SonarCloud analysis.
- `.github/workflows/ci-cd.yml` runs build, tests, deploy to Render, and healthcheck.

To enable deployment from GitHub Actions, add repository secrets:

```text
RENDER_DEPLOY_HOOK_URL=<Render deploy hook URL>
RENDER_HEALTHCHECK_URL=<deployed app health URL, for example https://app.example.com/actuator/health>
```

If these secrets are not configured, the deploy job fails after build and tests.
