# Documentation — Master Index

**Programme:** AU Small Finance Bank — Insurance Distribution Platform
**Repository:** `bank-insurance-platform`

This is the entry point for everything under `docs/`. If you don't know where a document
lives, start here.

---

## How this folder is organised

Documents are separated by **what kind of authority they carry**, not by who wrote them.
There are four buckets, and each answers a different question:

| Bucket | Question it answers | Scope | Binding? |
|--------|--------------------|-------|----------|
| **[`context/`](./context/README.md)** | *Who are we and what problem are we solving?* | Programme-wide background, personas, forward-looking roadmaps | ❌ Non-binding context |
| **[`platform/`](./platform/README.md)** | *How should the whole platform be built?* | Cross-cutting, applies to **every** service | ⚠️ Recommendation / approved spec (see each doc) |
| **[`au-bank-insurance-platform/`](./au-bank-insurance-platform/README.md)** | *What are we building and why?* | Business & product SSOT for the programme | ✅ Business SSOT |
| **[`1sb-insurance-integration/`](./1sb-insurance-integration/README.md)** | *How is the 1SB adapter built?* | One module — the 1SB integration service | ✅ Engineering SSOT (module) |

The dividing line to keep in mind:

```text
GENERIC / CROSS-CUTTING                    PROJECT- & MODULE-SPECIFIC
─────────────────────────                  ──────────────────────────
context/    — background, personas         au-bank-insurance-platform/ — the programme
platform/   — all-service architecture     1sb-insurance-integration/  — one module
```

---

## Full map

```text
docs/
├── README.md                          ← you are here
│
├── context/                           GENERIC — background & AI/RAG context
│   ├── business-problem-statement.md      Consolidated problem statement
│   ├── roles/                             Persona context (PO, SA, Tech Head)
│   └── roadmaps/                          Forward-looking transformation plans
│
├── platform/                          CROSS-CUTTING — applies to all services
│   ├── architecture-review/               Target AWS/EKS microservices architecture
│   └── authentication-authorization/      Workforce authN/authZ SSOT
│
├── au-bank-insurance-platform/        BUSINESS SSOT — the programme
│   ├── 00-07 …                            Charter → vision → discovery → decisions
│   ├── requirements/                      BRD, PRD, R0 scope
│   ├── knowledge-base/                    Synthesized from client baseline docs
│   ├── po-drive/                          PO project view, SWOT, gaps, TODO
│   ├── artefacts/                         Source PDFs + Figma exports
│   └── references/                        Pointers to prior research
│
└── 1sb-insurance-integration/         ENGINEERING SSOT — 1SB adapter module
    ├── service-ssot/                      Authoritative build docs + phase-0…4
    ├── architecture/                      Service & persistence design
    ├── api-catalog/                       1SB endpoint catalog
    ├── field-guides/                      Field-level mandatory/when/why
    ├── canonical-model/                   Bank-owned domain model
    ├── journeys/                          Universal LOB journey
    └── reference/                         Extracted 1SB schemas + source links
```

---

## Start here, by role

| If you are… | Read this first |
|-------------|-----------------|
| **New to the programme** | [`context/business-problem-statement.md`](./context/business-problem-statement.md) → [`au-bank-insurance-platform/README.md`](./au-bank-insurance-platform/README.md) |
| **Product Owner / BA** | [`au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md`](./au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) — the business MVP SSOT |
| **Solution Architect** | [`platform/architecture-review/README.md`](./platform/architecture-review/README.md) — target-state platform architecture |
| **Building the 1SB adapter** | [`1sb-insurance-integration/service-ssot/README.md`](./1sb-insurance-integration/service-ssot/README.md) |
| **Building auth / identity services** | [`platform/authentication-authorization/README.md`](./platform/authentication-authorization/README.md) |
| **QA / test engineer** | [`1sb-insurance-integration/service-ssot/QA-LEAD-TESTING-STRATEGY.md`](./1sb-insurance-integration/service-ssot/QA-LEAD-TESTING-STRATEGY.md) + [`TESTING-RULES.md`](./1sb-insurance-integration/service-ssot/TESTING-RULES.md) |
| **Looking for a decision** | See the decision registers below |

---

## Where decisions live

Each register owns a distinct ID range so IDs never collide across folders.

| ID range | Register | Covers |
|----------|----------|--------|
| `D-xxx`, `DOC-xxx` | [`au-bank-insurance-platform/DECISION-LOG.md`](./au-bank-insurance-platform/DECISION-LOG.md) | Business & product decisions |
| `ARCH-xxx` | [`platform/architecture-review/08-architecture-decision-log.md`](./platform/architecture-review/08-architecture-decision-log.md) | Target platform architecture decisions |
| `TD-xxx` | [`1sb-insurance-integration/service-ssot/TECH-DEBT.md`](./1sb-insurance-integration/service-ssot/TECH-DEBT.md) | Engineering tech debt |
| `FUNC-xxx` | [`1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md`](./1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md) | Module functional stories |
| `QA-xxx` | [`1sb-insurance-integration/service-ssot/QA-REVIEW-LOG.md`](./1sb-insurance-integration/service-ssot/QA-REVIEW-LOG.md) | QA baseline backlog + approvals |
| `CONFIRM-xx` | [`1sb-insurance-integration/service-ssot/phase-0/README.md`](./1sb-insurance-integration/service-ssot/phase-0/README.md) | Phase 0 confirmation checklists |

---

## Which document wins?

When two documents disagree, resolve in this order:

```text
1. au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md   business MVP SSOT
2. au-bank-insurance-platform/DECISION-LOG.md                                   canonical D-xxx / DOC-xxx
3. platform/authentication-authorization/README.md                              approved workforce authN/authZ spec
4. 1sb-insurance-integration/service-ssot/                                      module engineering SSOT (1SB adapter only)
5. platform/architecture-review/                                                architecture *recommendation* — not yet approved
6. au-bank-insurance-platform/knowledge-base/                                   baseline corpus; superseded where conflicted
7. context/                                                                     background & personas — never binding
```

Two rules that are easy to get wrong:

- **`platform/architecture-review/` is a recommendation, not an approval.** It proposes a
  ~16-service AWS/EKS target state. It does not overwrite the business SSOT, and its
  technology choices are tracked separately as `ARCH-xxx`.
- **`1sb-insurance-integration/` is one module, not the platform.** It is a thin adapter for
  a single aggregator. Where it and `platform/architecture-review/` appear to conflict, the
  review places the adapter inside the bigger picture — it does not discard it.

---

## Conventions

- **File naming:** kebab-case for prose docs (`business-problem-statement.md`); SCREAMING-CASE
  reserved for SSOT/register documents (`DECISION-LOG.md`, `TECH-DEBT.md`).
- **Numeric prefixes** (`00-`, `01-`, …) mean *intended reading order* within a folder.
- **Every folder has an entry point** — a `README.md` indexing its contents, or, for the
  `phase-1`/`phase-2` folders, the `STATUS.md` / `TL-KICKOFF.md` named in the
  [phase table](./1sb-insurance-integration/service-ssot/README.md#delivery-phases). If you
  add a document, add a row to the index that points at it.
- **Links are relative.** Keep them relative so the tree survives being moved or mirrored.

---

## Related documentation outside `docs/`

| Location | Contents |
|----------|----------|
| [`../README.md`](../README.md) | Repository entry point |
| [`../AGENTS.md`](../AGENTS.md) | Build, test, and role rules for agents working in this repo |
| [`../config/onesb/README.md`](../config/onesb/README.md) | 1SB provider/secrets configuration |
| `../services/*/README.md` | Per-service implementation notes |
