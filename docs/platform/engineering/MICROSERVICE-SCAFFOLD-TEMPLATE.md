# Microservice scaffold template

**Owner:** Mahesh — Principal Insurance Platform Architect (Board 1)  
**Status:** Architecture decision — skeleton scaffold for GitLab migration readiness  
**Horizon:** H0 — pre-implementation structure only  
**Work item:** S08-E06-S03 (service scaffolding template)

## Decision

Before migrating from GitHub to GitLab, every **backend bounded context** in the target microservices
architecture must exist as a **Gradle module skeleton** in this monorepo. Each skeleton:

- boots with Spring Boot 3.5 / Java 21;
- wires `bank-common-error`, `bank-common-observability` and `bank-common-audit`;
- registers a stable `bank.error.service-id` per [`07-PLATFORM-ERROR-CONTRACT.md`](../../journey-execution/07-PLATFORM-ERROR-CONTRACT.md);
- exposes `/actuator/health` and a placeholder `/internal/v1/{service-id}/info` route;
- carries the hexagonal package layout (`api` · `application` · `domain` · `config`);
- includes a minimal ArchUnit fitness test (empty-package tolerant until implementation lands).

No business logic, no real domain APIs, no production datastore wiring beyond local H2 for JPA
contexts. This is **structure for ownership and CI**, not feature delivery.

## Catalogue

The authoritative mapping of bounded context → module → port → GitLab group is
[`backend-service-catalog.yaml`](./backend-service-catalog.yaml).

## Template location

```text
templates/microservice-skeleton/
├── build.gradle.kts.jpa          # Aurora-backed contexts (Flyway + JPA + H2 local)
├── build.gradle.kts.stateless    # routing / adapter / DynamoDB contexts (no JPA)
├── Dockerfile
├── README.md
└── src/…                         # Application, package-info, tests
```

## Generate a service

```bash
# All WS-3 skeletons (already generated in this repo)
python3 scripts/scaffold/create-microservice.py --all-skeletons

# One module
python3 scripts/scaffold/create-microservice.py --module consent-service

# List catalogue
python3 scripts/scaffold/create-microservice.py --list

# Sync settings.gradle.kts after manual directory changes
python3 scripts/scaffold/create-microservice.py --sync-settings
```

The script reads `backend-service-catalog.yaml`, renders templates, and rewrites
`settings.gradle.kts`.

## Package naming

| Layer | Convention | Example |
|---|---|---|
| WS-3 domain services | `com.bank.platform.{context}` | `com.bank.platform.consent` |
| WS-1 integration | `com.bank.insurance.onesb` | existing |
| WS-2 IAM | `com.bank.identity.*` | existing |
| Platform persistence | `com.bank.persistence` | existing |

## Error contract

Each new module registers its `service-id` in
[`07-PLATFORM-ERROR-CONTRACT.md` §2.2](../../journey-execution/07-PLATFORM-ERROR-CONTRACT.md)
in the same change that creates the module. Layer defaults:

| Service kind | `bank.error.layer` |
|---|---|
| Journey Orchestration | `L6` |
| Domain microservices | `L5` |
| BFF | `L4` |

## GitLab group mapping

See [`GITLAB-REPO-STRUCTURE.md`](./GITLAB-REPO-STRUCTURE.md) for the proposed GitLab hierarchy
and which teams own which groups.

## Out of scope for this scaffold

- Customer BFF and RM Workspace BFF (frontend/edge — created manually later)
- Flutter client
- Business APIs, sagas, event schemas, or datastore-specific adapters
- Governance stage transitions or gate evidence

## References

- [`02-target-microservices-architecture.md`](../architecture-review/02-target-microservices-architecture.md)
- [`03-solution-architecture-r0.md`](../ws3-platform/03-solution-architecture-r0.md)
- [`S08-engineering-foundation.md`](../../application-lifecycle-bible/stages/S08-engineering-foundation.md) — S08-E06-S03
