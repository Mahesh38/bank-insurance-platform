# Reporting & MIS Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #18 |
| Gradle module | `services/reporting-mis-service` |
| GitLab group (proposed) | `ws3-platform` |
| HTTP port (local) | 8103 |
| Error contract `service-id` | `reporting` |
| Target datastore | Redshift / Athena over S3 data lake |

## Purpose

Placeholder module for the **Reporting & MIS** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/reporting
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:reporting-mis-service:test
./gradlew :services:reporting-mis-service:bootRun
curl -s http://localhost:8103/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module reporting-mis-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
