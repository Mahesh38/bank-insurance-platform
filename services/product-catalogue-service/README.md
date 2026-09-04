# Product Catalogue Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #8 |
| Gradle module | `services/product-catalogue-service` |
| GitLab group (proposed) | `ws3-domain` |
| HTTP port (local) | 8094 |
| Error contract `service-id` | `catalogue` |
| Target datastore | Aurora PostgreSQL + Redis (read cache) |

## Purpose

Placeholder module for the **Product Catalogue** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/catalogue
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:product-catalogue-service:test
./gradlew :services:product-catalogue-service:bootRun
curl -s http://localhost:8094/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module product-catalogue-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
