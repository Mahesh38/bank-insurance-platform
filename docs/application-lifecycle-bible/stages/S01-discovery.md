# S01 — Discovery & Capability Definition

**AIGEM stage:** L0 — Discovery · **Owner:** Rajal (Product)
**Central question:** *What problem, for whom, and what capabilities does it need?*

---

## 1. Purpose

Map the business domain well enough that later design is a matter of choosing between real
options rather than inventing the problem. Discovery output feeds S03 (requirements), S05
(experience) and S06 (bounded contexts) directly — a weak capability map produces an arbitrary
service decomposition three stages later, and nobody traces the defect back this far.

## 2. Entry criteria

- [ ] GATE-S00 passed: funded, sponsored, regulatory viability confirmed

## 3. Epics and stories

### S01-E01 — Stakeholder and actor definition · *Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S01-E01-S01 | Catalogue every actor | Bank actors (RM, BM, CM, RM-regional, Zonal, National), insurer actors (desk rep, regional ops, admin), customer segments (ETB, NTB), operations and compliance actors |
| S01-E01-S02 | Map organisational hierarchies | Bank and insurer hierarchies with the reporting relationships that will drive data scoping |
| S01-E01-S03 | Identify decision rights per actor | Who may quote, override, verify, approve, view MIS — the raw material for the S07 authorization model |
| S01-E01-S04 | Capture pain points per actor | Evidence-based, from interviews or observation, not assumed |

### S01-E02 — Capability mapping · *Rajal + Mahesh*

| ID | Story | Acceptance criteria |
|---|---|---|
| S01-E02-S01 | Build the level-1 and level-2 capability map | Capabilities, not systems and not org units; each stated as a business ability |
| S01-E02-S02 | Assess each capability's current maturity | None / partial / adequate, with the supporting system named |
| S01-E02-S03 | Identify capability gaps | The delta between current and required maturity; this is the programme's real scope |
| S01-E02-S04 | Prioritise capabilities by business value | Which capabilities carry the outcome from S00 |

### S01-E03 — Journey and value-stream discovery · *Rajal + BA*

| ID | Story | Acceptance criteria |
|---|---|---|
| S01-E03-S01 | Map the as-is journeys | RM-assisted and self-service, including the redirect point where visibility is lost |
| S01-E03-S02 | Map the to-be journeys at concept level | Lead → need analysis → suitability → consent → quote → proposal → payment → issuance |
| S01-E03-S03 | Identify journey variants | ETB vs NTB; assisted vs DIY vs hybrid; Group A (aggregator) vs Group B (redirect) insurers |
| S01-E03-S04 | Identify the moments that matter | Where the sale is won or lost; where regulatory obligations bind |

### S01-E04 — Domain vocabulary · *Rajal + Mahesh*

| ID | Story | Acceptance criteria |
|---|---|---|
| S01-E04-S01 | Build the glossary | Every domain term defined once; conflicting usages resolved, not documented in parallel |
| S01-E04-S02 | Define business-state vocabulary | What "quoted", "proposed", "issued", "sold" mean, precisely |
| S01-E04-S03 | Agree the ubiquitous language | The glossary's terms are the terms used in code, APIs and Jira |

### S01-E05 — Gap and question management · *Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S01-E05-S01 | Establish the gap register | Every open question has an ID, owner, severity, and exit criterion |
| S01-E05-S02 | Establish the discovery backlog | Open questions tracked to closure, not to a document |
| S01-E05-S03 | Classify gaps by blocking severity | P0 = blocks scope/build; P1 = blocks pilot; P2/P3 = later. **P0 must actually block** |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S01-VT-01 | Capability map is complete | Walk each to-be journey step; every step maps to a capability | No orphan step; no capability with no journey |
| S01-VT-02 | Actors are complete | Walk each capability; every one has an actor who performs and one who is accountable | No unowned capability |
| S01-VT-03 | Glossary is unambiguous | Give 5 team members 10 terms to define independently | ≥ 90% agreement; disputes resolved into the glossary |
| S01-VT-04 | Journeys reflect reality | Review the as-is journey with 3 practising RMs | RMs recognise their own process; corrections captured |
| S01-VT-05 | Gaps are honestly severed | For every P0 gap, name what it blocks and confirm that thing is actually blocked | No P0 gap whose "blocked" work is proceeding |

**S01-VT-05 is the test this programme failed.** GAP-006 and GAP-007 are P0 "block scope / build
freeze" and remained open while the quote path was built and hardened. The test exists so that
this is detected at discovery review, not at a regulatory audit.

## 5. Exit gate — GATE-S01

| # | Criterion | Evidence level | Evidence artefact |
|---|---|---|---|
| S01-G1 | Capability map complete and prioritised | E1 | Capability map with maturity and priority |
| S01-G2 | Stakeholder catalogue with decision rights | E1 | Stakeholder catalogue |
| S01-G3 | As-is and to-be journeys mapped with variants | E1 | Journey canvas |
| S01-G4 | Glossary agreed and adopted | E2 | Glossary, signed off by Product + Architecture |
| S01-G5 | Gap register established with severities that bind | E1 | Gap register + a stated blocking rule |
| S01-G6 | Discovery findings reviewed with business stakeholders | E2 | Review record with attendees and corrections |

**Approvers:** Rajal (AP) · Mahesh (RV) · Shailja (RV) · Swapnali (RV) · Kalpana (RV)

## 6. Current position in this repository — 🟢 Strong

This is the best-executed stage in the programme.

**Present and good:** capability map, value stream and journeys, business process catalogue,
stakeholder catalogue, information model and rules, integration strategy, glossary, gaps and PO
assessment, plus six source volumes and five phase artefacts under
`docs/au-bank-insurance-platform/`. The knowledge base has a source index, which is rarer than it
should be.

**Minor open items:**

| Item | Detail |
|---|---|
| GAP-023 | Self-service and hybrid journey detail thin — these became Day-1 scope by working decision, so the detail is now required |
| S01-VT-05 | The blocking rule exists on paper; it did not bind in practice |

**Do not redo this stage.** Its output is sound. The realignment consumes it rather than
repeating it.

## 7. Premature at this stage

Service decomposition · database design · API contracts · technology selection · sprint planning ·
UI design · anything with a class file.

Naming nineteen microservices during discovery — which did happen, in the architecture review —
is defensible as a target-state sketch and dangerous if read as a build order. It is the latter
reading that produces "sixteen services missing" as a perceived scope rather than "one journey
missing" as the real one.
