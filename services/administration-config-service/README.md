# Administration & Config Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #19 |
| Gradle module | `services/administration-config-service` |
| GitLab group (proposed) | `ws3-platform` |
| HTTP port (local) | 8104 |
| Error contract `service-id` | `admin-config` |
| Target datastore | Aurora PostgreSQL |

## Purpose

Placeholder module for the **Administration & Config** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/adminconfig
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:administration-config-service:test
./gradlew :services:administration-config-service:bootRun
curl -s http://localhost:8104/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module administration-config-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
