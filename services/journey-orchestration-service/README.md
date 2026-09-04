# Journey Orchestration Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #9 |
| Gradle module | `services/journey-orchestration-service` |
| GitLab group (proposed) | `ws3-domain` |
| HTTP port (local) | 8095 |
| Error contract `service-id` | `journey-orchestration` |
| Target datastore | DynamoDB (state machine) |

## Purpose

Placeholder module for the **Journey Orchestration** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/journey
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:journey-orchestration-service:test
./gradlew :services:journey-orchestration-service:bootRun
curl -s http://localhost:8095/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module journey-orchestration-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
