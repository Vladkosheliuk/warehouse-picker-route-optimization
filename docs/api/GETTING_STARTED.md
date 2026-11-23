# Getting Started — Backend and API

## 1. Prerequisites

- Java Development Kit (JDK) 21
- Apache Maven, or the project's Maven wrapper if included
- PostgreSQL 17 (local Docker Compose configuration or a reachable PostgreSQL/Supabase instance)
- IntelliJ IDEA (optional)
- Node.js/npm (only for optional OpenAPI linting with Redocly)

## 2. Configure the database

For database roles, migrations, and initial test data, follow the [Database Setup](../database/DATABASE_SETUP.md). A fresh database does not automatically contain picking task `1`.

The backend reads its database credentials from environment variables (`backend/src/main/resources/application.yaml`):

| Variable | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL, e.g. `jdbc:postgresql://localhost:5432/warehouse_picker` |
| `DB_USERNAME` | Application database user |
| `DB_PASSWORD` | Application database password |
| `FLYWAY_USERNAME` | User for running/validating Flyway migrations |
| `FLYWAY_PASSWORD` | Flyway user password |
| `INITIAL_MANAGER_USERNAME` | Optional initial manager username |
| `INITIAL_MANAGER_PASSWORD` | Optional initial manager password (12+ characters) |
| `INITIAL_MANAGER_FULL_NAME` | Optional initial manager display name |

Use **placeholder or rotated** credentials in any published examples. Never store real values in source control. The initial manager is created on startup only when all three `INITIAL_MANAGER_*` variables are supplied and no user with the `MANAGER` role exists; changing these variables will **not** reset the password of an existing manager.

### Local PostgreSQL

If your repository contains the provided Docker Compose file and its required `.env` variables (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`), start PostgreSQL with:

```powershell
docker compose up -d postgres
```

For local Docker, use `DB_URL=jdbc:postgresql://localhost:5432/warehouse_picker` with the corresponding local database account. `FLYWAY_USERNAME`/`FLYWAY_PASSWORD` must have permission to create/validate the schema. Do not apply both local and Supabase variables at the same time without checking which DB is selected.

### Hosted PostgreSQL / Supabase

Use the project-specific JDBC URL and credentials delivered through secure configuration. SSL requirements depend on the connection endpoint. **Never publish the actual credentials.**

## 3. Start the backend

In IntelliJ IDEA, open the root repository, select `BackendApplication` in the `backend` module, choose JDK 21 and set the environment variables under **Run > Edit Configurations > Environment variables**. Then run it.

Alternatively, in a PowerShell session whose environment variables are already configured:

```powershell
mvn -f backend/pom.xml spring-boot:run
```

The backend listens at port 8080. At startup, Flyway validates and applies pending V1–V5 migrations; Hibernate is configured with `ddl-auto: validate`.

## 4. Verify the service

```powershell
curl.exe -i http://localhost:8080/actuator/health
curl.exe -i -X POST http://localhost:8080/api/routes/calculate/1
```

Expect HTTP `200` with a health response for the first request if healthy, and HTTP `401` for the unauthenticated route request. This checks authentication enforcement, not successful route calculation.

Open the interactive API documentation:

- http://localhost:8080/swagger-ui.html
- http://localhost:8080/v3/api-docs
- http://localhost:8080/v3/api-docs.yaml

The Swagger **Authorize** dialog uses HTTP Basic authentication. Enter a valid MANAGER account for warehouse CRUD operations.

## 5. Reproduce a route comparison

The API assumes existing database records. To calculate task `1`, its warehouse must have an entrance and an exit node, connected paths, task items, product weights, and sufficient stock in storage locations. The available demonstration database previously returned:

- `POST /api/routes/calculate/1` → HTTP `200`
- `optimized.totalDistanceM = 80`, `optimized.loadDistanceKgM = 249`
- `sShape.totalDistanceM = 80`, `sShape.loadDistanceKgM = 249`

These values are **observations from a particular dataset**, not hardcoded output or a guaranteed result of a fresh installation. See [Integration Tutorial](INTEGRATION_TUTORIAL.md) for a step-by-step scenario.

## 6. Test and validate documentation

```powershell
mvn -f backend/pom.xml clean verify
Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs.yaml" -OutFile "docs/api/openapi.yaml"
npx @redocly/cli@latest lint docs/api/openapi.yaml
```

`mvn ... verify` creates a JaCoCo report only after the JaCoCo plugin is configured and the build succeeds. The local Java project has been shown with JaCoCo configured, but verify the committed `backend/pom.xml` before relying on this path. When the configured JaCoCo report goal runs successfully, find the generated report at `backend/target/site/jacoco/index.html`. The project still requires verification of ≥70% coverage and integration/CI tests. The linter may report warnings; resolve or document them rather than claiming an unverified clean result.

## 7. Troubleshooting

| Symptom | Likely cause or next action |
|---|---|
| `401 Unauthorized` | Missing/wrong Basic credentials, or a protected endpoint |
| `403 Forbidden` | Authenticated user lacks required role |
| `404 Picking task not found` | The task ID does not exist in the selected database |
| `409 Conflict` during route calculation | Missing entrance/exit, stock shortage or other invalid warehouse state |
| Cannot connect to DB | Check DB host, port, SSL settings, environment variables, Docker and network |
| `Ambiguous mapping` on startup | Two controllers declare the same HTTP method/path; remove the duplicate |
| `/v3/api-docs.yaml` returns 401 | Permit this exact path in Spring Security (in addition to `/v3/api-docs` and `/v3/api-docs/**`) |
| Browser downloads the YAML file | This is normal; save it as `docs/api/openapi.yaml` |
