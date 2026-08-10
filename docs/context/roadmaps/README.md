# Roadmaps — exploratory transformation plans

**Parent:** [`docs/context/README.md`](../README.md)
**Status:** 🟡 **Exploratory. These do not set programme scope or dates.**

---

## ⚠️ Read this first

These roadmaps are **thinking documents produced by the persona panel**, not the programme
plan. They explore *where the platform could go*; they do not commit the programme to
anything.

The authoritative delivery plans are:

| Authoritative plan | What it governs |
|---|---|
| [`po-drive/03-PROGRAMME-TODO.md`](../../au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md) | Programme-level actions and owners |
| [`requirements/R0-SCOPE.md`](../../au-bank-insurance-platform/requirements/R0-SCOPE.md) | What is actually in scope for R0 |
| [`architecture-review/07-delivery-roadmap-and-estimate.md`](../../platform/architecture-review/07-delivery-roadmap-and-estimate.md) | Phased build plan and estimate |
| [`service-ssot/ACTION-PLAN.md`](../../1sb-insurance-integration/service-ssot/ACTION-PLAN.md) | 1SB adapter module delivery phases |

Where these roadmaps and the plans above disagree, **the plans above win**.

---

## Contents

| Document | Maturity level | What it covers |
|----------|---------------|----------------|
| [brainstorming-roadmap-action-plan.md](./brainstorming-roadmap-action-plan.md) | **Level 1/2 — current target** | The panel's alignment on a deterministic, bank-owned microservices platform. Closest to the committed direction. |
| [agentic-ai-transformation-roadmap.md](./agentic-ai-transformation-roadmap.md) | **Next level — overlay** | How the same platform evolves once agentic AI becomes a building block. An *overlay*, explicitly starting only after the deterministic core is live. |

**Reading order:** brainstorming plan → agentic roadmap. The second assumes the first.

---

## Relationship to the rest of the tree

```text
business-problem-statement.md          ← shared factual baseline
        │
        ├──► brainstorming-roadmap-action-plan.md      (deterministic platform, Level 1/2)
        │              │
        │              └──► agentic-ai-transformation-roadmap.md   (agentic overlay)
        │                              │
        └──────────────────────────────┴──► roles/*-agentic-ai-evolution.md
                                              (per-persona reasoning)
```

The agentic roadmap **extends** — never replaces — the 4-phase roadmap in
[`../business-problem-statement.md`](../business-problem-statement.md) §8 and the
microservices blueprint in the brainstorming plan §2.
