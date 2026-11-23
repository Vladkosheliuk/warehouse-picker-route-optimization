# Warehouse Picker Route Optimization Platform

An academic warehouse order-picking platform for planning and comparing collection routes. The warehouse layout is represented as a graph of entrances, exits, intersections and storage locations. Route calculations account for walking distance, product locations, required quantities and collected product weight.

## Project scope

The platform is intended to help warehouse managers organize picking tasks and order pickers follow a recommended collection sequence. The project evaluates a weight-aware routing heuristic against an S-shape baseline using simulated warehouse layouts.

### Implemented backend functionality

- Warehouse CRUD REST API with role-based access control.
- Dijkstra shortest-path calculation for weighted, directional warehouse edges.
- Weight-aware greedy picking-route calculation and coordinate-based S-shape comparison.
- Distance, carried-load cost (kg·m), ordered picking stops and full node-path results.
- Spring Security HTTP Basic authentication, validation and centralized handling of selected application errors.
- OpenAPI 3.1 / Swagger UI documentation and Java unit tests.

The route-calculation endpoint currently **returns results without saving them to the database**. The routing heuristic does not guarantee that every generated route improves on the baseline.

### Planned or incomplete functionality

- React SPA and interactive warehouse visualization.
- Picking-task, inventory and picker-account management endpoints.
- Manual picking-progress updates and route analytics dashboards.
- JWT-based authentication (HTTP Basic is currently used).
- Database integration tests, verified JaCoCo ≥70% coverage, CI and external production deployment.

The database contains schema support for several of these features, but a database table alone does not mean the corresponding API is implemented.

## Technology stack

| Component | Technology | State |
|---|---|---|
| Backend | Java 21, Spring Boot 4.1.2, Spring Web MVC | Implemented locally |
| Persistence | PostgreSQL 17, Spring Data JPA / Hibernate, Flyway V1–V5 | Implemented locally |
| Security | Spring Security HTTP Basic / BCrypt | Implemented locally |
| API documentation | springdoc-openapi / Swagger UI / OpenAPI 3.1 | Implemented locally |
| Tests | JUnit 5, Mockito, JaCoCo | Unit tests written; coverage to verify |
| Frontend | React SPA | Planned |
| Containerization | Docker Compose (local PostgreSQL) | Available |

## Run the backend

### Prerequisites

- JDK 21; Apache Maven or the Maven wrapper in `backend/`.
- A local PostgreSQL database or a configured Supabase PostgreSQL connection.
- Database access credentials and Flyway migration credentials supplied through environment variables (never commit real passwords).

See the [Backend and API Getting Started guide](docs/api/GETTING_STARTED.md) and the [Database Setup guide](docs/database/DATABASE_SETUP.md) for the full configuration procedure.

From the repository root, with environment variables configured:

```powershell
mvn -f backend/pom.xml spring-boot:run
```

When the backend is running:

- Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml

The local Swagger interface documents the six current operations: five warehouse CRUD endpoints and `POST /api/routes/calculate/{taskId}`. Warehouse CRUD requires `MANAGER`; route calculation permits `MANAGER` and `PICKER`. Per-task authorization for a picker is not yet implemented.

## API documentation

- [OpenAPI specification](docs/api/openapi.yaml) — exported from the running backend; refresh it after changing API annotations.
- [API reference](docs/api/API_REFERENCE.md) — endpoints, request/response examples, data models and error codes.
- [Getting Started](docs/api/GETTING_STARTED.md) — database and backend setup.
- [Integration tutorial](docs/api/INTEGRATION_TUTORIAL.md) — a reproducible PowerShell workflow.
- [Example requests](docs/api/examples/) — JSON payloads for creating and updating a disposable warehouse.

Export the local specification while the backend is running:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs.yaml" -OutFile "docs/api/openapi.yaml"
```

Validate the exported file (requires Node.js/npm):

```powershell
npx @redocly/cli@latest lint docs/api/openapi.yaml
```

A generated file is not the same as a validated file; review and resolve the validator output before submitting.

## Tests

From the repository root:

```powershell
mvn -f backend/pom.xml clean verify
```

If the build and JaCoCo report generation succeed, open `backend/target/site/jacoco/index.html`. The required 70% coverage target has not been verified and must not be reported as achieved without the generated report. Integration testing and CI are still outstanding.

## Database documentation

- [Database setup](docs/database/DATABASE_SETUP.md)
- [Database schema](docs/database/DATABASE_SCHEMA.md)
- [Data dictionary](docs/database/DATA_DICTIONARY.md)
- [Logical database diagram](docs/database/database_schema.png)
- [Flyway migrations](backend/src/main/resources/db/migration/)
- [Database security scripts](database/security/)
- [Test data](database/seed/)
- [Database tests](database/tests/)

## Evaluation note

In one demonstrated test task, both algorithms returned 80 m of travel distance and 249 kg·m of load-distance cost. This is a successful functional demonstration, **not evidence of an improvement**. Further controlled scenarios are required to evaluate the target reductions of 10% and 5%.

## Security

Before providing reviewers access, replace credentials that have been disclosed, verify that the repository and its Git history contain no secrets, and use test data rather than private operational data. A public repository link is not a substitute for a published, externally accessible backend service.
