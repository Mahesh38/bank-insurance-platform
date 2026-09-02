# Payment Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #12 |
| Gradle module | `services/payment-service` |
| GitLab group (proposed) | `ws3-domain` |
| HTTP port (local) | 8098 |
| Error contract `service-id` | `payment` |
| Target datastore | Aurora PostgreSQL |

## Purpose

Placeholder module for the **Payment** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/payment
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:payment-service:test
./gradlew :services:payment-service:bootRun
curl -s http://localhost:8098/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module payment-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
