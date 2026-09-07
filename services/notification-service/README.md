# Notification Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #17 |
| Gradle module | `services/notification-service` |
| GitLab group (proposed) | `ws3-platform` |
| HTTP port (local) | 8102 |
| Error contract `service-id` | `notification` |
| Target datastore | DynamoDB (delivery log) |

## Purpose

Placeholder module for the **Notification** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/notification
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:notification-service:test
./gradlew :services:notification-service:bootRun
curl -s http://localhost:8102/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module notification-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
