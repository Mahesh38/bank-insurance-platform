# Architecture — 1SB integration module

**Up:** [docs index](../../README.md) → [1SB integration](../README.md) → **architecture**
**Scope:** This module and the platform services it depends on — **not** the whole platform.
For target-state platform architecture see [`docs/platform/architecture-review/`](../../platform/architecture-review/README.md).

---

| Document | What it covers | Read when |
|----------|---------------|-----------|
| **[1sb-integration-service-architecture.md](./1sb-integration-service-architecture.md)** | **The full technical SSOT** — modules, ports/adapters, APIs, data model, NFRs, compliance, test strategy (SOLID + DRY + KISS) | Before writing any code in this service |
| [replaceable-middleware.md](./replaceable-middleware.md) | *Why* the ports/adapters exist — how to use 1SB today without locking the bank in | You're tempted to let a 1SB shape leak into a bank API |
| [bank-persistence-service.md](./bank-persistence-service.md) | Platform common persistence contract — multi-consumer, Flyway, `/internal/v1` | You need durable state; this service has no DB of its own |
| [audit-consumer-service.md](./audit-consumer-service.md) | Audit-consumer stub — persists via bank-persistence HTTP only | Working on audit event sinks |

---

## The one rule

> Bank apps call **1sb-integration-service**, which translates canonical requests to 1SB.
> Durable state belongs to **bank-persistence-service** over internal HTTP.
> **Bank apps never call 1SB or the database directly.**

`bank-persistence-service` is **platform-common**, not 1SB-owned — the naming was corrected
under TD-016. Historical documents that say `1sb-persistence-service` are audit trail; see
[phase-1/TECH-LEAD-REVIEW.md](../service-ssot/phase-1/TECH-LEAD-REVIEW.md).

## Related

- Domain contexts behind the canonical model: [../canonical-model/contexts.md](../canonical-model/contexts.md)
- Build docs and phase history: [../service-ssot/README.md](../service-ssot/README.md)
- Service implementations: [`services/1sb-integration-service`](../../../services/1sb-integration-service/README.md) · [`services/bank-persistence-service`](../../../services/bank-persistence-service/README.md)
