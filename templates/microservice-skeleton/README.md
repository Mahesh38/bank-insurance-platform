# {{SERVICE_NAME}} Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #{{CONTEXT_ID}} |
| Gradle module | `services/{{MODULE}}` |
| GitLab group (proposed) | `{{GITLAB_GROUP}}` |
| HTTP port (local) | {{PORT}} |
| Error contract `service-id` | `{{SERVICE_ID}}` |
| Target datastore | {{DATASTORE}} |

## Purpose

Placeholder module for the **{{SERVICE_NAME}}** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.{{PACKAGE_PATH}}
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:{{MODULE}}:test
./gradlew :services:{{MODULE}}:bootRun
curl -s http://localhost:{{PORT}}/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module {{MODULE}}
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
