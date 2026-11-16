# Warehouse Picker Route Optimization Platform

A warehouse order-picking platform focused on optimizing picking routes by considering walking distance and the weight of collected products.

## About the Project

The platform is designed to improve warehouse order-picking efficiency and reduce the physical workload of warehouse employees.

The warehouse layout is represented as a graph of storage locations, intersections, entrances, and exits. Picking routes are calculated based on the warehouse layout, product locations, required quantities, and product weight.

The optimization approach is evaluated against the traditional S-shape picking strategy.

## Features

- Warehouse layout and navigation graph management.
- Product and inventory management.
- Picker account management.
- Picking task creation and assignment.
- Weight-aware route optimization.
- Comparison of picking strategies.
- Picking progress tracking.
- Route performance analysis and real-time analytics.

## User Roles

**Warehouse Manager**

Manages warehouse layouts, products, and inventory, creates picker accounts, creates and assigns picking tasks, and analyzes route performance.

**Order Picker**

Receives assigned picking tasks, follows optimized routes, and confirms collected products.

## Technology Stack

| Component | Technology |
|---|---|
| Backend | Java 21, Spring Boot |
| Frontend | React |
| Database | PostgreSQL 17 |
| Data access | Spring Data JPA |
| Database migrations | Flyway |
| Build tool | Maven |
| Containerization | Docker Compose |
| Cloud database | Supabase |

## Getting Started

### Requirements

- Java 21
- Git
- PostgreSQL database or Supabase project
- Docker Desktop (optional, for local deployment)

### Installation

Clone the repository:

```bash
git clone https://github.com/Vladkosheliuk/warehouse-picker-route-optimization.git
cd warehouse-picker-route-optimization
```

Configure the PostgreSQL connection using environment variables.

For local database deployment, configure `.env` using `.env.example` and start PostgreSQL:

```bash
docker compose up -d
```

Apply the Flyway migrations, configure database security roles, and run the Spring Boot application.

For complete installation and configuration instructions, see [Database Setup](docs/DATABASE_SETUP.md).

## Documentation

- [Database Setup](docs/DATABASE_SETUP.md)
- [Database Schema](docs/DATABASE_SCHEMA.md)
- [Data Dictionary](docs/DATA_DICTIONARY.md)
- [Logical Database Diagram](docs/database_schema.png)
- [SQL Migrations](backend/src/main/resources/db/migration/)
- [Database Security Scripts](database/security/)
- [Test Data](database/seed/)
- [Database Tests](database/tests/)