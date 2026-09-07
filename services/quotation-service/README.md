# Quotation Service

**Status:** SKELETON — scaffold only; no business logic yet.

| Field | Value |
|---|---|
| Bounded context | #10 |
| Gradle module | `services/quotation-service` |
| GitLab group (proposed) | `ws3-domain` |
| HTTP port (local) | 8096 |
| Error contract `service-id` | `quotation` |
| Target datastore | DynamoDB (job/poll) + Redis (idempotency) |

## Purpose

Placeholder module for the **Quotation** bounded context from the target microservices
architecture ([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md)).
Created ahead of the GitHub → GitLab migration so repository groups, CI policies and team
ownership can be assigned per service.

## Package layout

```text
com.bank.com/bank/platform/quotation
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:quotation-service:test
./gradlew :services:quotation-service:bootRun
curl -s http://localhost:8096/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module quotation-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
