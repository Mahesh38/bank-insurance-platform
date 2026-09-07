# Policy & Issuance Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #13 |
| Gradle module | `services/policy-issuance-service` |
| GitLab group (proposed) | `ws3-domain` |
| HTTP port (local) | 8099 |
| Error contract `service-id` | `policy` |
| Target datastore | Aurora PostgreSQL + S3 (policy PDFs) |

## Purpose

Placeholder module for the **Policy & Issuance** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/policy
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:policy-issuance-service:test
./gradlew :services:policy-issuance-service:bootRun
curl -s http://localhost:8099/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module policy-issuance-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
