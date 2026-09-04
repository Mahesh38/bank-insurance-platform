# Direct Insurer Adapter Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #adapter-direct |
| Gradle module | `services/direct-insurer-adapter-service` |
| GitLab group (proposed) | `ws3-integration` |
| HTTP port (local) | 8105 |
| Error contract `service-id` | `direct-insurer` |
| Target datastore | none (adapter only) |

## Purpose

Placeholder module for the **Direct Insurer Adapter** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/integration/direct
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:direct-insurer-adapter-service:test
./gradlew :services:direct-insurer-adapter-service:bootRun
curl -s http://localhost:8105/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module direct-insurer-adapter-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
