# Consent Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #6 |
| Gradle module | `services/consent-service` |
| GitLab group (proposed) | `ws3-domain` |
| HTTP port (local) | 8092 |
| Error contract `service-id` | `consent` |
| Target datastore | Aurora PostgreSQL (append-only) |

## Purpose

Placeholder module for the **Consent** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/consent
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:consent-service:test
./gradlew :services:consent-service:bootRun
curl -s http://localhost:8092/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module consent-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
