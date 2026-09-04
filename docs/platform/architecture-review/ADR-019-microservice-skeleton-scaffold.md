# ADR-019 — Backend microservice skeleton scaffold

**Status:** Accepted (architecture stakeholder decision — pre-GitLab migration)  
**Date:** 2026-09-02  
**Deciders:** Mahesh (Principal Insurance Platform Architect)  
**Consulted:** Amit (Engineering), Shivanshi (SRE), Kalpana (Delivery)

## Context

The target microservices architecture ([`02-target-microservices-architecture.md`](../platform/architecture-review/02-target-microservices-architecture.md))
defines ~16 logic-bearing backend services plus integration adapters. Only five Gradle modules existed
before this decision. A GitHub → GitLab migration requires **known repository boundaries and group-level
ownership** before policies, CI and access controls can be applied.

Stakeholders requested skeleton projects — not feature code — so new engineers see the intended
topology and GitLab groups can mirror bounded contexts.

## Decision

1. **Create a standard microservice scaffold template** at `templates/microservice-skeleton/` fulfilling
   S08-E06-S03 (observability, error handling, health, hex package layout, ArchUnit stub).
2. **Register every backend bounded context** in `docs/platform/engineering/backend-service-catalog.yaml`.
3. **Generate skeleton Gradle modules** for all WS-3 contexts not yet present (16 modules), excluding
   edge BFFs and the Flutter client (manual follow-up).
4. **Map each module to a proposed GitLab group** documented in `GITLAB-REPO-STRUCTURE.md`.
5. **Register `bank.error.service-id` values** for every new module in
   `07-PLATFORM-ERROR-CONTRACT.md` §2.2.

Two template variants:

| Variant | When | Includes |
|---|---|---|
| `jpa` | Aurora-backed contexts | Flyway, JPA, H2 local profile |
| `stateless` | Hub, adapters, DynamoDB contexts | Web + actuator only |

## Consequences

**Positive**

- Engineers, CI and GitLab admins share one catalogue of services, ports and groups.
- New services are created by script, not copy-paste.
- Error contract and package conventions are enforced from day zero.

**Negative / deferred**

- Skeleton JPA modules do not reflect DynamoDB/Redis production stores yet — README and catalogue
  document the target datastore; physical wiring lands with each context's implementation epic.
- Monorepo still builds all modules; GitLab split is a migration step, not done in this ADR.
- BFFs (`customer-bff`, `rm-workspace-bff`) remain manual — edge layer follows backend stabilisation.

## Evidence

- `./gradlew test` green with all skeleton modules included.
- Scaffold generator: `scripts/scaffold/create-microservice.py`.
- Catalogue: `docs/platform/engineering/backend-service-catalog.yaml`.
