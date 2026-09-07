# Suitability & Recommendation Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #7 |
| Gradle module | `services/suitability-service` |
| GitLab group (proposed) | `ws3-domain` |
| HTTP port (local) | 8093 |
| Error contract `service-id` | `suitability` |
| Target datastore | Aurora PostgreSQL |

## Purpose

Placeholder module for the **Suitability & Recommendation** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/suitability
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:suitability-service:test
./gradlew :services:suitability-service:bootRun
curl -s http://localhost:8093/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module suitability-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
