# Secure Task Manager

Spring Boot backend for task and project management with JWT authentication, PostgreSQL, Redis, Flyway migrations, OpenAPI documentation, Docker packaging, and GitHub Actions CI.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Flyway
- springdoc OpenAPI / Swagger UI
- JUnit 5
- Testcontainers
- Docker / Docker Compose
- GitHub Actions

## Main modules

- `security`
  - registration, login, refresh, logout
  - JWT filter and token service
  - custom principal and current user access
- `manager.project`
  - project CRUD
  - owner-based access checks
- `manager.task`
  - task CRUD
  - relation to project
- `common`
  - base entity
  - global exception handling
  - OpenAPI and JPA config

## Data model

Current Flyway migrations:

- `V1__create_users_table.sql`
- `V2__create_projects_table.sql`
- `V3__create_tasks_table.sql`

Core relations:

- `User` owns many `Project`
- `Project` has many `Task`
- `Task` may have an assignee `User`

## Requirements

For local development without Docker:

- Java 21
- PostgreSQL
- Redis

For containerized local start:

- Docker
- Docker Compose

## Configuration

Application settings are read from `src/main/resources/application.properties` and may be overridden through environment variables.

Main variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `APP_JWT_SECRET`
- `APP_JWT_EXPIRATION_MS`
- `APP_JWT_REFRESH_EXPIRATION_MS`

Default local values point to:

- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379`

## Run locally

Start the application with Maven wrapper:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Application URL:

- `http://localhost:8080`

Swagger UI:

- `http://localhost:8080/swagger-ui/index.html`

OpenAPI JSON:

- `http://localhost:8080/v3/api-docs`

## Run with Docker Compose

Start the full local stack:

```bash
docker compose up --build
```

This starts:

- app on `http://localhost:8080`
- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379`
- Adminer on `http://localhost:8081`

Stop the stack:

```bash
docker compose down
```

## Tests

The project separates fast tests and integration tests by naming:

- `*Test` -> unit and slice tests
- `*IT` -> integration tests

Run unit and slice tests:

```bash
./mvnw test
```

Run only integration tests:

```bash
./mvnw verify -DskipUnitTests=true
```

Run the full verification pipeline locally:

```bash
./mvnw verify
```

## CI

GitHub Actions workflow is defined in [.github/workflows/ci.yml](.github/workflows/ci.yml).

On `push` to `master` and on `pull_request` to `master`, CI runs:

1. unit and slice tests
2. integration tests
3. application packaging
4. Docker image build

On `push` to `master`, the Docker image is also published to GitHub Container Registry:

- `ghcr.io/<owner>/<repo>`

## Docker image

Local build:

```bash
docker build -t secure-task-manager:local .
```

Run the image manually:

```bash
docker run --rm -p 8080:8080 secure-task-manager:local
```

Manual `docker run` is not enough for normal application startup unless PostgreSQL and Redis are available and configured through environment variables.

## Notes

- Flyway is enabled by default.
- JPA uses `ddl-auto=validate`, so the database schema is expected to come from migrations.
- Refresh tokens are stored through Redis-backed infrastructure.
