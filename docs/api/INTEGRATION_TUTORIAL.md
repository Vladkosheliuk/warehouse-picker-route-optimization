# Integration Tutorial — Calling the Warehouse API from PowerShell

This tutorial demonstrates **HTTP requests** against the local Java backend. It shows how to authenticate, create a test warehouse, retrieve/update it, calculate a route for an existing picking task and inspect common errors.

## Prerequisites

Complete [Getting Started](GETTING_STARTED.md). The backend must be running at `http://localhost:8080`, with an existing MANAGER account. The supplied demo database may already have task ID `1` and warehouse ID `1`; a fresh installation may not.

The commands use Windows `curl.exe` (rather than PowerShell's `curl` alias). The `-u manager` option **prompts for a password** without requiring it to be put in the command or Git history. Replace `manager` with the real manager username.

## Step 1 — Health check

```powershell
curl.exe -i http://localhost:8080/actuator/health
```

Expected when healthy: `HTTP/1.1 200` and a health status such as `{"status":"UP"}`.

## Step 2 — Verify that protected API denies anonymous access

```powershell
curl.exe -i -X POST http://localhost:8080/api/routes/calculate/1
```

Expected: `HTTP/1.1 401` and `WWW-Authenticate: Basic ...`.

## Step 3 — List warehouses with Basic authentication

```powershell
curl.exe -u manager -i http://localhost:8080/api/warehouses
```

Enter the password only when prompted. Expected for a MANAGER account: `HTTP/1.1 200` and a JSON array (`[]` for an empty database).

## Step 4 — Create a disposable test warehouse

A sample file is included at [`examples/warehouse-create.json`](examples/warehouse-create.json):

```json
{"name": "API Review Test Warehouse", "description": "Disposable test data"}
```

Send its JSON body without dealing with Windows shell quoting:

```powershell
curl.exe -u manager -i -X POST http://localhost:8080/api/warehouses -H "Content-Type: application/json" --data-binary "@docs/api/examples/warehouse-create.json"
```

Expected: HTTP `201 Created` and JSON containing the new `id`. **Record the returned ID**. The next step uses it; do not assume it equals `42`.

## Step 5 — Read and update the test warehouse

Set `$warehouseId` to the numeric ID returned in step 4:

```powershell
$warehouseId = 42  # Replace 42 with the actual newly created warehouse ID
curl.exe -u manager -i "http://localhost:8080/api/warehouses/$warehouseId"
```

Expected HTTP `200` and the warehouse object. To update the warehouse, use [`examples/warehouse-update.json`](examples/warehouse-update.json):

```powershell
curl.exe -u manager -i -X PUT "http://localhost:8080/api/warehouses/$warehouseId" -H "Content-Type: application/json" --data-binary "@docs/api/examples/warehouse-update.json"
```

Expected: HTTP `200` with the updated `name` and `description`.

## Step 6 — Calculate a picking route from an existing seeded task

This step needs an **already populated** picking task with warehouse nodes, valid walkable edges, entrance and exit nodes, product locations, sufficient stock, and weights. The simple warehouse created in step 4 does not contain these records.

```powershell
curl.exe -u manager -i -X POST http://localhost:8080/api/routes/calculate/1
```

When using the demonstrated test data with task ID `1`, expect HTTP `200` and a `RouteComparison` JSON object containing `optimized` and `sShape`. The sample previously returned **80 m / 249 kg·m for each algorithm**. Different seed data will produce different results.

If the task does not exist, use an existing valid task ID instead. See [API Reference](API_REFERENCE.md) for the response models.

## Step 7 — Test a documented business error

```powershell
curl.exe -u manager -i -X POST http://localhost:8080/api/routes/calculate/999999999
```

Expected with a valid manager login: `HTTP/1.1 404` and a JSON error like:

```json
{
  "timestamp": "2026-11-23T00:04:58+02:00",
  "status": 404,
  "error": "Not Found",
  "message": "Picking task not found: 999999999",
  "path": "/api/routes/calculate/999999999"
}
```

Timestamp varies. A blank warehouse name on POST should produce HTTP `400` with `fieldErrors.name` describing the validation problem.

## Step 8 — Cleanup disposable data only

Delete **only** the warehouse created in step 4, after confirming it has no related nodes/tasks:

```powershell
curl.exe -u manager -i -X DELETE "http://localhost:8080/api/warehouses/$warehouseId"
```

Expected: HTTP `204 No Content`. Do not delete the existing seeded warehouse used by your route tests; foreign keys can prevent deletion and the current server may report an unhandled database error.

## Step 9 — Review API docs and export the specification

Open http://localhost:8080/swagger-ui.html, authorize with MANAGER credentials, and expand the routes described above. Then run from the repository root:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs.yaml" -OutFile "docs/api/openapi.yaml"
npx @redocly/cli@latest lint docs/api/openapi.yaml
```

`npx` requires Node.js/npm; follow the linter's output to resolve warnings or errors. Commit the updated YAML and documentation only after reviewing the repository and Git history for secrets.
