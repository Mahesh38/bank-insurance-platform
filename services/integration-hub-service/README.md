# Integration Hub Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #14 |
| Gradle module | `services/integration-hub-service` |
| GitLab group (proposed) | `ws3-integration` |
| HTTP port (local) | 8100 |
| Error contract `service-id` | `integration-hub` |
| Target datastore | DynamoDB (routing config only) |

## Purpose

Placeholder module for the **Integration Hub** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/integration/hub
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:integration-hub-service:test
./gradlew :services:integration-hub-service:bootRun
curl -s http://localhost:8100/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module integration-hub-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
