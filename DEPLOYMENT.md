# Deployment

## Local Run

Run without PostgreSQL, using the local H2 profile:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-local.ps1
```

Application URL:

```text
http://localhost:8082/
```

Healthcheck:

```text
http://localhost:8082/actuator/health
```

## Docker Compose

Docker Compose starts both the application and PostgreSQL:

```powershell
docker compose up --build
```

Stop containers:

```powershell
docker compose down
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

The main variables are listed in `.env.example`.

For Docker Compose:

```text
SERVER_PORT=8082
POSTGRES_DB=car_sharing
DB_USERNAME=postgres
DB_PASSWORD=postgres
ADMIN_LOGIN=admin
ADMIN_PASSWORD=Admin123!
SPRING_PROFILES_ACTIVE=docker
```

For PaaS deployment, set:

```text
SPRING_PROFILES_ACTIVE=docker
SPRING_DATASOURCE_URL=<managed-postgres-jdbc-url>
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
ADMIN_LOGIN=<admin-login>
ADMIN_PASSWORD=<admin-password>
```

If the platform provides a `PORT` variable, the application will use it automatically.

## CI/CD

GitHub Actions workflows:

- `.github/workflows/sonar.yml` runs Maven verify and SonarCloud analysis.
- `.github/workflows/ci-cd.yml` runs build, tests, Docker build, optional deploy, and healthcheck.

To enable deployment from GitHub Actions, add repository secrets:

```text
DEPLOY_HOOK_URL=<PaaS deploy hook URL>
APP_HEALTH_URL=<deployed app health URL, for example https://app.example.com/actuator/health>
```

If these secrets are not configured, deployment is skipped and build/test/docker-build still run.
