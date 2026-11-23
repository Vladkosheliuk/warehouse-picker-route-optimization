
# API Integration Tutorial — Warehouse Picker Route Optimization Platform

This tutorial shows how to exercise the local REST API from Windows PowerShell with `curl.exe`. It covers authentication, warehouse creation, map import, user and product lookup, picking-task creation, route comparison, authorization checks, and error handling.

- **Local API:** `http://localhost:8080`
- **Authorization:** HTTP Basic
**Reference:** [API Reference](API_REFERENCE.md)

## 1. Prerequisites

- A running Java 21 Spring Boot backend connected to PostgreSQL.
- A valid `MANAGER` account.
- An active `PICKER` account for the picker workflow.
- A seeded warehouse with product stock for task creation and route calculation.
- The terminal opened in the **repository root**.

Follow [Getting Started](GETTING_STARTED.md) and [Database Setup](../database/DATABASE_SETUP.md) for setup details.

The commands use `curl.exe -u username` without an inline password. Curl prompts for a password instead of recording it in the shell command. Replace example usernames and IDs with values from your environment. Never commit or publish test credentials.

## 2. Check backend availability

```powershell
curl.exe -i http://localhost:8080/actuator/health
```

A healthy backend returns `200 OK` and a response containing `"status":"UP"`.

## 3. Verify authentication behavior

```powershell
curl.exe -i http://localhost:8080/api/auth/me
```

Expected: `401 Unauthorized` without credentials.

Then use your manager login:

```powershell
curl.exe -u YOUR_MANAGER_USERNAME -i http://localhost:8080/api/auth/me
```

Expected: `200 OK` with `username` and `role: "MANAGER"`.

The backend uses HTTP Basic, not JWT.

## 4. Review existing warehouses

```powershell
curl.exe -u YOUR_MANAGER_USERNAME -i http://localhost:8080/api/warehouses
```

Expected: `200 OK` and a JSON array of warehouses. Record the ID of a **seeded warehouse containing products**, if present, for the route scenario later.

## 5. Create a disposable warehouse

Use the included example file:

[Warehouse Create Example](examples/warehouse-create.json)

```powershell
curl.exe -u YOUR_MANAGER_USERNAME -i -X POST http://localhost:8080/api/warehouses -H "Content-Type: application/json" --data-binary "@docs/api/examples/warehouse-create.json"
```

Expected: `201 Created` and a `WarehouseDTO` containing a new database ID.

Set `$warehouseId` to the **actual returned ID**:

```powershell
$warehouseId = 42  # Replace with the new disposable warehouse ID

curl.exe -u YOUR_MANAGER_USERNAME -i "http://localhost:8080/api/warehouses/$warehouseId"
```

Expected: `200 OK` and the warehouse details.

## 6. Import and retrieve a warehouse graph

A new warehouse has no graph. Create a minimal graph file in the repository root:

```powershell
@'
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
'@ | Set-Content -Encoding UTF8 "warehouse-map-demo.json"
```

Import the graph:

```powershell
curl.exe -u YOUR_MANAGER_USERNAME -i -X POST "http://localhost:8080/api/warehouses/$warehouseId/map/import" -H "Content-Type: application/json" --data-binary "@warehouse-map-demo.json"
```

Expected: `201 Created` with `nodes` and `edges`.

Retrieve it:

```powershell
curl.exe -u YOUR_MANAGER_USERNAME -i "http://localhost:8080/api/warehouses/$warehouseId/map"
```

Expected: `200 OK` and the same graph structure.

A second import into the same warehouse should return `409 Conflict`.

**Do not use this disposable warehouse for a picking task** unless it also contains product-location stock. The API does not currently expose a complete stock-placement workflow.

## 7. Inspect users and available products

List users:

```powershell
curl.exe -u YOUR_MANAGER_USERNAME -i http://localhost:8080/api/users
```

Select an existing active `PICKER` ID from the JSON response.

List products for an existing **seeded** warehouse, not the newly created empty one:

```powershell
$seededWarehouseId = 1  # Replace with an actual warehouse containing stock

curl.exe -u YOUR_MANAGER_USERNAME -i "http://localhost:8080/api/catalog/products?warehouseId=$seededWarehouseId"
```

Expected: `200 OK` with available product IDs, names, weights, and quantities.

If it returns `[]`, use a different seeded warehouse or load test data according to the database setup guide.

## 8. Create an assigned picking task

Use a real active picker ID and product ID from step 7.

This example assumes warehouse `1`, picker `3`, product `1`, and available stock of at least `2` units. Replace the IDs if they differ.

```powershell
@'
{
  "warehouseId": 1,
  "pickerId": 3,
  "items": [
    {"productId": 1, "quantity": 2}
  ]
}
'@ | Set-Content -Encoding UTF8 "picking-task-demo.json"
```

Ensure `warehouseId`, `pickerId`, and `productId` match the actual values.

Then create the task:

```powershell
curl.exe -u YOUR_MANAGER_USERNAME -i -X POST http://localhost:8080/api/tasks -H "Content-Type: application/json" --data-binary "@picking-task-demo.json"
```

Expected: `201 Created` with `TaskResponse` and task status `ASSIGNED`.

Record the returned task ID; do not assume a fixed value.

## 9. Calculate and compare routes

Set `$taskId` to the actual created task ID:

```powershell
$taskId = 5  # Replace with the actual task ID

curl.exe -u YOUR_MANAGER_USERNAME -i -X POST "http://localhost:8080/api/routes/calculate/$taskId"
```

Expected when the warehouse has a connected graph, entrance, exit, appropriate product storage locations and stock: `200 OK` with `optimized` and `sShape`.

Inspect these fields:

- `optimized.totalDistanceM` and `sShape.totalDistanceM`.
- `optimized.loadDistanceKgM` and `sShape.loadDistanceKgM`.
- `stops` for ordered product collection.
- `fullPathNodeIds` for the complete warehouse graph path.

Equal metrics are a legitimate result and do not prove either method is better.

This endpoint calculates routes without saving them.

## 10. Verify picker access

Use the username of the picker assigned to the task:

```powershell
curl.exe -u YOUR_PICKER_USERNAME -i http://localhost:8080/api/tasks/my

curl.exe -u YOUR_PICKER_USERNAME -i "http://localhost:8080/api/tasks/$taskId"
```

Both requests should return `200 OK` for a correctly assigned, active picker.

A picker should not be able to retrieve `/api/users`:

```powershell
curl.exe -u YOUR_PICKER_USERNAME -i http://localhost:8080/api/users
```

Expected: `403 Forbidden`.

## 11. Start, collect, and complete the task

This part **changes task state**. Use only a disposable test task, not an operational task.

Start:

```powershell
curl.exe -u YOUR_PICKER_USERNAME -i -X POST "http://localhost:8080/api/tasks/$taskId/start"
```

Expected: `200 OK`, `status: "IN_PROGRESS"`.

Find the task item `id` and its `requiredQuantity` in the returned JSON.

For an item requiring two units, create a request file:

```powershell
@'
{"quantity": 2}
'@ | Set-Content -Encoding UTF8 "collect-demo.json"
```

Then use the real item ID:

```powershell
$itemId = 11  # Replace with the actual task item ID

curl.exe -u YOUR_PICKER_USERNAME -i -X POST "http://localhost:8080/api/tasks/$taskId/items/$itemId/collect" -H "Content-Type: application/json" --data-binary "@collect-demo.json"
```

Expected: `200 OK` and an updated `collectedQuantity`.

When all task items have been collected:

```powershell
curl.exe -u YOUR_PICKER_USERNAME -i -X POST "http://localhost:8080/api/tasks/$taskId/complete"
```

Expected: `200 OK`, task status `COMPLETED`.

Completion before all required quantities have been collected returns `409 Conflict`.

## 12. Check common error responses

### Unauthenticated access

```powershell
curl.exe -i http://localhost:8080/api/tasks
```

Expected: `401 Unauthorized`.

### Insufficient manager permissions

```powershell
curl.exe -u YOUR_PICKER_USERNAME -i http://localhost:8080/api/users
```

Expected: `403 Forbidden`.

### Unknown task

```powershell
curl.exe -u YOUR_MANAGER_USERNAME -i "http://localhost:8080/api/tasks/999999999"
```

Expected: `404 Not Found`.

### Repeated map import

```powershell
curl.exe -u YOUR_MANAGER_USERNAME -i -X POST "http://localhost:8080/api/warehouses/$warehouseId/map/import" -H "Content-Type: application/json" --data-binary "@warehouse-map-demo.json"
```

Expected: `409 Conflict`.

Security-filter errors may return an empty body.

Business and Bean Validation errors generally contain `timestamp`, `status`, `error`, `message`, and `path`. Field validation can also include `fieldErrors`.

## 13. Cleanup

Do **not** delete seeded warehouses or test accounts relied on by frontend E2E tests.

The disposable warehouse created in step 5 now contains map nodes and edges, so a direct warehouse `DELETE` may fail due to foreign key constraints.

Leave it as test data or clean up its dependent records with an authorized database maintenance process in an isolated environment.

Locally created JSON example files can be deleted after testing:

```powershell
Remove-Item -ErrorAction SilentlyContinue warehouse-map-demo.json, picking-task-demo.json, collect-demo.json
```

## 14. Export and validate OpenAPI

With the backend running, execute **from the repository root**:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs.yaml" -OutFile "docs/api/openapi.yaml"

npx --yes @redocly/cli@latest lint docs/api/openapi.yaml
```

The OpenAPI 3.1 document has been validated locally with zero errors and zero warnings as of the current documentation work.

This is a schema/documentation check, not a substitute for exercising endpoint behavior or security tests.

For complete endpoint and data-model definitions, see [API Reference](API_REFERENCE.md).
