# Audit & Compliance Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #16 |
| Gradle module | `services/audit-compliance-service` |
| GitLab group (proposed) | `ws3-platform` |
| HTTP port (local) | 8101 |
| Error contract `service-id` | `audit` |
| Target datastore | DynamoDB (append-only) + S3 archive |

## Purpose

Placeholder module for the **Audit & Compliance** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/audit
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:audit-compliance-service:test
./gradlew :services:audit-compliance-service:bootRun
curl -s http://localhost:8101/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module audit-compliance-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
