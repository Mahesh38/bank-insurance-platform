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
| 3 | [../architecture/1sb-integration-service-architecture.md](../architecture/1sb-integration-service-architecture.md) | Full technical design (modules, APIs, NFR, compliance, data model, tests) |
| 4 | [FUNCTIONAL-NFR-COMPLIANCE-MAP.md](./FUNCTIONAL-NFR-COMPLIANCE-MAP.md) | Quick map: functional vs NFR vs compliance vs shared JARs |
| 5 | [../architecture/replaceable-middleware.md](../architecture/replaceable-middleware.md) | Why Case 2 / ports exist (context) |
| 6 | [../api-catalog/README.md](../api-catalog/README.md) | 1SB endpoint reference |
| 7 | [../field-guides/README.md](../field-guides/README.md) | Mandatory fields / when / why |

---

## One-line product definition

> A single bank-internal microservice that exposes LOB-routed insurance integration APIs (quote, proposal, payment, status, masters) and translates them to 1SB — with audit, security, and replaceable adapters — so bank apps never call 1SB directly.

## Accepted architecture pattern (do not reopen)

```text
Bank → 1SB Integration Service → QuoteService.create() → TermQuoteHandler → 1SB
```

Orchestration first, LOB handler second. Same pattern for Proposal / Payment / Status.
