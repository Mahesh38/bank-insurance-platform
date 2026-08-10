# 06 — Work Classification & Breakdown

**Layer:** L1 — generic
**Pipeline steps:** 5 — Work Classification · 8 — Work Breakdown Analysis
**Owner:** Tech Lead (type) · PO (epic scope)

---

## 1. Why type matters

The work type determines **which backlog it lands in, which review boards are mandatory, and
what "done" means**. Misclassification is not cosmetic: a defect logged as a feature skips the
root-cause question; a deliberate shortcut logged as a bug never gets an expiry date.

---

## 2. Work types

| Code | Type | Definition | Home | Mandatory boards |
|------|------|------------|------|------------------|
| `FUNC` | **Feature / Functional** | New or changed user-visible behaviour | [PRODUCT-BACKLOG.md](../1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md) | Product, Technical, QA |
| `BUG` | **Defect** | Delivered behaviour does not match its approved AC or design | Backlog (defects) | Technical, QA |
| `NFR` | **Non-functional** | Performance, availability, scalability, resilience, observability | Product backlog (NFR) | Technical, Architecture, Ops |
| `ARCH` | **Architecture** | Boundaries, contracts, component responsibility, topology | ADR + backlog | **Architecture**, Technical |
| `SEC` | **Security** | Authn/authz, secrets, crypto, attack surface, dependency vulns | Backlog + risk register | **Security**, Architecture |
| `COMP` | **Compliance / Regulatory** | Consent, retention, audit, attribution, legal, financial control | Backlog + risk register | **Risk & Compliance**, Security |
| `INFRA` | **Infrastructure** | Environments, pipelines, packaging, runtime platform | Backlog | Ops, Architecture |
| `DEBT` | **Technical debt** | A **deliberate** shortcut with a known cost | [TECH-DEBT.md](../1sb-insurance-integration/service-ssot/TECH-DEBT.md) | Technical |
| `REFACTOR` | **Refactor** | Behaviour-preserving structural improvement | Backlog | Technical, QA |
| `QA` | **Test / quality** | Test assets, coverage, harnesses, gates | [TEST-BACKLOG.md](../1sb-insurance-integration/service-ssot/TEST-BACKLOG.md) | QA |
| `OPS` | **Operations** | Runbooks, dashboards, alerts, incident response, DR | Backlog | Ops |
| `SPIKE` | **Spike** | Time-boxed investigation to raise confidence | Backlog | Technical (+ requester) |
| `DOC` | **Documentation** | Docs, diagrams, SSOT updates | In place | — (author + one reviewer) |
| `MIGRATION` | **Migration** | Data or contract movement with a cutover | Backlog + ADR | Architecture, Technical, Ops, Risk |
| `GOV` | **Governance** | Changes to `docs/governance/**` | This folder | Architecture, Product |
| `IDEA` | **Idea** | Captured, not committed to any stage | [PARKED-BACKLOG.md](./registers/PARKED-BACKLOG.md) Ideas | — |

---

## 3. Classification decision tree

```text
Does approved, delivered behaviour differ from its acceptance criteria or approved design?
├─ YES ─► Was the deviation KNOWN and ACCEPTED at merge time?
│         ├─ YES ─► DEBT   (deliberate shortcut — must have owner + expiry)
│         └─ NO  ─► BUG    (unintended — must have a root cause)
└─ NO ──► Does it change user-visible behaviour or a promised contract?
          ├─ YES ─► Is the behaviour compelled by a regulator, law, or consent regime?
          │         ├─ YES ─► COMP
          │         └─ NO  ─► FUNC
          └─ NO ──► What does it change?
                    ├─ Attack surface / secrets / authn / authz / crypto ─► SEC
                    ├─ Speed, capacity, resilience, observability ───────► NFR
                    ├─ Component boundaries, contracts, topology ───────► ARCH (+ ADR)
                    ├─ Structure only, behaviour identical ────────────► REFACTOR
                    ├─ Tests, coverage, harnesses ─────────────────────► QA
                    ├─ Pipelines, environments, packaging ─────────────► INFRA
                    ├─ Runbooks, alerts, dashboards, DR ───────────────► OPS
                    ├─ Data or contract movement with a cutover ───────► MIGRATION
                    ├─ Documents only ─────────────────────────────────► DOC
                    ├─ Governance rules ───────────────────────────────► GOV
                    └─ We don't know enough to answer ─────────────────► SPIKE
```

### Disambiguation rules

| Ambiguity | Rule |
|-----------|------|
| "It works as designed, but the design is wrong" | **Not a bug.** `ARCH` or `FUNC` — the design is the thing changing. A bug requires a *correct* specification that the code violates. |
| "It's slow" | `NFR` only if a target exists or is being set. Without a target it is a `SPIKE` to establish one. |
| "The code is ugly" | `REFACTOR` — and only admitted if it serves an in-flight item or repays a registered `DEBT` entry. Aesthetics alone is SC2. |
| "We took a shortcut last sprint" | `DEBT`, with owner, severity, expiry ([15](./15-TECH_DEBT_POLICY.md)) — never `BUG`. |
| "A dependency has a CVE" | `SEC` if reachable in shipped code; `DEBT`/`INFRA` if unreachable. State the reachability finding. |
| "We need to log more" | `NFR` (observability) — or `COMP` if the driver is auditability. The driver decides. |
| "Add a test for existing code" | `QA`. If it exposes a defect, that defect is a separate `BUG` item. |
| "Rename / restructure a module" | `REFACTOR` if internal; `ARCH` if it moves a boundary or a published contract. |
| Two types genuinely apply | Classify by the **strictest review requirement** (COMP > SEC > ARCH > NFR > FUNC) and record the secondary type in `also`. |

> **Rule WC-1 — A `BUG` must name the specification it violates** (AC ID, state model, contract).
> A defect with no violated specification is a `FUNC` change request in disguise.

---

## 4. ID conventions

Existing repository IDs are preserved. Governance adds its own namespace.

> **Rule ID-1 — agent-minted IDs must be collision-resistant.** `SUG` and `DEP` IDs use
> `<PREFIX>-<YYYYMMDD>-<3 chars from 0-9a-z>` — e.g. `SUG-20260812-a1b`. Two agents on two
> branches reading a shared counter would mint the same ID, and resolving the resulting YAML
> merge conflict does **not** repair the register rows and backlinks that already point at it.
> Human-owned, low-frequency registers (`CR`, `RISK`, `ASM`, `ADR`, `EPIC`, `SPIKE`) stay
> sequential — one owner, one counter, no race. Allocation rules live in
> [state/CURRENT-STATE.yaml](./state/CURRENT-STATE.yaml) `id_allocation`; uniqueness across the
> repository is asserted by `FreshnessCheck`.
>
> Four-digit `SUG-0043` forms in these documents are **legacy examples** and remain valid.

| Prefix | Used for | Assigned by |
|--------|----------|-------------|
| `SUG-<date>-<xxx>` | Triage record — **every** input gets one | Agent, on receipt |
| `FUNC-###`, `NFR-###`, `COMP-###`, `TECH-###`, `SHARED-###` | Existing product backlog IDs | PO / TL |
| `TD-###` | Tech debt ledger | TL |
| `QA-###` | Test backlog | QA Lead |
| `EPIC-###` | Epic | TL / PO |
| `SPIKE-###` | Time-boxed investigation | TL |
| `ADR-###` | Architecture decision record | Architect |
| `RISK-###` | Risk register | Risk owner |
| `DEP-###` | Dependency edge | Agent |
| `CR-###` | Change request | Anyone; approved by PO + Architect |
| `ASM-###` | Assumption | Agent |

A `SUG-####` that is ADMITTED gains a second ID of its work type and keeps both, so the
suggestion → work item chain is traceable ([08 §6](./08-BACKLOG_RULES.md#6-traceability)).

---

## 5. Work breakdown — Epic / Story / Task / Spike

### The primary test

```text
Can ONE developer complete the change independently,
producing ONE acceptance outcome?
           │
         YES ──► STORY
           │
          NO
           ▼
Does it contain MULTIPLE independently testable
business or technical outcomes?
           │
         YES ──► EPIC  (decompose into stories)
           │
          NO ──► the work is unclear, not large
                 ──► SPIKE first
```

The "no / no" branch matters: work that is neither one outcome nor several *identifiable*
outcomes is not big — it is **not understood**. Splitting it produces fake stories. Run a spike.

### Hierarchy

```text
Initiative
   │
   ├── Epic A
   │      ├── Story A1
   │      │      ├── Task A1.1
   │      │      └── Task A1.2
   │      │
   │      ├── Story A2
   │      └── Story A3
   │
   └── Epic B
```

| Level | Definition | Completion |
|-------|------------|------------|
| **Initiative** | A stage-sized business outcome; spans epics | Stage gate passed |
| **Epic** | Multiple independently testable outcomes toward one capability | All child stories Done **and** the epic's own completion definition met |
| **Story** | One acceptance outcome, one owner, independently testable and shippable | AC pass + [13-DoD](./13-DEFINITION_OF_DONE.md) |
| **Task** | A step inside a story with no independent business value | Story owner confirms |
| **Spike** | Time-boxed question with a written answer as its deliverable | Answer recorded; follow-on items raised |

### Epic triggers — any **two** force an epic

- Multiple stories
- Multiple components or services
- Multiple acceptance outcomes
- Multiple developers or agents
- Multiple dependencies
- Delivery spans multiple implementation increments

> A single one of these is a large story. Two or more is an epic. This threshold stops
> "everything is an epic" inflation and its opposite.

### Every epic must declare

```yaml
epic:
  id: EPIC-004
  title: "Health LOB enablement"
  outcome: "Bank apps can run the full quote→payment journey for lob=HEALTH"
  completion_definition: >
    Health sandbox path green end to end AND Term regression unaffected AND
    no orchestration change in QuoteService.
  stories: [FUNC-012, FUNC-016, FUNC-017, QA-011]
  not_included: ["Motor LOB", "Health-specific pricing rules"]
  dependencies: [DEP-014]
  owner: "Tech Lead"
```

`completion_definition` and `not_included` are mandatory. An epic without an explicit boundary
is a scope-creep container — it absorbs every adjacent suggestion because nothing says it
shouldn't.

### Story splitting patterns (in preference order)

1. **By acceptance outcome** — the default; one AC group per story
2. **By workflow step** — create → read → update → expire
3. **By interface** — API contract first, then handler, then persistence
4. **By variation** — happy path first, then error/edge classes
5. **By data slice** — one LOB, one product, one insurer first
6. **By quality attribute** — functional first, then the NFR as a tracked follow-on

Never split by **technical layer alone** (controller story / service story / repo story): no
layer is independently testable against a business outcome, and no layer is shippable.

### Sizing sanity checks

| Signal | Response |
|--------|----------|
| Story needs > 1 owner | Split or promote to epic |
| Story has > 7 acceptance criteria | Probably several outcomes — split |
| Story touches > 3 components | Check for a hidden boundary problem → Architecture board |
| Story cannot be demonstrated | It is a task, not a story |
| Story estimate > one increment | Split; long stories hide unknowns |
| "It depends what we find" | It is a spike |

---

## 6. Spikes

A spike exists to convert uncertainty into a decision. It must declare, before starting:

```yaml
spike:
  id: SPIKE-007
  question: "Can Redis-backed idempotency preserve current at-least-once semantics?"
  timebox: "2 days"
  deliverable: "ADR draft + prototype branch + measured latency delta"
  decision_it_unblocks: ADR-011
  exit: "question answered either way — a NO is a successful spike"
```

Rules:
- A spike **never** ships production code; its branch is evidence, not delivery.
- Timebox expiry is a hard stop: report what is known and re-decide.
- Required whenever confidence < C3 ([16 §5](./16-DECISION_MODEL.md#5-confidence-levels)) — see
  cap PRI-6.

---

## 7. Classification in the triage record

```yaml
classification:
  type: NFR
  also: [SEC]                # secondary types
  breakdown: STORY           # EPIC | STORY | TASK | SPIKE | ADR
  epic: EPIC-004             # if a child
  rationale: "Adds resilience to an existing contract; no user-visible behaviour change."
  destination: "service-ssot/PRODUCT-BACKLOG.md#E11"
```
