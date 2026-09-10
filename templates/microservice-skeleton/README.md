# {{SERVICE_NAME}} Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #{{CONTEXT_ID}} |
| Gradle module | `services/{{MODULE}}` |
| Logical ownership group | `{{GITLAB_GROUP}}` (CODEOWNERS / future extract — not a GitLab project at H0) |
| HTTP port (local) | {{PORT}} |
| Error contract `service-id` | `{{SERVICE_ID}}` |
| Target datastore | {{DATASTORE}} |

## Purpose

Placeholder module for the **{{SERVICE_NAME}}** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created so engineers see the bounded-context topology inside the Gradle monorepo
(`ADR-019`). GitLab CE H0 is one backend project (`ADR-020`); this group name is ownership,
not a separate GitLab project.

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
