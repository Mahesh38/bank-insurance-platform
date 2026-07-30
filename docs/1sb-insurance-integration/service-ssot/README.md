# 1SB Integration Service — Single Source of Truth (SSOT)

**Audience:** Developers, tech leads, QA, compliance reviewers  
**Scope:** `1sb-integration-service` only (not the full bank insurance platform)

This folder is the **authoritative starting point** for building the service. If something here conflicts with earlier research notes, **this SSOT wins**.

---

## Read in this order

| # | Document | Purpose |
|---|----------|---------|
| 1 | [00-po-architect-design-session.md](./00-po-architect-design-session.md) | PO ↔ Architect discussion, accepted decisions, defaults |
| 2 | [PRODUCT-BACKLOG.md](./PRODUCT-BACKLOG.md) | Epics, stories, priorities, AC, DoD — what to implement |
| 3 | [ACTION-PLAN.md](./ACTION-PLAN.md) | Phased action plan to progress the project without blockers |
| 4 | **[phase-0/README.md](./phase-0/README.md)** | **Phase 0 config + confirmation/TODO pack (loose coupling first)** |
| 5 | [../architecture/1sb-integration-service-architecture.md](../architecture/1sb-integration-service-architecture.md) | Full technical design (SOLID + DRY + KISS, modules, APIs, NFR, compliance, data model, tests) |
| 6 | [FUNCTIONAL-NFR-COMPLIANCE-MAP.md](./FUNCTIONAL-NFR-COMPLIANCE-MAP.md) | Quick map: functional vs NFR vs compliance vs shared JARs |
| 7 | [../architecture/replaceable-middleware.md](../architecture/replaceable-middleware.md) | Why Case 2 / ports exist (context) |
| 8 | [../api-catalog/README.md](../api-catalog/README.md) | 1SB endpoint reference |
| 9 | [../field-guides/README.md](../field-guides/README.md) | Mandatory fields / when / why |

---

## One-line product definition

> Bank apps call **1sb-integration-service**, which exposes LOB-routed insurance APIs and translates them to 1SB. Durable state is owned by the platform **bank-persistence-service** (common DB service over internal HTTP — also used by audit-consumer and other microservices). Bank apps never call 1SB or the DB directly.

## Living engineering docs

| Document | Purpose |
|----------|---------|
| [TECH-DEBT.md](./TECH-DEBT.md) | Tech debt log (senior review + TL findings) |
| [../architecture/bank-persistence-service.md](../architecture/bank-persistence-service.md) | Platform common persistence contract (multi-consumer, Flyway, `/internal/v1`) |
| [../architecture/audit-consumer-service.md](../architecture/audit-consumer-service.md) | Audit-consumer stub — persists via bank-persistence HTTP only |
| [phase-1/TECH-LEAD-REVIEW.md](./phase-1/TECH-LEAD-REVIEW.md) | Phase 1 + senior comment disposition |
| [phase-1/TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md](./phase-1/TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md) | Persistence is platform-common (not 1SB-owned) |
| [phase-1/REFACTOR-COMMON-PERSISTENCE.md](./phase-1/REFACTOR-COMMON-PERSISTENCE.md) | Agent split for common-persistence rename |
| [phase-1/TECH-LEAD-CONFIRMATION-COMMON-PERSISTENCE.md](./phase-1/TECH-LEAD-CONFIRMATION-COMMON-PERSISTENCE.md) | TL pass — common persistence senior comment closed |
| [phase-1/REFACTOR-TASK-SPLIT.md](./phase-1/REFACTOR-TASK-SPLIT.md) | Agent ownership for prior remediations |
| [phase-1/TECH-LEAD-CONFIRMATION-PASS.md](./phase-1/TECH-LEAD-CONFIRMATION-PASS.md) | Prior TL pass — senior #1–#5 closed |

## Accepted architecture pattern (do not reopen)

```text
Bank → 1SB Integration Service → QuoteService.create() → TermQuoteHandler → 1SB
```

Orchestration first, LOB handler second. Same pattern for Proposal / Payment / Status.
