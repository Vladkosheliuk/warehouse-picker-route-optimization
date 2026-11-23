
# Warehouse Picker Route Optimization API — Reference

- **API version:** 1.0.0
- **Specification:** OpenAPI 3.1
- **Local base URL:** `http://localhost:8080`
- **Authentication:** HTTP Basic
- **Interactive documentation:** [Swagger UI](http://localhost:8080/swagger-ui/index.html)
**Machine-readable contract:** [openapi.yaml](openapi.yaml)

## Overview

The API supports warehouse administration, user management, importing warehouse graphs, product availability lookup, task assignment and fulfillment, and calculation of two picking-route strategies.

The current controller set exposes **21 HTTP operations**. Route calculation is read-like but uses `POST`; it does not persist calculated routes.

## Authentication and authorization

All `/api/**` operations require authentication. Use an HTTP Basic `Authorization` header over HTTPS outside local development.

| Area | Permitted roles |
|---|---|
| `GET /api/auth/me` | `MANAGER`, `PICKER` |
| `/api/warehouses/**` | `MANAGER` |
| `/api/users` | `MANAGER` |
| `/api/catalog/products` | `MANAGER` |
| `GET /api/tasks`, `POST /api/tasks`, `POST /api/tasks/{id}/assign`, `POST /api/tasks/{id}/cancel` | `MANAGER` |
| `GET /api/tasks/my`, `POST /api/tasks/{id}/start`, `POST /api/tasks/{id}/items/{itemId}/collect`, `POST /api/tasks/{id}/complete` | `PICKER` |
| `GET /api/tasks/{id}` | `MANAGER` or assigned `PICKER` |
| `POST /api/routes/calculate/{taskId}` | `MANAGER` or assigned `PICKER` for an available task |

Role restrictions for tasks are checked in `PickingTaskService`. A picker cannot retrieve or calculate another picker's task. The backend additionally rejects use of inactive accounts when task operations resolve the current user.

**Note:** `/actuator/health`, Swagger UI, and OpenAPI endpoints are publicly readable in the current security configuration.

## Endpoint index

| Method | Path | Purpose | Role | Success |
|---|---|---|---|---|
| `GET` | `/api/auth/me` | Current username and role | Both | `200` |
| `GET` | `/api/warehouses` | List warehouses | Manager | `200` |
| `GET` | `/api/warehouses/{id}` | Get warehouse | Manager | `200` |
| `POST` | `/api/warehouses` | Create warehouse | Manager | `201` |
| `PUT` | `/api/warehouses/{id}` | Update warehouse | Manager | `200` |
| `DELETE` | `/api/warehouses/{id}` | Delete warehouse | Manager | `204` |
| `GET` | `/api/warehouses/{warehouseId}/map` | Get warehouse graph | Manager | `200` |
| `POST` | `/api/warehouses/{warehouseId}/map/import` | Import graph into empty warehouse | Manager | `201` |
| `GET` | `/api/users` | List user accounts | Manager | `200` |
| `POST` | `/api/users` | Create user account | Manager | `201` |
| `GET` | `/api/catalog/products?warehouseId={id}` | Products in warehouse with stock | Manager | `200` |
| `GET` | `/api/tasks` | List all tasks | Manager | `200` |
| `GET` | `/api/tasks/my` | List own assigned tasks | Picker | `200` |
| `GET` | `/api/tasks/{id}` | Get a task | Manager / assigned picker | `200` |
| `POST` | `/api/tasks` | Create and assign task | Manager | `201` |
| `POST` | `/api/tasks/{id}/assign` | Assign or reassign task | Manager | `200` |
| `POST` | `/api/tasks/{id}/start` | Start assigned task | Picker | `200` |
| `POST` | `/api/tasks/{id}/items/{itemId}/collect` | Record collected quantity | Picker | `200` |
| `POST` | `/api/tasks/{id}/complete` | Complete task | Picker | `200` |
| `POST` | `/api/tasks/{id}/cancel` | Cancel task | Manager | `200` |
| `POST` | `/api/routes/calculate/{taskId}` | Compare optimized and S-shape routes | Manager / assigned picker | `200` |

## 1. Authentication

### `GET /api/auth/me`

Returns the username and role of the authenticated account. No request body.

Example `200 OK` response:

```json
{
  "username": "picker_demo",
  "role": "PICKER"
}
```

The login name is illustrative. Invalid or missing Basic credentials yield `401`. An unsupported application role is rejected with `403`.

## 2. Warehouse management

All five operations require `MANAGER`.

### `GET /api/warehouses`

Returns an array of `WarehouseDTO` objects; an empty database returns `[]`.

### `GET /api/warehouses/{id}`

Returns the warehouse with the specified numeric database ID, or `404` when missing.

### `POST /api/warehouses`

Request body:

```json
{
  "name": "API Test Warehouse",
  "description": "Disposable warehouse for integration testing"
}
```

Example `201 Created` response (generated ID varies):

```json
{
  "id": 42,
  "name": "API Test Warehouse",
  "description": "Disposable warehouse for integration testing"
}
```

### `PUT /api/warehouses/{id}`

Replaces the editable name and description of an existing warehouse. Returns updated `WarehouseDTO` with `200`; missing warehouse yields `404`.

### `DELETE /api/warehouses/{id}`

Deletes an existing warehouse. A successful response is `204 No Content` without a JSON body. Deletion may fail due to database foreign keys when related graph or task records exist; the current exception handling does not guarantee a standardized `409` for these database-level failures.

### `WarehouseDTO` fields

| Field | Type | Constraints |
|---|---|---|
| `id` | int64, nullable in input | Assigned by database |
| `name` | string | Required, nonblank, at most 150 characters |
| `description` | string or null | Optional, at most 1000 characters |

## 3. Warehouse maps

Both operations require `MANAGER`.

### `GET /api/warehouses/{warehouseId}/map`

Returns the stored warehouse graph in `WarehouseMapDTO` format. Missing warehouse returns `404`. A warehouse without a map can return empty `nodes` and `edges` arrays.

### `POST /api/warehouses/{warehouseId}/map/import`

Imports a graph **only into a warehouse with no existing nodes or edges**. Returns `201 Created` and the submitted `WarehouseMapDTO`. Re-importing an existing map results in `409`.

Minimal example request:

```json
{
  "nodes": [
    {"code": "ENTRY", "type": "ENTRANCE", "x": 0, "y": 0},
    {"code": "S01", "type": "STORAGE", "x": 10, "y": 0},
    {"code": "EXIT", "type": "EXIT", "x": 20, "y": 0}
  ],
  "edges": [
    {"from": "ENTRY", "to": "S01", "distanceM": 10, "bidirectional": true},
    {"from": "S01", "to": "EXIT", "distanceM": 10, "bidirectional": true}
  ]
}
```

Constraints:

- `nodes`: 3–1000 entries; `edges`: 1–5000 entries.
- Each node has a unique `code` matching `[A-Za-z0-9_-]{1,50}`, a valid `type`, and decimal `x`/`y` coordinates.
- Node types: `ENTRANCE`, `EXIT`, `INTERSECTION`, `STORAGE`.
- Edge fields: `from`, `to`, positive `distanceM` (at least `0.01`), and `bidirectional`.
- Exactly one entrance, one exit, and at least one storage node are required.
- Duplicate or self-referencing edges and unknown node references are rejected.
- All graph nodes must be reachable from the entrance and must have a path to the exit, respecting edge direction.

Validation failures generally return `400`; existing maps return `409`.

## 4. User management

Both operations require `MANAGER`.

### `GET /api/users`

Returns an array of user accounts without password hashes.

Illustrative `200 OK` response:

```json
[
  {
    "id": 3,
    "username": "picker_demo",
    "fullName": "Demo Picker",
    "role": "PICKER",
    "active": true
  }
]
```

### `POST /api/users`

Creates a `MANAGER` or `PICKER` account. The password is encoded with BCrypt and never returned in the response.

Example `CreateUserRequest`:

```json
{
  "username": "picker_demo",
  "fullName": "Demo Picker",
  "password": "replace-with-strong-password",
  "role": "PICKER"
}
```

The password is a placeholder, not a usable credential. The response is `201 Created` with a `UserResponse` object.

| Field | Validation |
|---|---|
| `username` | Required; matches `[A-Za-z0-9._-]{3,100}` |
| `fullName` | Required, nonblank, maximum 150 characters |
| `password` | Required, 12–72 characters |
| `role` | `MANAGER` or `PICKER` |

A duplicate username yields `409 Conflict`. Bean Validation failures yield `400 Bad Request`.

## 5. Product catalog

### `GET /api/catalog/products?warehouseId={id}`

**Role:** `MANAGER`.

`warehouseId` is a required numeric query parameter. Returns products with positive available stock aggregated across storage locations in the selected warehouse, sorted by SKU.

Illustrative `200 OK` response:

```json
[
  {
    "id": 1,
    "sku": "DEMO-001",
    "name": "Demo Product",
    "weightKg": 0.5,
    "availableQuantity": 12
  }
]
```

Missing warehouse returns `404`. A warehouse with no available products returns `[]`.

## 6. Picking tasks

A task consists of a warehouse, creator, assigned picker, status, timestamps, and task item lines. IDs shown below are illustrative and must be replaced with actual database IDs.

### `POST /api/tasks`

**Role:** `MANAGER`. Creates and assigns a task in `ASSIGNED` state. Returns `201 Created` and `TaskResponse`.

```json
{
  "warehouseId": 1,
  "pickerId": 3,
  "items": [
    {"productId": 1, "quantity": 2},
    {"productId": 2, "quantity": 1}
  ]
}
```

Fields `warehouseId`, `pickerId`, `productId`, and `quantity` must be positive. `items` must be nonempty. Duplicate products are rejected (`400`), unknown warehouse/product/picker IDs return `404`, an inactive or non-picker account yields `400`, and insufficient warehouse stock yields `409`.

### `GET /api/tasks`

**Role:** `MANAGER`. Returns all tasks, newest ID first.

### `GET /api/tasks/my`

**Role:** `PICKER`. Returns tasks assigned to the current picker, newest ID first.

### `GET /api/tasks/{id}`

**Role:** `MANAGER` or the picker assigned to this task. Returns `TaskResponse`. Missing task returns `404`; unauthorized task ownership yields `403`.

### `POST /api/tasks/{id}/assign`

**Role:** `MANAGER`. Assigns or reassigns a `NEW` or `ASSIGNED` task and returns `200 OK`.

```json
{"pickerId": 3}
```

Other task states yield `409 Conflict`.

### `POST /api/tasks/{id}/start`

**Role:** assigned `PICKER`. Changes state from `ASSIGNED` to `IN_PROGRESS`, sets `startedAt`, and returns the updated task. Other states yield `409`.

### `POST /api/tasks/{id}/items/{itemId}/collect`

**Role:** assigned `PICKER`. Adds collected quantity to one task item when task state is `IN_PROGRESS`.

```json
{"quantity": 1}
```

Quantity must be positive. The cumulative collected quantity cannot exceed `requiredQuantity` (`409`). A nonexistent item returns `404`; an item from another task returns `400`.

**Current limitation:** This operation updates the collected quantity, but does not deduct stock from `product_locations`.

### `POST /api/tasks/{id}/complete`

**Role:** assigned `PICKER`. Only an `IN_PROGRESS` task with every required quantity collected can be completed; otherwise returns `409`. The success response contains the task in `COMPLETED` state.

### `POST /api/tasks/{id}/cancel`

**Role:** `MANAGER`. Cancels a task unless it is already `COMPLETED` or `CANCELLED`; invalid state yields `409`.

### `TaskResponse` example

```json
{
  "id": 5,
  "warehouseId": 1,
  "warehouseName": "Demo Warehouse",
  "assignedPickerId": 3,
  "assignedPickerUsername": "picker_demo",
  "status": "ASSIGNED",
  "createdAt": "2026-11-23T18:00:00Z",
  "startedAt": null,
  "completedAt": null,
  "items": [
    {
      "id": 11,
      "productId": 1,
      "sku": "DEMO-001",
      "productName": "Demo Product",
      "requiredQuantity": 2,
      "collectedQuantity": 0
    }
  ]
}
```

These IDs, product names, and timestamps are illustrative. The actual database determines them.

Task status values: `NEW`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.

## 7. Route optimization

### `POST /api/routes/calculate/{taskId}`

**Role:** `MANAGER`, or the picker assigned to the task if its status is `ASSIGNED` or `IN_PROGRESS`. No request body.

Returns a `RouteComparison`:

```json
{
  "taskId": 1,
  "warehouseId": 1,
  "optimized": {
    "stops": [
      {
        "nodeId": 9,
        "productId": 3,
        "quantity": 2,
        "distanceFromPreviousM": 20,
        "carriedWeightAfterKg": 3
      },
      {
        "nodeId": 2,
        "productId": null,
        "quantity": 0,
        "distanceFromPreviousM": 30,
        "carriedWeightAfterKg": 3
      }
    ],
    "totalDistanceM": 50,
    "loadDistanceKgM": 90,
    "fullPathNodeIds": [1, 9, 2]
  },
  "sShape": {
    "stops": [
      {
        "nodeId": 9,
        "productId": 3,
        "quantity": 2,
        "distanceFromPreviousM": 20,
        "carriedWeightAfterKg": 3
      },
      {
        "nodeId": 2,
        "productId": null,
        "quantity": 0,
        "distanceFromPreviousM": 30,
        "carriedWeightAfterKg": 3
      }
    ],
    "totalDistanceM": 50,
    "loadDistanceKgM": 90,
    "fullPathNodeIds": [1, 9, 2]
  }
}
```

**Illustrative shape only:** the numeric route values and paths above are not a recorded test result. Use the actual calculation output for performance conclusions.

| Field | Meaning |
|---|---|
| `taskId` | Requested picking task |
| `warehouseId` | Warehouse containing the task |
| `optimized`, `sShape` | Results from the two strategies |
| `stops[]` | Ordered collection stops and final exit |
| `totalDistanceM` | Total walking distance (m) |
| `loadDistanceKgM` | Accumulated distance × carried load (kg·m) |
| `fullPathNodeIds[]` | Graph node IDs, including intermediate nodes |
| `distanceFromPreviousM` | Travel distance from preceding stop (m) |
| `carriedWeightAfterKg` | Load after the current collection (kg) |

The optimized method uses a greedy score of shortest-path distance plus `0.1 ×` candidate item weight. Shortest paths are calculated on the warehouse graph. The S-shape approach is a coordinate/aisle-based baseline. The optimized method is a **heuristic**, not a guaranteed global optimum.

Possible errors: `403` when a picker does not own the task; `404` for an unknown task; `409` for cancelled/unavailable task state, missing entrance or exit, no items to pick, or insufficient stock; `400` for invalid or unreachable path conditions. Authentication failures return `401`.

**Persistence:** Neither algorithm's result is saved by this endpoint. The database includes `picking_routes` and `route_stops` tables for potential future persistence.

## 8. Error responses

Application-level errors handled by `GlobalExceptionHandler` use fields such as:

```json
{
  "timestamp": "2026-11-23T18:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Picking task not found: 999999999",
  "path": "/api/tasks/999999999"
}
```

Bean Validation failures can additionally include `fieldErrors`:

```json
{
  "timestamp": "2026-11-23T18:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/users",
  "fieldErrors": {
    "username": "must match the required pattern"
  }
}
```

The `fieldErrors` messages vary with the constraint and input. **Security filter `401` and `403` responses may have empty bodies**. Some framework-generated validation and `ResponseStatusException` responses may differ from the application-level JSON error format.

| Code | Meaning | Examples |
|---|---|---|
| `200` | Successful read, update, or calculation | GET, PUT, task transitions |
| `201` | Created | Warehouse, user, task, map import |
| `204` | Deleted | Warehouse deletion |
| `400` | Bad Request | Bean Validation, duplicate products, malformed graph |
| `401` | Unauthorized | Missing/invalid Basic credentials |
| `403` | Forbidden | Wrong role or unauthorized task ownership |
| `404` | Not Found | Unknown warehouse, task, user, or item |
| `409` | Conflict | Duplicate username, insufficient stock, invalid task state, repeated map import |
| `500` | Internal Server Error | Unhandled technical/database problem; not a normal business outcome |

OpenAPI response definitions describe supported or expected statuses. They are not a guarantee that all error responses share one serialized JSON schema.

## 9. Reproducibility and maintenance

The authoritative API contract is generated by springdoc-openapi from the running Spring Boot application. After changing controllers, DTOs, validation, or OpenAPI annotations, re-export the specification **from the repository root**:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs.yaml" -OutFile "docs/api/openapi.yaml"

npx --yes @redocly/cli@latest lint docs/api/openapi.yaml
```

See [Getting Started](GETTING_STARTED.md), [Integration Tutorial](INTEGRATION_TUTORIAL.md), and [Database Setup](../database/DATABASE_SETUP.md). All examples in this document are explanatory unless expressly identified as a measured result.
