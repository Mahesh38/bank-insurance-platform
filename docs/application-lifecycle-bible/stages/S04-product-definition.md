# S04 — Product Definition & Release Slicing

**AIGEM stage:** L1 — Business Design · **Owner:** Rajal (Product)
**Central question:** *What is in R0, and what is deliberately not?*

---

## 1. Purpose

Convert a complete requirement set into **releases that can actually be delivered and launched**.
The critical output is not the in-scope list — it is the **out-of-scope list with revisit
triggers**, because that list is what AIGEM's scope-fit triage consults for the rest of the
programme's life.

A release slice is well formed when it delivers business value on its own. "All the back-end
services" is not a release; nobody can sell a policy with it.

## 2. Entry criteria

- [ ] GATE-S03 passed: requirements with acceptance criteria, rules, and information model
- [ ] Capacity and delivery constraints known from Kalpana

## 3. Epics and stories

### S04-E01 — Release strategy · *Rajal + Kalpana*

| ID | Story | Acceptance criteria |
|---|---|---|
| S04-E01-S01 | Define the release ladder | R0 (pilot), R1, R2… each with a business outcome, not a component list |
| S04-E01-S02 | Define R0's business outcome | One sentence a sponsor would recognise: e.g. "an RM can sell one Term product to an ETB customer, end to end, with full audit" |
| S04-E01-S03 | Define the pilot population | Which branches, how many RMs, which customers, over what period |
| S04-E01-S04 | Define launch and rollback criteria | What makes R0 a success; what would cause it to be withdrawn |

### S04-E02 — Scope definition · *Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S04-E02-S01 | Define R0 in-scope | Requirement IDs, explicitly enumerated — not described by theme |
| S04-E02-S02 | **Define R0 out-of-scope with revisit triggers** | Every excluded item names the release or condition that revisits it |
| S04-E02-S03 | Define permanent exclusions | Things that will never be in scope, so they stop being re-proposed |
| S04-E02-S04 | Lock the channel, segment and LOB decisions | Assisted/DIY/hybrid; ETB/NTB; which LOB first — each recorded as a decision with rationale |
| S04-E02-S05 | Define the insurer panel for R0 | Group A named for in-platform journeys; Group B handled by controlled redirect |

### S04-E03 — Product backlog · *Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S04-E03-S01 | Decompose requirements into epics and stories | Every R0 requirement maps to ≥ 1 story; every story traces back |
| S04-E03-S02 | Prioritise using AIGEM P1–P5 | No persona-local severity used as priority |
| S04-E03-S03 | Identify dependencies between stories | Dependency register populated; the critical path visible |
| S04-E03-S04 | Size the backlog | Anything XL is split or converted to a spike |
| S04-E03-S05 | Identify technical enablers | Foundation, integration and platform work made visible as backlog items, not assumed as overhead |

### S04-E04 — Definition of Ready and Done · *Rajal + Swapnali + Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S04-E04-S01 | Agree Definition of Ready | Published, and applied — stories that fail it are not started |
| S04-E04-S02 | Agree Definition of Done | Includes tests green in CI, evidence linked, observability present, no PII in logs |
| S04-E04-S03 | Agree the review tiering rules | Which changes are T1–T4; the automatic T4 triggers |

### S04-E05 — Success measurement · *Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S04-E05-S01 | Define R0 KPIs | Conversion, drop-off by step, time-to-issue, reconciliation completeness, RM adoption |
| S04-E05-S02 | Define the measurement mechanism | Which system emits each KPI; instrumentation becomes an S11 story, not an afterthought |
| S04-E05-S03 | Define the baseline | What the legacy redirect model achieves today, for comparison |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S04-VT-01 | R0 delivers standalone value | Ask: can a policy be sold using only R0? | Yes, end to end, by a real RM |
| S04-VT-02 | Scope is unambiguous | Present 15 candidate items; ask 3 people to classify in/out | ≥ 90% agreement |
| S04-VT-03 | Out-of-scope items have triggers | Traverse the list | Zero items with no revisit condition |
| S04-VT-04 | Backlog traces to requirements | Query both directions | No orphan story; no uncovered R0 requirement |
| S04-VT-05 | Dependencies are known | Attempt to sequence the backlog | A viable critical path exists with no cycles |
| S04-VT-06 | Enablers are visible | Count foundation and platform stories in the backlog | Non-zero, sized, and scheduled |
| S04-VT-07 | KPIs are instrumentable | For each KPI, name the emitting system and the story that adds it | 100% |

**S04-VT-06 exists because of this programme's history.** Foundation work that lives outside the
backlog as assumed overhead is foundation work that never gets scheduled — which is one direct
mechanism behind the missing S08 and S09.

## 5. Exit gate — GATE-S04

| # | Criterion | Evidence level | Evidence artefact |
|---|---|---|---|
| S04-G1 | R0 scope defined: in, out with triggers, and never | E2 | Signed scope document |
| S04-G2 | PRD approved for R0 | E2 | Signed PRD |
| S04-G3 | Backlog decomposed, prioritised, sized, dependency-mapped | E1 | Backlog with dependency register |
| S04-G4 | DoR and DoD agreed and published | E2 | Signed definitions |
| S04-G5 | R0 delivers standalone business value | E2 | Walkthrough record confirming a complete sale is possible |
| S04-G6 | KPIs defined with measurement mechanisms | E1 | KPI definitions naming source systems |
| S04-G7 | Technical enablers visible in the backlog and scheduled | E1 | Backlog query showing foundation items |

**Approvers:** Rajal (AP) · Kalpana (AP, feasibility) · Mahesh (RV) · Swapnali (RV) ·
Deepali (RV) · Shailja (RV) · Shivanshi (RV)

## 6. Current position in this repository — 🟡 Partial

**Present and good:** `R0-SCOPE.md`, `PRD-R0-DISTRIBUTION-PLATFORM.md`, the working-decisions
document that locked channel (RM + Self + Hybrid Day 1), segment (ETB only), LOB (Life first),
and the "sold" definition. The scope structure — in / out with revisit-at / never — is exactly
right and AIGEM consumes it correctly.

**Open:**

| Item | Detail |
|---|---|
| GAP-013 | Product matrix dimensions undefined — R0's catalogue content is not specified |
| GAP-012 | Quote validity and compare rules missing |
| S04-E05 | KPI instrumentation not traced to stories; measurement mechanism unspecified |
| **S04-VT-06** | **Fails.** The backlog contains no CI, IaC, environment, or observability enablers. Foundation was treated as overhead, and therefore never scheduled |

**The R0 scope is also worth re-reading against reality.** R0 as written requires assisted, DIY
*and* hybrid channels on Day 1. With no UI built and no foundation, a narrower R0 — one channel,
one product, one insurer — would reach a demonstrable sale materially sooner and would make S11
achievable. That is a Product decision and I am recommending it as part of the realignment.

## 7. Premature at this stage

Architecture · service design · implementation · UI build · infrastructure.

The one thing S04 must *not* do is under-scope the enablers to make R0 look cheaper. A release
plan that omits foundation is not an optimistic plan; it is a plan for the situation this
repository is currently in.
