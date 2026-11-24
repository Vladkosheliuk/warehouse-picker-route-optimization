# Getting Started — Warehouse Picker Route Optimization API

## Overview

This guide covers installation, database configuration, starting the Java backend, verifying authentication, and accessing the OpenAPI documentation.

For all 21 HTTP operations, request models, permissions, and error responses, see the [API Reference](API_REFERENCE.md).

For a reproducible scenario, see the [Integration Tutorial](INTEGRATION_TUTORIAL.md).

- **Backend:** Java 21, Spring Boot 4.1.2
- **Database:** PostgreSQL 17, Flyway migrations V1–V5
- **Authentication:** Spring Security HTTP Basic
**Local API:** `http://localhost:8080`

## 1. Prerequisites

- JDK 21.
- Maven or the repository's Maven wrapper (`backend/mvnw.cmd`).
- PostgreSQL 17 or a compatible hosted PostgreSQL connection.
- IntelliJ IDEA or another Java development environment.
- Node.js/npm for Redocly validation (optional for backend execution).
- Docker Desktop when using a local Compose database or Testcontainers integration tests.

## 2. Database setup

The database schema uses ten application tables in the `public` schema, managed by five Flyway migrations.

See [Database Setup](../database/DATABASE_SETUP.md), [Database Schema](../database/DATABASE_SCHEMA.md), and [Data Dictionary](../database/DATA_DICTIONARY.md).

Configure these environment variables through IntelliJ IDEA **Run > Edit Configurations > Environment variables**, or in the terminal used to start the backend:

| Variable | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | Restricted application database user |
| `DB_PASSWORD` | Application database password |
| `FLYWAY_USERNAME` | Account allowed to validate and apply schema migrations |
| `FLYWAY_PASSWORD` | Password for the migration account |
| `INITIAL_MANAGER_USERNAME` | Optional initial manager login name |
| `INITIAL_MANAGER_PASSWORD` | Optional initial manager password, at least 12 characters |
| `INITIAL_MANAGER_FULL_NAME` | Optional initial manager display name |

Example placeholder JDBC URL for a local instance:

```text
jdbc:postgresql://localhost:5432/warehouse_picker
```

For Supabase or another remote PostgreSQL provider, use the corresponding provider-specific host, credentials, port, and SSL settings.

Never commit actual credentials.

The initial manager is created only when the necessary initial-manager variables are provided and the database has no existing manager. Changing the variables does not reset an existing manager password.

### Optional local Docker database

When the repository's Compose configuration and `.env` variables are prepared:

```powershell
docker compose up -d postgres
docker compose ps
```

Do not assume that a fresh database contains products or picking tasks; initialize demonstration data separately as described in the database setup guide.

## 3. Start the Application

The platform supports separate backend development and
a unified fullstack execution mode.

### 3.1 Unified Fullstack Build

From the repository root on Windows:

```powershell
cd backend
.\mvnw.cmd -Pfullstack clean verify
```

The Maven fullstack profile automatically installs Node.js,
installs frontend dependencies, builds the React application,
and packages the frontend into the Spring Boot JAR.

The build also runs backend automated tests and verifies
the JaCoCo coverage thresholds.

Docker Desktop must be running for Testcontainers-based
integration tests.

### 3.2 Run the Executable JAR

After a successful fullstack build, execute from `backend/`:

```powershell
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

The required database environment variables must be
available to the Java process.

Application URL:

http://localhost:8080/

The React interface and REST API are served by the same
Spring Boot application on port 8080.

The executable JAR contains the frontend and backend,
but does not contain database credentials.

### 3.3 Backend Development Mode

The Java backend can also be started independently.

From the repository root:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Alternatively, run `BackendApplication` in IntelliJ IDEA
with the required environment variables configured.

The backend API runs at:

http://localhost:8080

A separate React development server can be started using
`npm run dev` inside the `frontend/` directory.

### 3.4 Database and Initial Manager

The application supports local and hosted PostgreSQL
databases configured through environment variables.

Flyway applies the versioned database migrations.

The initial manager is created automatically when all
three INITIAL_MANAGER environment variables are provided
and no MANAGER account already exists in the database.

The administrator can then create additional users through
the authenticated application interface.

Demonstration data can be imported using
`database/seed/01_seed_test_data.sql`.

The demonstration dataset includes sample manager and
picker accounts. Because a manager already exists after
importing this dataset, the initial-manager bootstrap
will not create another manager.

For a new manager account with custom credentials, use
a fresh database before importing the demonstration data.

See [Database Setup](../database/DATABASE_SETUP.md)
for detailed database configuration and security instructions.

## 4. Check service health and authentication

Use a separate PowerShell window:

```powershell
curl.exe -i http://localhost:8080/actuator/health

curl.exe -i http://localhost:8080/api/auth/me
```

When healthy, the first request returns `200 OK` with a health status.

The second request returns `401 Unauthorized` because it lacks credentials.

To authenticate without entering a password directly into the command:

```powershell
curl.exe -u YOUR_MANAGER_USERNAME -i http://localhost:8080/api/auth/me
```

`curl.exe` prompts for the password.

A valid manager account receives a JSON object with `username` and `role: "MANAGER"`.

Roles are `MANAGER` and `PICKER`.

HTTP Basic is the current authentication mechanism; the API does not issue JWT access tokens.

## 5. Explore the API

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml

Swagger UI's **Authorize** control accepts the HTTP Basic username and password.

The API currently includes these groups:

| Group | Operations |
|---|---:|
| Authentication | 1 |
| Warehouse Management | 5 |
| Warehouse Maps | 2 |
| User Management | 2 |
| Product Catalog | 1 |
| Picking Tasks | 9 |
| Route Optimization | 1 |
| **Total** | **21** |

Manager-only endpoints are enforced in Spring Security or backend services. Task ownership checks restrict pickers to their assigned tasks.

See [API Reference](API_REFERENCE.md) for full endpoint descriptions and JSON examples.

## 6. Reproduce a route comparison

A valid calculation requires an existing picking task, warehouse entrance and exit nodes, a connected warehouse graph, product storage locations, product weights, and sufficient stock.

For a task ID returned by your database:

```powershell
curl.exe -u YOUR_MANAGER_USERNAME -i -X POST http://localhost:8080/api/routes/calculate/1
```

Replace `1` with a real task ID.

A successful response contains two results: `optimized` and `sShape`, including `stops`, `totalDistanceM`, `loadDistanceKgM`, and `fullPathNodeIds`.

The calculation does not save these routes in the database.

For the complete manager and picker workflow, follow the [Integration Tutorial](INTEGRATION_TUTORIAL.md).

## 7. Run backend tests

From `backend/`:

```powershell
.\mvnw.cmd clean verify
```

When the configured JaCoCo report goal executes successfully, inspect:

```text
backend/target/site/jacoco/index.html
```

The measured coverage must be checked against the current source code before stating that any required percentage has been achieved.

The latest GitHub Backend Tests workflow should also be reviewed in the repository's Actions tab.

## 8. Export and validate OpenAPI 3.1

With the backend running, switch to the repository root and execute:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs.yaml" -OutFile "docs/api/openapi.yaml"

npx --yes @redocly/cli@latest lint docs/api/openapi.yaml
```

The current local OpenAPI specification was validated successfully with **zero errors and zero warnings** after documentation annotation updates.

Do not edit only the generated YAML for permanent API changes.

Update the Java DTOs, controllers, and OpenAPI configuration, restart the backend, and export the YAML again.

## 9. Common problems

| Symptom | Resolution |
|---|---|
| `401 Unauthorized` | Supply correct HTTP Basic credentials |
| `403 Forbidden` | Use an authorized role and an assigned task when required |
| `404 Not Found` | Check that the specified warehouse, task, picker, or product exists |
| `400 Bad Request` | Check required fields, quantity constraints, and request JSON |
| `409 Conflict` | Check task status, stock availability, duplicate usernames, or repeated map imports |
| Database connection fails | Verify JDBC host, port, credentials, SSL, and database state |
| Flyway startup failure | Use an authorized migration account and check applied migrations |
| `/v3/api-docs.yaml` unavailable | Confirm the backend is running and the endpoint is permitted by Spring Security |
| Empty product catalog | Load seed data or choose a warehouse with positive product stock |
| Route calculation fails | Check task ownership, task state, entrance/exit, connectivity, product locations, and stock |
| Application fails with `${FLYWAY_USERNAME}` authentication error | Define `FLYWAY_USERNAME` and `FLYWAY_PASSWORD` in the environment of the Java process |
| Root URL returns 404 instead of the React interface | Build with `-Pfullstack` and start the resulting JAR; a backend-only development build may not contain React assets |

## 10. Related documentation

- [API Reference](API_REFERENCE.md)
- [Integration Tutorial](INTEGRATION_TUTORIAL.md)
- [OpenAPI Specification](openapi.yaml)
- [Database Setup](../database/DATABASE_SETUP.md)
- [Database Schema](../database/DATABASE_SCHEMA.md)
- [Database Data Dictionary](../database/DATA_DICTIONARY.md)
