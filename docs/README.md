# Documentation — Master Index

**Programme:** AU Small Finance Bank — Insurance Distribution Platform
**Repository:** `bank-insurance-platform`

This is the entry point for everything under `docs/`. If you don't know where a document
lives, start here.

---

## How this folder is organised

Documents are separated by **what kind of authority they carry**, not by who wrote them.
There are six buckets, and each answers a different question:

| Bucket | Question it answers | Scope | Binding? |
|--------|--------------------|-------|----------|
| **[`governance/`](./governance/README.md)** | *Should this work be done at all, and when?* | How every input is triaged, prioritised, and gated — process, not content | ✅ **Binding process** |
| **[`application-lifecycle-bible/`](./application-lifecycle-bible/README.md)** | *Which stage are we in, what must it produce, and when is it done?* | The 16-stage journey from business idea to mature platform: epics, stories, gates, evidence | ⏳ Proposed (CR-010) |
| **[`context/`](./context/README.md)** | *Who are we, what problem are we solving, and what context should be loaded?* | Portable framework + project manifest/overlay, personas and roadmaps | ❌ Non-binding context |
| **[`platform/`](./platform/README.md)** | *How should the whole platform be built?* | Cross-cutting, applies to **every** service | ⚠️ Recommendation / approved spec (see each doc) |
| **[`au-bank-insurance-platform/`](./au-bank-insurance-platform/README.md)** | *What are we building and why?* | Business & product SSOT for the programme | ✅ Business SSOT |
| **[`1sb-insurance-integration/`](./1sb-insurance-integration/README.md)** | *How is the 1SB adapter built?* | One module — the 1SB integration service | ✅ Engineering SSOT (module) |

`governance/` sits across the other four: it governs **how work enters and leaves** them,
while they hold **what is true**. The remaining four split like this:

```text
PORTABLE / CROSS-CUTTING                   PROJECT- & MODULE-SPECIFIC
────────────────────────                   ──────────────────────────
context/framework/ — reusable model       context manifest/overlay — this project
platform/          — all-service specs     au-bank-insurance-platform/ — programme
                                             1sb-insurance-integration/ — module
```

> ⚠️ **Before you act on anything you find here:**
> **agents** read [`context/BOOT.md`](./context/BOOT.md) — the tier-0 capsule that answers the
> ten-fact knowledge contract from generated state — then resolve the task to its capsule with
> `python3 scripts/context/context-load.py resolve "<the request>"` and read only what it lists.
> **Humans** read your [role card in `RUNBOOK.md §6`](./governance/RUNBOOK.md#6-role-cards).
> A suggestion is never implemented in the turn it is raised — it is triaged and recorded.

---

## Full map

```text
docs/
├── README.md                          ← you are here
│
├── hdl.svg                            NORTH STAR — target-state platform HLD, release-coded R0→RN
├── architecture/                      RENDERED VIEWS — read README.md first
│   ├── README.md                          Why there are two diagrams and which is binding
│   └── r0-reference-architecture.svg      R0 executable architecture (the admitted scope)
│
├── governance/                        PROCESS — how work is admitted, gated & recorded
│   ├── RUNBOOK.md                         Operating manual — role cards, cadences
│   ├── 00–19 …                            Decision pipeline, priority, review gates
│   ├── registers/                         Suggestions, risks, decisions, parked backlog
│   ├── state/                             CURRENT-STATE.yaml + GATE-EVIDENCE.yaml
│   ├── autopilot/                         Safe selection/transition boundary
│   ├── schemas/  templates/               Machine-checked record formats
│   └── ORG-STANDARDS.md                   Company-level standards (L2)
│
├── application-lifecycle-bible/       LIFECYCLE — idea → mature platform, stage by stage
│   ├── 00–09 …                            How to use, position assessment, stage model,
│   │                                      realignment plan, gates, docs/QA/security/SRE canons
│   ├── stages/                            S00–S15: epics, stories, AC, validation tests, gates
│   ├── backlog/                           BACKLOG.yaml + Jira import CSV (generated)
│   └── templates/                         Epic, story, validation test, gate sign-off, ORR
│
├── context/                           PORTABLE MODEL + PROJECT OVERLAY + AGENT ROUTING
│   ├── BOOT.md                            TIER 0 — the only file an agent reads by default
│   ├── AGENT-CONTEXT-INDEX.yaml           TIER 1 — task -> exact, budgeted read list
│   ├── personas/                          TIER 1 — persona decision cards (3-6 KB each)
│   ├── context-manifest.yaml              Machine-readable layers, roles, load profiles
│   ├── framework/                         Reusable model, loading protocol, templates
│   ├── schemas/                           Manifest validation contract
│   ├── business-problem-statement.md      This project's problem statement
│   ├── roles/                             This project's persona packages
│   └── roadmaps/                          This project's forward-looking plans
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
| **About to act on a requirement, bug, or suggestion** | [`governance/RUNBOOK.md`](./governance/RUNBOOK.md) — find your role card; triage before you build |
| **An AI agent starting a session** | [`context/BOOT.md`](./context/BOOT.md), then `context-load.py resolve` — the binding contract behind them is [`09-AI_EXECUTION_RULES.md`](./governance/09-AI_EXECUTION_RULES.md) |
| **Acting as a persona (any board)** | [`context/personas/`](./context/personas/README.md) — a card, not a package |
| **Selecting safe non-blocked work** | [`governance/autopilot/README.md`](./governance/autopilot/README.md) + [`governance/state/GATE-EVIDENCE.yaml`](./governance/state/GATE-EVIDENCE.yaml) |
| **Reusing context for another project/domain** | [`context/framework/README.md`](./context/framework/README.md) — scaffold, replace the project overlay, validate |
| **New to the programme** | [`context/business-problem-statement.md`](./context/business-problem-statement.md) → [`au-bank-insurance-platform/README.md`](./au-bank-insurance-platform/README.md) |
| **Asking "what stage are we in, and what does it require?"** | [`application-lifecycle-bible/README.md`](./application-lifecycle-bible/README.md) — position banner, then the current stage file |
| **Product Owner / BA** | [`au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md`](./au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) — the business MVP SSOT |
| **Solution Architect** | [`platform/architecture-review/README.md`](./platform/architecture-review/README.md) — target-state platform architecture |
| **Wanting the picture, not the prose** | [`architecture/README.md`](./architecture/README.md) — the North Star HLD ([`hdl.svg`](./hdl.svg)) and the R0 architecture, and why they are two diagrams |
| **Building the 1SB adapter** | [`1sb-insurance-integration/service-ssot/README.md`](./1sb-insurance-integration/service-ssot/README.md) |
| **Building auth / identity services** | [`platform/authentication-authorization/README.md`](./platform/authentication-authorization/README.md) |
| **QA / test engineer** | [`1sb-insurance-integration/service-ssot/QA-LEAD-TESTING-STRATEGY.md`](./1sb-insurance-integration/service-ssot/QA-LEAD-TESTING-STRATEGY.md) + [`TESTING-RULES.md`](./1sb-insurance-integration/service-ssot/TESTING-RULES.md) |
| **Looking for a decision** | See the decision registers below |

---

## Where decisions live

Each register owns a distinct ID range so IDs never collide across folders.

| ID range | Register | Covers |
|----------|----------|--------|
| `GOV-xxx` | [`governance/registers/DECISION-REGISTER.md`](./governance/registers/DECISION-REGISTER.md) | Governance decisions (this register also indexes the two logs below) |
| `SUG-xxxx` | [`governance/registers/SUGGESTION-REGISTER.md`](./governance/registers/SUGGESTION-REGISTER.md) | Triaged suggestions — admitted, parked, or rejected |
| `RISK-xxx` · `ASM-xxx` · `DEP-xxx` | [`governance/registers/`](./governance/README.md) | Risks · assumptions · dependencies |
| `D-xxx`, `DOC-xxx` | [`au-bank-insurance-platform/DECISION-LOG.md`](./au-bank-insurance-platform/DECISION-LOG.md) | Business & product decisions |
| `ARCH-xxx` | [`platform/architecture-review/08-architecture-decision-log.md`](./platform/architecture-review/08-architecture-decision-log.md) | Target platform architecture decisions |
| `TD-xxx` | [`1sb-insurance-integration/service-ssot/TECH-DEBT.md`](./1sb-insurance-integration/service-ssot/TECH-DEBT.md) | Engineering tech debt |
| `FUNC-xxx` | [`1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md`](./1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md) | Module functional stories |
| `QA-xxx` | [`1sb-insurance-integration/service-ssot/QA-REVIEW-LOG.md`](./1sb-insurance-integration/service-ssot/QA-REVIEW-LOG.md) | QA baseline backlog + approvals |
| `CONFIRM-xx` | [`1sb-insurance-integration/service-ssot/phase-0/README.md`](./1sb-insurance-integration/service-ssot/phase-0/README.md) | Phase 0 confirmation checklists |

---

## Which document wins?

**Process questions** — may this work start, what priority, which gate — are settled by
`governance/` and its registers; see
[`00-GOVERNANCE.md`](./governance/00-GOVERNANCE.md) for its own precedence rules. Everything
below is about **content**: when two documents disagree on a fact, resolve in this order:

```text
1. au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md   business MVP SSOT
2. au-bank-insurance-platform/DECISION-LOG.md                                   canonical D-xxx / DOC-xxx
3. platform/authentication-authorization/README.md                              approved workforce authN/authZ spec
4. 1sb-insurance-integration/service-ssot/                                      module engineering SSOT (1SB adapter only)
5. platform/architecture-review/                                                architecture *recommendation* — not yet approved
6. au-bank-insurance-platform/knowledge-base/                                   baseline corpus; superseded where conflicted
7. context/                                                                     background & personas — never binding
```

Three rules that are easy to get wrong:

- **`application-lifecycle-bible/` is proposed, not binding.** It carries a Product-authored
  position assessment and a 16-stage delivery model. Until **CR-010** is ratified by Boards 1–7
  it is a planning instrument: plan against it, but cite `governance/` — not the bible — as
  authority in a triage record. Where the two ever conflict, **AIGEM wins**.
- **`platform/architecture-review/` is a recommendation, not an approval.** It proposes a
  ~16-service AWS/EKS target state. It does not overwrite the business SSOT, and its
  technology choices are tracked separately as `ARCH-xxx`.
- **`1sb-insurance-integration/` is one module, not the platform.** It is a thin adapter for
  a single aggregator. Where it and `platform/architecture-review/` appear to conflict, the
  review places the adapter inside the bigger picture — it does not discard it.
- **Diagrams are rendered views, never sources of truth.** [`hdl.svg`](./hdl.svg) and
  [`architecture/`](./architecture/README.md) render the documents above; where a diagram and a
  document disagree, the document wins. `hdl.svg` in particular is *target state*: only its R0
  band is admitted scope, and nothing on it authorises work.

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
