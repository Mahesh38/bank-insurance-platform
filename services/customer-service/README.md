# Customer Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #4 |
| Gradle module | `services/customer-service` |
| GitLab group (proposed) | `ws3-domain` |
| HTTP port (local) | 8090 |
| Error contract `service-id` | `customer` |
| Target datastore | Aurora PostgreSQL |

## Purpose

Placeholder module for the **Customer** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/customer
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:customer-service:test
./gradlew :services:customer-service:bootRun
curl -s http://localhost:8090/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module customer-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
