# Automated Testing and Coverage

## 1. Overview

The Warehouse Picker Route Optimization Platform uses automated testing to verify backend business logic, REST API behavior, database integration, warehouse route calculations, and frontend workflows.

The testing strategy combines unit testing, integration testing, component testing, and end-to-end testing.

Automated verification is supported by GitHub Actions.

## 2. Testing Technologies

| Technology | Purpose |
|---|---|
| JUnit 5 | Backend test execution |
| Mockito | Isolation of services and their dependencies |
| Spring Boot Test | Application integration testing |
| MockMvc | HTTP API and security testing |
| Testcontainers | Isolated PostgreSQL test environment |
| PostgreSQL 17 | Integration test database |
| Flyway | Database schema migrations |
| JaCoCo 0.8.14 | Java code coverage analysis |
| Vitest | Frontend component testing |
| React Testing Library | Component behavior verification |
| Playwright | End-to-end browser testing |
| GitHub Actions | Continuous integration |

## 3. Backend Test Organization

Backend tests are located in:

`backend/src/test/java/com/warehouse/picker/`

The test suite is organized according to the corresponding application components.

| Test Class | Tests | Focus |
|---|---:|---|
| RouteOptimizerTest | 6 | Weight-aware route optimization |
| ShortestPathFinderTest | 6 | Graph shortest-path calculations |
| SShapeRoutePlannerTest | 5 | S-shape baseline route planning |
| BackendApplicationTests | 8 | Application context, PostgreSQL, Flyway, API security |
| GlobalExceptionHandlerTest | 4 | Centralized API error handling |
| PickingTaskServiceTest | 29 | Picking task lifecycle and authorization |
| RouteCalculationServiceTest | 7 | Route calculation and inventory allocation |
| WarehouseMapServiceTest | 19 | Warehouse map validation and persistence |
| WarehouseServiceTest | 9 | Warehouse CRUD operations |
| **Total** | **93** | **All tests passed** |

The test suite was verified on November 23, 2026.

## 4. Unit Testing

Unit tests exercise application components independently of external services.

Mockito is used to replace repository dependencies where appropriate.

### 4.1 Route Optimization

Algorithm tests cover graph traversal and route calculation behavior.

The tested components include:

- RouteOptimizer
- ShortestPathFinder
- SShapeRoutePlanner

The tests verify route computation behavior under defined graph and picking scenarios.

### 4.2 Picking Task Management

PickingTaskServiceTest verifies:

- Authentication and role restrictions.
- Manager access to picking tasks.
- Picker access to assigned tasks.
- Task ownership enforcement.
- Starting assigned tasks.
- Preventing invalid task state transitions.
- Recording collected product quantities.
- Preventing excessive collection.
- Completing fully collected tasks.
- Rejecting incomplete tasks.
- Assigning and reassigning pickers.
- Cancelling tasks.
- Route access restrictions.
- Warehouse product availability and quantity aggregation.

The tests use Mockito repositories and do not modify production data.

### 4.3 Warehouse Map Management

WarehouseMapServiceTest verifies:

- Importing valid warehouse graphs.
- Importing directed and bidirectional connections.
- Rejecting imports into populated warehouses.
- Rejecting duplicate node codes.
- Requiring exactly one entrance and one exit.
- Requiring at least one storage location.
- Rejecting unknown edge endpoints.
- Rejecting self-looping edges.
- Rejecting duplicate directed edges.
- Detecting unreachable nodes.
- Requiring paths from warehouse nodes to the exit.
- Reading persisted warehouse maps.
- Handling missing warehouses.

These checks support the integrity of the graph data used by route-planning algorithms.

### 4.4 Other Service Tests

WarehouseServiceTest covers warehouse creation, retrieval, modification, deletion, and missing resources.

RouteCalculationServiceTest covers route calculation prerequisites, stock availability, and integration with the two route-planning strategies.

GlobalExceptionHandlerTest verifies the handling of selected application exceptions.

## 5. Integration Testing

BackendApplicationTests uses Spring Boot Test, MockMvc, and Testcontainers.

The integration test environment starts an isolated PostgreSQL 17 container.

The application connects to the test database, and Flyway applies the five schema migrations.

The integration tests verify:

1. Spring Boot application startup.
2. Connection to the isolated PostgreSQL database.
3. Successful application of Flyway migrations V1–V5.
4. Public access to the health endpoint.
5. Rejection of unauthenticated requests.
6. Manager access through HTTP Basic authentication.
7. Picker role restrictions.
8. HTTP error responses for inaccessible or missing resources.

Test credentials are created for the isolated test database.

The integration suite does not require access to the production database.

## 6. Code Coverage

JaCoCo measures backend test coverage during Maven execution.

### 6.1 Verified Coverage Results

The following results were obtained from the JaCoCo report on November 23, 2026.

| Coverage Metric | Result | Quality Gate |
|---|---:|---:|
| Instruction coverage | 77% | 70% |
| Branch coverage | 71% | 70% |
| Service instruction coverage | 83% | Not separately enforced |
| Service branch coverage | 80% | Not separately enforced |

Both project-wide coverage requirements were satisfied.

The percentages represent measured code coverage, not the percentage of passing tests.

### 6.2 Coverage Quality Gate

The Maven build uses `jacoco-maven-plugin` version `0.8.14`.

The plugin performs three tasks:

1. `prepare-agent` collects coverage data.
2. `report` generates the coverage report.
3. `check` enforces the minimum coverage thresholds.

The quality gate requires:

- Instruction coverage >= 70%.
- Branch coverage >= 70%.

Both limits apply to the backend bundle.

The build fails during Maven `verify` when either limit is not met.

Verified Maven output:

```text
--- jacoco:0.8.14:check (coverage-check) @ backend ---
All coverage checks have been met.
BUILD SUCCESS
```

### 6.3 Coverage Report Location

The HTML report is generated at:

`backend/target/site/jacoco/index.html`

The machine-readable XML report is generated at:

`backend/target/site/jacoco/jacoco.xml`

Coverage reports are generated artifacts and are not required to be committed to the repository.

## 7. Running Backend Tests

### Prerequisites

- Java 21.
- Docker Desktop or another compatible Docker environment.
- Maven Wrapper included in the repository.

From the project root on Windows:

```powershell
cd backend
.\mvnw.cmd clean verify
```

The command compiles the project, executes the tests, creates the application artifact, generates coverage reports, and checks the coverage thresholds.

To open the coverage report:

```powershell
Start-Process "target/site/jacoco/index.html"
```

To execute a specific test class:

```powershell
.\mvnw.cmd "-Dtest=PickingTaskServiceTest" test
```

A successful isolated test execution does not replace the full `clean verify` coverage verification.

### Fullstack Build Verification

The project supports a unified Maven build that packages
the React frontend and Spring Boot backend into one
executable JAR.

The build is executed from the repository root:

```powershell
cd backend
.\mvnw.cmd -Pfullstack clean verify
```

The fullstack build performs:

- Frontend dependency installation.
- React production compilation using Vite.
- Backend compilation and packaging.
- Automated backend test execution.
- JaCoCo coverage verification.

The generated React assets are included in the
Spring Boot executable JAR.

The fullstack build was verified locally on
November 24, 2026.

Verification results:

- React production build successful.
- All 93 backend tests passed.
- JaCoCo coverage thresholds satisfied.
- Frontend assets successfully packaged into the JAR.
- Maven BUILD SUCCESS.

The standard backend CI workflow runs without the
fullstack Maven profile. Frontend tests and production
compilation are verified by the separate Frontend CI workflow.

## 8. Frontend Testing

Frontend testing is organized into component tests and browser-based end-to-end tests.

### 8.1 Component Tests

Vitest and React Testing Library cover three React components.

| Component | Tests | Result |
|---|---:|---|
| ManagerDashboard | 2 | Passed |
| PickerDashboard | 2 | Passed |
| InteractiveWarehouseMap | 3 | Passed |
| **Total** | **7** | **Passed** |

Run from the frontend directory:

```powershell
npm run test
```

These tests verify component rendering, navigation behavior, and map interactions.

### 8.2 End-to-End Tests

Playwright is used to verify browser workflows in Chromium.

Two E2E scenarios have passed in the local development environment:

1. Manager authentication, picking-task navigation, and route calculation.
2. Picker authentication, assigned-task navigation, and role-specific controls.

Run from the frontend directory:

```powershell
npm run test:e2e
```

These tests require a running backend, a configured database, suitable test data, and dedicated manager/picker credentials.

Playwright E2E tests have been verified locally.
A dedicated GitHub Actions workflow is configured to execute them against an isolated PostgreSQL database.

Detailed frontend testing instructions are available in:

[Frontend Testing](../frontend/TESTING.md)

## 9. Continuous Integration

The repository contains separate GitHub Actions workflows for backend tests, frontend component tests, and Playwright
end-to-end testing.

### 9.1 Backend CI

Workflow:

`.github/workflows/backend-tests.yml`

The workflow runs on GitHub Actions using Ubuntu and Java 21.

Its Maven command is:

```bash
./mvnw --batch-mode clean verify
```

The workflow performs:

1. Repository checkout.
2. Java environment setup.
3. Backend test execution.
4. Testcontainers-based integration testing.
5. JaCoCo report generation.
6. JaCoCo coverage quality gate verification.
7. Upload of test and coverage artifacts.

The published backend workflow successfully passed for commit:

`4a691b4`

Commit description:

`Expand backend tests and enforce JaCoCo coverage thresholds`

Verification date: November 23, 2026.

Verified CI output:

```text
All coverage checks have been met.
BUILD SUCCESS
```

GitHub Actions run:

https://github.com/Vladkosheliuk/warehouse-picker-route-optimization/actions/runs/45263297187

### 9.2 CI Artifacts

The backend workflow uploads:

- `jacoco-report`: HTML coverage report.
- `junit-test-results`: Maven Surefire test reports.

These artifacts allow reviewers to inspect the automated test results from the CI run.

### 9.3 Frontend CI

Workflow:

`.github/workflows/frontend-ci.yml`

The workflow executes:

```bash
npm ci
npm run test
npm run build
```

It verifies frontend dependencies, component tests, TypeScript compilation, and the production Vite build.

Playwright E2E tests are configured in a separate
GitHub Actions workflow: `.github/workflows/playwright-e2e.yml`.

### 9.4 Playwright E2E CI

Workflow: `.github/workflows/playwright-e2e.yml`

The workflow runs browser-based tests against a
temporary PostgreSQL 17 database.

Flyway initializes the database schema, followed by
demonstration data and temporary test accounts.

Spring Boot and the React development server are started
automatically before Playwright executes the Chromium tests.

The test environment does not depend on the development
Supabase database.

Browser test reports and failure artifacts are uploaded
to GitHub Actions.

The workflow must complete successfully in GitHub Actions
before CI execution is considered verified.

## 10. Test Isolation and Reproducibility

Automated backend integration tests use a dedicated PostgreSQL container.

Testcontainers provides an isolated database environment for each test application context.

Flyway applies the schema migrations to that environment.

Mockito-based unit tests isolate the tested business logic from database interactions.

The backend CI workflow provides intentionally unusable default database connection variables. Integration tests use the Testcontainers connection instead.

This configuration helps prevent accidental dependence on an external database during CI execution.

No production credentials are required for backend automated tests.

## 11. Verified Results and Limitations

### Verified Results

- 93 backend tests passed.
- Zero backend test failures.
- Zero backend test errors.
- Zero skipped backend tests.
- 77% instruction coverage.
- 71% branch coverage.
- JaCoCo 70% quality gates passed.
- GitHub Actions backend workflow passed.
- Seven frontend component tests passed locally.
- Two Playwright E2E scenarios passed locally.
- Unified React and Spring Boot build verified locally.
- Executable JAR packaging completed successfully.

### Current Limitations

- Playwright E2E GitHub Actions workflow passed successfully.
- Backend Tests, Frontend CI, and Playwright E2E passed for commit `cd9eb4e` on November 24, 2026.
- Coverage percentages may change when application code is modified.
- The JaCoCo quality gate applies to the backend bundle, rather than imposing individual minimums on every package.
- Passing tests do not guarantee the absence of defects.

## 12. Conclusion

The Warehouse Picker Route Optimization Platform uses automated tests to verify route-planning algorithms, warehouse operations, picking task management, API security, and database integration.

The backend achieved coverage above the configured 70% instruction and branch thresholds.

The Maven build enforces these limits automatically, and the successful GitHub Actions run provides independent evidence that the published backend test suite can be executed in CI.
