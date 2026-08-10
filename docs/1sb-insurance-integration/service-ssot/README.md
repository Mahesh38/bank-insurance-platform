# 1SB Integration Service — Single Source of Truth (SSOT)

**Audience:** Developers, tech leads, QA, compliance reviewers  
**Scope:** `1sb-integration-service` only (not the full bank insurance platform)  
**Up:** [docs index](../../README.md) → [1SB integration](../README.md) → **service SSOT**

This folder is the **authoritative starting point** for building the service. If something here conflicts with earlier research notes, **this SSOT wins**.

**Two things this SSOT does *not* own** — check these before assuming a gap:

| Concern | Owned by |
|---------|----------|
| Workforce authentication & authorization (BFF token-hiding, RBAC/ABAC, identity providers) | [`docs/platform/authentication-authorization/`](../../platform/authentication-authorization/README.md) |
| Target-state platform architecture (all ~16 services, AWS/EKS) | [`docs/platform/architecture-review/`](../../platform/architecture-review/README.md) |

Inbound auth *for this service specifically* (JWT + mTLS dual-ready) is still local: [phase-0/CONFIRM-03-inbound-auth.md](./phase-0/CONFIRM-03-inbound-auth.md).

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
| [ROLE-GUIDELINES-AND-DOD.md](./ROLE-GUIDELINES-AND-DOD.md) | Team Lead / Dev / QA / QA Lead responsibilities & Definition of Done |
| [WORK-SEQUENCE.md](./WORK-SEQUENCE.md) | Ordered steps from TL assignment → Done (with FUNC example) |
| [QA-LEAD-TESTING-STRATEGY.md](./QA-LEAD-TESTING-STRATEGY.md) | QA Lead testing pyramid, ownership, coverage |
| [TESTING-RULES.md](./TESTING-RULES.md) | Enforceable test rules / DoD |
| [TEST-BACKLOG.md](./TEST-BACKLOG.md) | QA test backlog (JaCoCo, IT, Phase 3 gates) |
| [QA-REVIEW-LOG.md](./QA-REVIEW-LOG.md) | Dual TL+QA Lead approvals for baseline QA items |
| [COVERAGE.md](./COVERAGE.md) | JaCoCo how-to-run, thresholds, exclusions, interim service floor |
| [../architecture/bank-persistence-service.md](../architecture/bank-persistence-service.md) | Platform common persistence contract (multi-consumer, Flyway, `/internal/v1`) |
| [../architecture/audit-consumer-service.md](../architecture/audit-consumer-service.md) | Audit-consumer stub — persists via bank-persistence HTTP only |
| [phase-1/TECH-LEAD-REVIEW.md](./phase-1/TECH-LEAD-REVIEW.md) | Phase 1 + senior comment disposition |
| [phase-1/TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md](./phase-1/TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md) | Persistence is platform-common (not 1SB-owned) |
| [phase-1/REFACTOR-COMMON-PERSISTENCE.md](./phase-1/REFACTOR-COMMON-PERSISTENCE.md) | Agent split for common-persistence rename |
| [phase-1/TECH-LEAD-CONFIRMATION-COMMON-PERSISTENCE.md](./phase-1/TECH-LEAD-CONFIRMATION-COMMON-PERSISTENCE.md) | TL pass — common persistence senior comment closed |
| [phase-2/TL-KICKOFF.md](./phase-2/TL-KICKOFF.md) | Phase 2 task breakdown + AC |
| [phase-2/TASK-SPLIT.md](./phase-2/TASK-SPLIT.md) | Dev A / Dev B ownership |
| [phase-2/STATUS.md](./phase-2/STATUS.md) | Phase 2 delivery status |
| [phase-2/TL-REVIEW.md](./phase-2/TL-REVIEW.md) | Phase 2 final TL approval |
| [phase-1/REFACTOR-TASK-SPLIT.md](./phase-1/REFACTOR-TASK-SPLIT.md) | Agent ownership for prior remediations |
| [phase-1/TECH-LEAD-CONFIRMATION-PASS.md](./phase-1/TECH-LEAD-CONFIRMATION-PASS.md) | Prior TL pass — senior #1–#5 closed |

## Delivery phases

Each phase folder has a README that orients you before you read its assignment/review pairs.

| Phase | Entry point | Delivered | Status |
|-------|------------|-----------|--------|
| **0** | [phase-0/README.md](./phase-0/README.md) | Access, config & confirmation pack | Exit gate partly open (prod TODOs) |
| **1** | [phase-1/STATUS.md](./phase-1/STATUS.md) | Foundations + common-persistence split | Done |
| **2** | [phase-2/TL-KICKOFF.md](./phase-2/TL-KICKOFF.md) | Ports/adapters, error model, idempotency | Done — [TL-REVIEW](./phase-2/TL-REVIEW.md) |
| **3** | [phase-3/README.md](./phase-3/README.md) | FUNC-001…006 — master data → quote → proposal | Done — all dual-approved |
| **4** | [phase-4/README.md](./phase-4/README.md) | FUNC-007 payment, FUNC-009 status | Done — ⚠️ 2 recorded variances |

⚠️ The Phase 4 folder does **not** match [ACTION-PLAN.md](./ACTION-PLAN.md)'s "Phase 4 —
hardening" scope; that hardening work is still open. See
[phase-4/README.md](./phase-4/README.md).

## Accepted architecture pattern (do not reopen)

```text
Bank → 1SB Integration Service → QuoteService.create() → TermQuoteHandler → 1SB
```

Orchestration first, LOB handler second. Same pattern for Proposal / Payment / Status.
