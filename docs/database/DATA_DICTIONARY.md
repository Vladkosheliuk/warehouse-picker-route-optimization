# Database Data Dictionary

**Project:** Warehouse Picker Route Optimization Platform  
**Database:** PostgreSQL 17  
**Schema:** `public`  
**Migration tool:** Flyway  
**Database version:** V1–V5

## 1. Introduction

This document describes the database structure of the Warehouse Picker Route Optimization Platform.

The database stores warehouse layouts, products, users, picking tasks, generated routes, and route performance metrics.

The database schema is defined through versioned Flyway SQL migration files located at:

`backend/src/main/resources/db/migration/`

The database contains 10 application tables. An additional `flyway_schema_history` table is managed by Flyway.

### Column notation

- **PK** — Primary Key
- **FK** — Foreign Key
- **UQ** — Unique Constraint
- **NN** — NOT NULL
- **NULL** — Nullable column

All `id` primary keys are generated automatically using PostgreSQL identity columns.

---

## 2. Table: `warehouses`

**Purpose:** Stores basic information about warehouses.

**Migration:** V1

| Column | Data type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, NN, Identity | Unique warehouse identifier |
| `name` | VARCHAR(150) | NN | Warehouse name |
| `description` | TEXT | NULL | Optional warehouse description |
| `created_at` | TIMESTAMPTZ | NN, DEFAULT NOW() | Record creation timestamp |

**Primary key:** `id`

**Relationships:** One warehouse can have multiple nodes, edges, and picking tasks.

## 3. Table: `warehouse_nodes`

**Purpose:** Represents locations and navigation points inside a warehouse.

**Migration:** V1

| Column | Data type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, NN, Identity | Unique node identifier |
| `warehouse_id` | BIGINT | FK, NN | Warehouse containing the node |
| `node_code` | VARCHAR(50) | NN | Node code within the warehouse |
| `node_type` | VARCHAR(30) | NN, CHECK | Type of warehouse node |
| `x_coordinate` | NUMERIC(10,2) | NN | X coordinate of the node |
| `y_coordinate` | NUMERIC(10,2) | NN | Y coordinate of the node |

**Primary key:** `id`

**Foreign key:** `warehouse_id` references `warehouses(id)`.

**Unique constraints:**
- `UNIQUE (warehouse_id, node_code)`
- `UNIQUE (id, warehouse_id)`

**Allowed node types:** `ENTRANCE`, `EXIT`, `INTERSECTION`, `STORAGE`.

The composite unique constraint on `(id, warehouse_id)` also supports composite foreign keys from `warehouse_edges`.

## 4. Table: `warehouse_edges`

**Purpose:** Stores connections between warehouse nodes. Together with `warehouse_nodes`, these records form the warehouse navigation graph.

**Migration:** V1

| Column | Data type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, NN, Identity | Unique edge identifier |
| `warehouse_id` | BIGINT | FK, NN | Warehouse containing the connection |
| `from_node_id` | BIGINT | FK, NN | Starting node |
| `to_node_id` | BIGINT | FK, NN | Destination node |
| `distance_m` | NUMERIC(10,2) | NN, CHECK | Connection distance in metres |
| `is_bidirectional` | BOOLEAN | NN, DEFAULT TRUE | Whether travel is allowed in both directions |

**Primary key:** `id`

**Foreign keys:**
- `warehouse_id` → `warehouses(id)`
- `(from_node_id, warehouse_id)` → `warehouse_nodes(id, warehouse_id)`
- `(to_node_id, warehouse_id)` → `warehouse_nodes(id, warehouse_id)`

**Unique constraint:** `(warehouse_id, from_node_id, to_node_id)`.

**Check constraints:**
- `distance_m > 0`
- `from_node_id <> to_node_id`

The composite foreign keys ensure that both connected nodes belong to the specified warehouse.

## 5. Table: `products`

**Purpose:** Stores product information and product weights required for weight-aware route optimization.

**Migration:** V2

| Column | Data type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, NN, Identity | Unique product identifier |
| `sku` | VARCHAR(50) | NN, UQ | Unique stock keeping unit |
| `name` | VARCHAR(200) | NN | Product name |
| `weight_kg` | NUMERIC(10,3) | NN, CHECK | Weight of one product unit in kilograms |

**Primary key:** `id`

**Unique constraint:** `sku`.

**Check constraint:** `weight_kg > 0`.

Product weight is used to calculate the accumulated load during picking.

## 6. Table: `product_locations`

**Purpose:** Records the quantity of each product stored at a particular warehouse node.

**Migration:** V2

| Column | Data type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, NN, Identity | Unique product-location identifier |
| `product_id` | BIGINT | FK, NN | Stored product |
| `node_id` | BIGINT | FK, NN | Storage node |
| `quantity` | INTEGER | NN, DEFAULT 0, CHECK | Available quantity at this location |

**Primary key:** `id`

**Foreign keys:**
- `product_id` → `products(id)`
- `node_id` → `warehouse_nodes(id)`

**Unique constraint:** `(product_id, node_id)`.

**Check constraint:** `quantity >= 0`.

A product can be stored at multiple nodes, and a node can contain multiple products.

## 7. Table: `users`

**Purpose:** Stores application users and their roles.

**Migration:** V3

| Column | Data type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, NN, Identity | Unique user identifier |
| `username` | VARCHAR(100) | NN, UQ, CHECK | Unique login name |
| `password_hash` | VARCHAR(255) | NN, CHECK | Password hash, not plaintext password |
| `full_name` | VARCHAR(150) | NN, CHECK | User's full name |
| `role` | VARCHAR(30) | NN, CHECK | Application user role |
| `is_active` | BOOLEAN | NN, DEFAULT TRUE | Whether the account is active |
| `created_at` | TIMESTAMPTZ | NN, DEFAULT NOW() | Account creation timestamp |

**Primary key:** `id`

**Unique constraint:** `username`.

**Allowed roles:**
- `MANAGER` — manages warehouse data, creates picker
  accounts, creates picking tasks, and assigns tasks.
- `PICKER` — performs assigned picking tasks and
  confirms collected products.

**Check constraints:** `username`, `password_hash`, and `full_name` must contain at least one non-whitespace character.

The database stores password hashes rather than plaintext passwords.

Application roles are independent of PostgreSQL
database roles.

The `app_read` database role cannot access the
`password_hash` column. The backend accesses user
credentials through the restricted `warehouse_app`
database account.

Password hashing and application-level authorization are
implemented using Spring Security.

User passwords are encoded with BCrypt before being stored
in the database.

The application supports HTTP Basic authentication with
two roles: `MANAGER` and `PICKER`.

Authorization is enforced by Spring Security and
backend service-level permission checks.

### Initial Manager Account

The application does not allow public self-registration
because it is intended for internal warehouse operations.

During initial setup, the administrator may configure
the initial manager through environment variables.

If no MANAGER account exists, the backend creates
the first manager using the configured credentials.

The account is stored in the `users` table with the
`MANAGER` role and a BCrypt-encoded password.

Once authenticated, the manager can create additional
MANAGER and PICKER accounts through the user management API.

The initial manager creation is skipped when the
database already contains a MANAGER account.

## 8. Table: `picking_tasks`

**Purpose:** Stores warehouse picking tasks, their assignments, statuses, and timestamps.

**Migration:** V4

| Column | Data type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, NN, Identity | Unique picking task identifier |
| `warehouse_id` | BIGINT | FK, NN | Warehouse where picking takes place |
| `created_by_id` | BIGINT | FK, NN | User who created the task |
| `assigned_picker_id` | BIGINT | FK, NULL | User assigned to collect the products |
| `status` | VARCHAR(30) | NN, DEFAULT 'NEW', CHECK | Current task status |
| `created_at` | TIMESTAMPTZ | NN, DEFAULT NOW() | Task creation timestamp |
| `started_at` | TIMESTAMPTZ | NULL, CHECK | Picking start timestamp |
| `completed_at` | TIMESTAMPTZ | NULL, CHECK | Picking completion timestamp |

**Primary key:** `id`

**Foreign keys:**
- `warehouse_id` → `warehouses(id)`
- `created_by_id` → `users(id)`
- `assigned_picker_id` → `users(id)`

**Allowed statuses:** `NEW`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.

**Date constraints:**
- `started_at` must not be earlier than `created_at`.
- `completed_at`, when provided, requires `started_at` and must not be earlier than `started_at`.

The assigned picker is optional, allowing tasks to exist before assignment.

## 9. Table: `picking_task_items`

**Purpose:** Stores the products and quantities required for each picking task.

**Migration:** V4

| Column | Data type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, NN, Identity | Unique task item identifier |
| `task_id` | BIGINT | FK, NN | Related picking task |
| `product_id` | BIGINT | FK, NN | Required product |
| `required_quantity` | INTEGER | NN, CHECK | Number of units to collect |
| `collected_quantity` | INTEGER | NN, DEFAULT 0, CHECK | Number of units already collected |

**Primary key:** `id`

**Foreign keys:**
- `task_id` → `picking_tasks(id)`
- `product_id` → `products(id)`

**Unique constraint:** `(task_id, product_id)`.

**Check constraints:**
- `required_quantity > 0`
- `collected_quantity >= 0`
- `collected_quantity <= required_quantity`

Each product can appear only once in a particular picking task.

## 10. Table: `picking_routes`

**Purpose:** Stores generated picking routes and performance metrics used to compare routing algorithms.

**Migration:** V5

| Column | Data type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, NN, Identity | Unique route identifier |
| `task_id` | BIGINT | FK, NN | Picking task associated with the route |
| `algorithm` | VARCHAR(30) | NN, CHECK | Algorithm used to generate the route |
| `total_distance_m` | NUMERIC(12,2) | NN, CHECK | Total route distance in metres |
| `load_distance_kg_m` | NUMERIC(16,3) | NN, CHECK | Accumulated weight-distance cost in kg·m |
| `calculation_time_ms` | INTEGER | NN, CHECK | Route calculation time in milliseconds |
| `created_at` | TIMESTAMPTZ | NN, DEFAULT NOW() | Route creation timestamp |

**Primary key:** `id`

**Foreign key:** `task_id` → `picking_tasks(id)`.

**Allowed algorithms:**
- `S_SHAPE` — traditional S-shape baseline
- `OPTIMIZED` — implemented weight-aware route optimization heuristic

**Check constraints:**
- `total_distance_m >= 0`
- `load_distance_kg_m >= 0`
- `calculation_time_ms >= 0`

The load-distance metric represents the accumulated carried weight multiplied by travel distance across route segments.

Multiple route records can be associated with one task, allowing comparison between algorithms.

The database schema supports the persistence of picking
routes and their performance metrics.

The current REST API calculates picking routes and returns
the results directly to the client without automatically
persisting new route records.

The demonstration dataset contains predefined route
records and ordered route stops for database testing.

The implemented weight-aware algorithm is a heuristic
and does not guarantee a globally optimal solution.

## 11. Table: `route_stops`

**Purpose:** Stores the ordered sequence of nodes and optional product collection actions within a picking route.

**Migration:** V5

| Column | Data type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, NN, Identity | Unique route stop identifier |
| `route_id` | BIGINT | FK, NN | Route containing the stop |
| `node_id` | BIGINT | FK, NN | Warehouse node visited |
| `stop_order` | INTEGER | NN, UQ (composite), CHECK | Position of the stop within the route |
| `product_id` | BIGINT | FK, NULL | Product collected at this stop, if applicable |
| `quantity` | INTEGER | NULL, CHECK | Quantity collected at this stop |
| `distance_from_previous_m` | NUMERIC(12,2) | NN, CHECK | Distance travelled from the previous stop |
| `carried_weight_after_kg` | NUMERIC(12,3) | NN, CHECK | Total carried weight after this stop |

**Primary key:** `id`

**Foreign keys:**
- `route_id` → `picking_routes(id)`
- `node_id` → `warehouse_nodes(id)`
- `product_id` → `products(id)`

**Unique constraint:** `(route_id, stop_order)`.

**Check constraints:**
- `stop_order >= 1`
- `distance_from_previous_m >= 0`
- `carried_weight_after_kg >= 0`
- If `product_id` is NULL, `quantity` must also be NULL.
- If `product_id` is provided, `quantity` must be positive.

This structure supports both navigation-only stops and product collection stops.

## 12. Database Relationships

| Parent table | Child table | Relationship |
|---|---|---|
| `warehouses` | `warehouse_nodes` | One-to-many |
| `warehouses` | `warehouse_edges` | One-to-many |
| `warehouses` | `picking_tasks` | One-to-many |
| `warehouse_nodes` | `warehouse_edges` | One-to-many, for each endpoint |
| `warehouse_nodes` | `product_locations` | One-to-many |
| `products` | `product_locations` | One-to-many |
| `users` | `picking_tasks` | One-to-many, as creator |
| `users` | `picking_tasks` | One-to-many, as assigned picker |
| `picking_tasks` | `picking_task_items` | One-to-many |
| `products` | `picking_task_items` | One-to-many |
| `picking_tasks` | `picking_routes` | One-to-many |
| `picking_routes` | `route_stops` | One-to-many |
| `warehouse_nodes` | `route_stops` | One-to-many |
| `products` | `route_stops` | One-to-many, optional |

All relationships listed above are implemented using foreign key constraints.

## 13. Database Integrity

The database uses several types of constraints:

**Primary keys:** Identify each record uniquely.

**Foreign keys:** Maintain relationships between tables and prevent references to nonexistent parent records.

**Unique constraints:** Prevent duplicate usernames, product SKUs, warehouse node codes within a warehouse, and other duplicate combinations.

**Check constraints:** Validate values such as positive product weights, valid user roles, valid task statuses, and non-negative quantities or distances.

**NOT NULL constraints:** Require mandatory data to be provided.

**Default values:** Automatically initialize selected fields, such as creation timestamps, quantities, and task status.

The database uses PostgreSQL identity columns for automatically generated primary key values.

### Data Quality Expectations

The database defines the following data quality requirements to ensure accurate and consistent warehouse operations.

| Data category | Quality requirements |
|---|---|
| Warehouse structure | Warehouse nodes must have valid types and coordinates. Connections must have positive distances and connect nodes within the same warehouse. |
| Products | Each product must have a unique SKU, a valid name, and a positive weight expressed in kilograms. |
| Inventory | Stock quantities must be non-negative. Each product-location combination must be unique. |
| Users | Usernames must be unique. User roles must be valid, and passwords must be stored as secure hashes. |
| Picking tasks | Tasks must reference existing warehouses and users. Task statuses must be valid, and timestamps must follow chronological order. |
| Task items | Required quantities must be positive. Collected quantities must not exceed required quantities. |
| Picking routes | Routes must reference existing tasks. Distance, load-distance cost, and calculation time must be non-negative. |
| Route stops | Stop order must be unique within each route. Product collection stops must contain valid products and positive quantities. |

### Data Quality Validation

Data quality is maintained through two levels of validation:

- **Database-level validation:** Primary keys, foreign keys, unique constraints, NOT NULL constraints, and CHECK constraints prevent invalid or inconsistent records.
- **Application-level validation:** Business rules that involve multiple tables, such as stock availability, user role verification, and warehouse consistency, are validated by the application.

Database constraints are defined in the Flyway SQL migrations. Additional application-level validation is required for business rules that cannot be fully enforced by the current database constraints.

## 14. Flyway Migration History

Flyway manages database schema versions and records migration execution history in `flyway_schema_history`.

The application schema is created by the following migration sequence:

1. `V1__create_warehouse_structure.sql`
2. `V2__create_products.sql`
3. `V3__create_users.sql`
4. `V4__create_picking_tasks.sql`
5. `V5__create_routes.sql`

This ensures that the database structure can be reproduced from version-controlled SQL files.


## 15. Design Notes and Limitations

The PostgreSQL database schema is implemented through
five versioned Flyway migrations.

The application uses the database to manage warehouses,
warehouse graphs, product locations, user accounts,
picking tasks, and collection progress.

### Implemented Business Validation

The backend implements application-level validation
for operations that cannot be enforced by SQL
constraints alone.

Examples include:

- Role-based authorization for MANAGER and PICKER accounts.
- Verification of the assigned picker during task operations.
- Validation of picking task state transitions.
- Validation of available stock when creating picking tasks.
- Prevention of collected quantities exceeding required quantities.
- Validation of warehouse graphs used for route calculation.

Spring Security handles authentication and authorization,
while Spring service methods enforce business rules.

### Current Limitations

The following features are not implemented in the
current application:

- Automatic reservation or deduction of inventory stock
  when picking items are collected.
- Automatic persistence of newly calculated routes
  into the `picking_routes` and `route_stops` tables.
- Historical analysis of calculated picking routes.

The database schema supports storing picking routes,
but the current route calculation API returns results
without saving them.

Collected quantities are recorded in `picking_task_items`.
They do not automatically update inventory quantities
in `product_locations`.

These limitations are documented to distinguish the
implemented functionality from possible future extensions.

### Data Integrity

Primary keys, foreign keys, unique constraints,
CHECK constraints, and NOT NULL constraints enforce
structural data integrity.

Application-level validation complements these database
constraints for business rules involving multiple entities.

Transactional service methods help maintain consistency
during picking task operations.

## 16. Data Quality and Integrity Verification

Database constraints and transaction behavior were tested using SQL scripts executed against the Supabase PostgreSQL development database.

### Constraint Verification

| Table | Tested condition | Expected behavior | Result |
|---|---|---|---|
| `products` | Negative product weight | Reject invalid weight | Passed |
| `products` | Duplicate SKU | Reject duplicate identifier | Passed |
| `products` | NULL product name | Reject missing required value | Passed |
| `product_locations` | Nonexistent product reference | Reject invalid foreign key | Passed |
| `product_locations` | Negative stock quantity | Reject invalid quantity | Passed |

### Transaction Verification

A transaction test verified that the creation of a picking task and its items is atomic.

When an invalid task item was inserted, PostgreSQL rejected the operation and rolled back the transaction block, including the previously created task and valid task item.

**Result:** Passed.

### Access Control Verification

The following database access restrictions were verified:

- The `app_read` role cannot modify application records.
- The `app_read` role cannot read password hashes.
- The `app_write` role can read and modify application records.
- Application roles cannot create tables in the `public` schema.
- Application roles cannot modify Flyway migration history.
- The `warehouse_app` account has no superuser, database creation, or role creation privileges.

The Java backend connection was successfully verified using the restricted `warehouse_app` account.

### Verification Scripts

- [Constraint Tests](../../database/tests/01_test_constraints.sql)
- [Transaction Tests](../../database/tests/02_test_transactions.sql)
- [Permission Tests](../../database/security/03_verify_permissions.sql)

## 17. Related Documentation

- [Database Schema](DATABASE_SCHEMA.md)
- [Database Setup](DATABASE_SETUP.md)
- [Logical Database Diagram](database_schema.png)
- [Flyway Migrations](../../backend/src/main/resources/db/migration/)
- [Database Security Scripts](../../database/security/)
- [Project README](../../README.md)