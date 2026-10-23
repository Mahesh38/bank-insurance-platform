# Administration & Config Service

**Status:** R0 slice — rule-set store (ADR-024): versions, maker-checker publish, resolve, seed import as DRAFT. Contract: [`openapi.yaml`](../../docs/platform/contracts/administration-config-service/openapi.yaml) · design: [`LLD.md`](../../docs/platform/contracts/administration-config-service/LLD.md).

| Field | Value |
|---|---|
| Bounded context | #19 |
| Gradle module | `services/administration-config-service` |
| GitLab group (proposed) | `ws3-platform` |
| HTTP port (local) | 8104 |
| Error contract `service-id` | `admin-config` |
| Target datastore | Aurora PostgreSQL |

## Purpose

R0 **rule-set store** for the **Administration & Config** bounded context
([`02-target-microservices-architecture.md`](../../docs/platform/architecture-review/02-target-microservices-architecture.md),
ADR-024). Versions, maker-checker publish, resolve and seed import are implemented; outbox relay
and PDP-gated approve remain open (see the build brief).

## Package layout

```text
com.bank.platform.adminconfig
├── api/           HTTP controllers (bank-canonical API)
├── application/   use cases / orchestration
├── domain/        aggregates, ports, invariants
└── config/        Spring wiring
```

## Run locally

```bash
./gradlew :services:administration-config-service:test
./gradlew :services:administration-config-service:bootRun
curl -s http://localhost:8104/actuator/health
```

## Scaffold from template

```bash
python3 scripts/scaffold/create-microservice.py --module administration-config-service
```

See [`MICROSERVICE-SCAFFOLD-TEMPLATE.md`](../../docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md).
