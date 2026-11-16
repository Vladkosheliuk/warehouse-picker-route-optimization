# Database Setup

**Project:** Warehouse Picker Route Optimization Platform  
**Database:** PostgreSQL 17  
**Database hosting:** Supabase  
**Migration tool:** Flyway  
**Backend:** Java 21, Spring Boot

## 1. Prerequisites

The following tools are required:

- Java 21
- Git
- IntelliJ IDEA or another Java IDE
- PostgreSQL database or Supabase project
- Docker Desktop (optional, for local database deployment)

The project uses PostgreSQL hosted on Supabase as its development database. A local PostgreSQL instance can also be deployed using Docker Compose.

## 2. Clone the Repository

```bash
git clone https://github.com/Vladkosheliuk/warehouse-picker-route-optimization.git
cd warehouse-picker-route-optimization
```

## 3. Database Configuration

### 3.1 Supabase Database

The development database is hosted on Supabase.

To configure a connection:

1. Open the Supabase dashboard.
2. Select the project.
3. Open the Connect section.
4. Select Session Pooler.
5. Copy the database connection parameters.

Configure the following environment variables in the IntelliJ IDEA Run/Debug Configuration:

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | Database application username |
| `DB_PASSWORD` | Database application password |

Example JDBC URL:

```text
jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require
```

The application uses a dedicated database account named `warehouse_app`. For the Supabase session pooler, the username includes the project reference:

```text
warehouse_app.<project-reference>
```

Database credentials must be configured locally and must not be committed to the repository.

### 3.2 Local Database with Docker

A local PostgreSQL instance can be created using the provided `compose.yaml` file.

Copy `.env.example` to `.env` and configure the database credentials:

```env
POSTGRES_DB=warehouse_picker
POSTGRES_USER=warehouse_owner
POSTGRES_PASSWORD=replace_with_your_local_password
```

Start PostgreSQL:

```bash
docker compose up -d
```

Verify that the database container is running:

```bash
docker compose ps
```

Local connection parameters:

| Parameter | Value |
|---|---|
| Host | localhost |
| Port | 5432 |
| Database | warehouse_picker |
| Administrator | warehouse_owner |

The database administrator is used for initial schema creation and security configuration. A restricted application account must be used for normal backend operations.

## 4. Database Migrations

The database schema is managed using Flyway.

Migration files are located in:

```text
backend/src/main/resources/db/migration/
```

| Version | Migration | Description |
|---|---|---|
| V1 | V1__create_warehouse_structure.sql | Creates warehouses, nodes, and edges |
| V2 | V2__create_products.sql | Creates products and product locations |
| V3 | V3__create_users.sql | Creates application users |
| V4 | V4__create_picking_tasks.sql | Creates picking tasks and task items |
| V5 | V5__create_routes.sql | Creates picking routes and route stops |

For initial deployment, Flyway must be executed using a database administrator or migration account with permission to create and modify database objects.

Flyway applies migrations in version order and records their execution in `flyway_schema_history`.

The restricted application account does not have permission to modify the database schema or migration history. Therefore, schema migrations must be executed separately from normal application operations or configured with separate migration credentials.

Verify migration execution:

```sql
SELECT version, description, success
FROM public.flyway_schema_history
ORDER BY installed_rank;
```

All five migrations must have `success = true`.

## 5. Database Security and Roles

The database uses role-based access control to separate administrative operations from normal application access.

| Role | Purpose |
|---|---|
| `postgres` | Supabase database administration and schema migrations |
| `app_read` | Read-only access to application data |
| `app_write` | Read and write access to application data |
| `warehouse_app` | Dedicated backend login account with `app_write` membership |

The database security scripts are located in:

```text
database/security/
```

Execute the scripts using the database administrator after applying the schema migrations.

| Script | Purpose |
|---|---|
| `01_create_roles.sql` | Creates database access roles |
| `02_grant_permissions.sql` | Assigns table, schema, and sequence permissions |
| `04_create_application_user.sql` | Creates the dedicated backend login account |
| `03_verify_permissions.sql` | Verifies database role permissions |

The application account password must be configured separately using a secure, locally supplied value.

### Access Restrictions

- `app_read` can read application data but cannot insert, update, or delete records.
- `app_read` cannot access the `password_hash` column in the `users` table.
- `app_write` can select, insert, update, and delete application records.
- Neither application role can create tables in the `public` schema.
- Application roles cannot modify Flyway migration history.
- `warehouse_app` is not a superuser and cannot create databases or roles.
- Application user passwords are stored as password hashes rather than plaintext passwords.

Database permissions must be reviewed whenever new tables or other database objects are introduced.

## 6. Test Data

The test data script is located in:

```text
database/seed/01_seed_test_data.sql
```

Execute the script after the database schema has been created.

The script populates all ten application tables with demonstration data.

| Table | Records |
|---|---:|
| warehouses | 1 |
| warehouse_nodes | 12 |
| warehouse_edges | 17 |
| products | 6 |
| product_locations | 6 |
| users | 3 |
| picking_tasks | 3 |
| picking_task_items | 8 |
| picking_routes | 2 |
| route_stops | 22 |
| **Total** | **80** |

The demonstration dataset includes:

- A warehouse navigation graph with entrances, exits, intersections, and storage locations.
- Products with different weights and stock quantities.
- Manager and picker accounts.
- Picking tasks with different statuses.
- Two example routes for comparing distance and accumulated load-distance cost.

The example routes are predefined demonstration data and do not represent measured results from the final Java optimization algorithm.

The seed script should be executed on an empty application dataset. It should not be executed repeatedly against an already populated database.

## 7. Database Testing

Database tests are located in:

```text
database/tests/
```

### 7.1 Constraint Testing

Execute:

```text
01_test_constraints.sql
```

The script verifies that the database rejects invalid records.

| Test | Expected result | Verified result |
|---|---|---|
| Negative product weight | Rejected by CHECK | Passed |
| Duplicate product SKU | Rejected by UNIQUE | Passed |
| NULL product name | Rejected by NOT NULL | Passed |
| Invalid product reference | Rejected by FOREIGN KEY | Passed |
| Negative stock quantity | Rejected by CHECK | Passed |

### 7.2 Transaction Testing

Execute:

```text
02_test_transactions.sql
```

The transaction test creates a picking task, inserts a valid task item, and attempts to insert an invalid item.

The invalid operation causes the transaction block to roll back.

The test verifies that neither the picking task nor its valid item remains in the database after the failure.

**Verified result:** Passed. The picking task and its items were rolled back successfully.

### 7.3 Permission Testing

Execute:

```text
database/security/03_verify_permissions.sql
```

The permission tests verify:

- Database role attributes.
- Read-only permissions.
- Application write permissions.
- Password hash protection.
- Schema creation restrictions.
- Flyway migration history protection.

All six permission checks were successfully verified against the Supabase development database.

## 8. Running the Backend

After the database schema, security roles, and test data have been configured:

1. Open the `backend` module in IntelliJ IDEA.
2. Select Java 21.
3. Configure the database environment variables.
4. Configure Flyway to use separate migration credentials or disable automatic migration execution for normal application startup.
5. Run `BackendApplication`.

Main application class:

```text
com.warehouse.picker.BackendApplication
```

The backend uses port `8080`.

The backend database connection was verified using the dedicated `warehouse_app` account. PostgreSQL activity monitoring confirmed JDBC connections established under this restricted account.

## 9. Related Documentation

- [Database Schema](DATABASE_SCHEMA.md)
- [Data Dictionary](DATA_DICTIONARY.md)
- [Logical Database Diagram](database_schema.png)
- [Flyway Migrations](../backend/src/main/resources/db/migration/)
- [Database Security Scripts](../database/security/)
- [Test Data](../database/seed/)
- [Database Tests](../database/tests/)
- [Project README](../README.md)