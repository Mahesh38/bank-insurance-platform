# S03 — Business Requirements & Process Design

**AIGEM stage:** L1 — Business Design · **Owner:** Rajal (Product) + Business Analysis
**Central question:** *What behaviour do we promise, precisely enough to test?*

---

## 1. Purpose

Turn journeys and obligations into **requirements that can be built and verified**. The test of
this stage is simple and unforgiving: hand a requirement to an engineer and a tester separately
and see whether they produce the same system.

A requirement without acceptance criteria is an aspiration. It cannot be estimated, cannot be
tested, and cannot be traced — which means it cannot be evidenced to a regulator either.

## 2. Entry criteria

- [ ] GATE-S02 passed: obligations, controls and rule packs approved
- [ ] GATE-S01 passed: journeys and capabilities mapped

## 3. Epics and stories

### S03-E01 — Requirement catalogue · *Rajal + BA*

| ID | Story | Acceptance criteria |
|---|---|---|
| S03-E01-S01 | Decompose capabilities into requirements | Every capability yields ≥ 1 requirement; every requirement traces to a capability |
| S03-E01-S02 | **Write acceptance criteria for every requirement** | Given/When/Then or an equivalent checkable form; observable by someone who did not write it |
| S03-E01-S03 | Classify requirements | Functional / non-functional / compliance / technical enabler |
| S03-E01-S04 | Attach obligations to requirements | Every S02 control appears in ≥ 1 requirement's AC |
| S03-E01-S05 | Define failure and exception behaviour | For each requirement: what happens on timeout, rejection, abandonment, duplicate, and partial failure |

### S03-E02 — Business process design (to-be) · *BA + Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S03-E02-S01 | Model each to-be process | Actors, steps, decisions, systems, hand-offs, and the regulatory checkpoints |
| S03-E02-S02 | Define process variants | Assisted / DIY / hybrid; ETB / NTB; Group A / Group B insurers |
| S03-E02-S03 | Define exception paths | Underwriting rejection, payment failure, insurer timeout, KYC mismatch, consent withdrawal |
| S03-E02-S04 | Define manual and operational procedures | What humans do when automation cannot — this becomes the S12 runbook's business half |
| S03-E02-S05 | Define SLAs per process step | Business-time expectations that become the S07 NFR inputs |

### S03-E03 — Business rules catalogue · *Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S03-E03-S01 | Catalogue eligibility rules | Age, sum assured, income, medical, occupation limits per product class |
| S03-E03-S02 | Catalogue quote rules | Validity period, comparison basis, ranking and display rules, re-quote triggers (**closes GAP-012**) |
| S03-E03-S03 | Catalogue proposal rules | Mandatory fields per product, document requirements, medical triggers |
| S03-E03-S04 | Catalogue payment rules | Permitted instruments, device isolation, timeout, retry, refund |
| S03-E03-S05 | Catalogue attribution rules | Distributor ID and SP licence sourcing; behaviour when an RM's certification is expired (**closes GAP-014**) |
| S03-E03-S06 | Define the product matrix | LOB × insurer × product × dimensions, for the catalogue service (**closes GAP-013**) |

### S03-E04 — Information model · *BA + Aarti*

| ID | Story | Acceptance criteria |
|---|---|---|
| S03-E04-S01 | Define business entities | Every noun in the glossary that has a lifecycle |
| S03-E04-S02 | Define attributes per entity | Name, type, optionality, validation, classification, source of truth (**closes GAP-016**) |
| S03-E04-S03 | Define entity relationships and cardinality | Including the ones that break naive models: joint life, multiple nominees, multi-policy customers |
| S03-E04-S04 | Define business states per entity | The state machine as the business sees it, before any technical design |

### S03-E05 — Traceability · *Rajal + Swapnali*

| ID | Story | Acceptance criteria |
|---|---|---|
| S03-E05-S01 | Establish the traceability matrix | Obligation → requirement → (later) story → code → test → evidence |
| S03-E05-S02 | Verify bidirectional completeness | No requirement without a source; no obligation without a requirement |
| S03-E05-S03 | Define how traceability is maintained | A tool and a cadence, not a one-off spreadsheet |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S03-VT-01 | Requirements are testable | QA reviews every AC for observability | 100% observable; zero "should be user-friendly" |
| S03-VT-02 | Requirements are unambiguous | Two people independently describe the behaviour from the AC | Descriptions match |
| S03-VT-03 | Failure paths are specified | Count requirements with only a happy path | Zero |
| S03-VT-04 | Rules are deterministic | Run 20 scenarios through the eligibility and quote rules by hand | Same answer every time, by every person |
| S03-VT-05 | Information model is complete | Walk each requirement; every field it needs exists in the model | No missing attribute |
| S03-VT-06 | Traceability is complete both ways | Query the matrix | Zero orphans in either direction |
| S03-VT-07 | Obligations are covered | For each S02 control, find the requirement enforcing it | 100% covered |

## 5. Exit gate — GATE-S03

| # | Criterion | Evidence level | Evidence artefact |
|---|---|---|---|
| S03-G1 | BRD complete with acceptance criteria on **every** requirement | E2 | Signed BRD — **closes GAP-008** |
| S03-G2 | To-be processes modelled including exception paths | E1 | Process models |
| S03-G3 | Business rules catalogue approved | E2 | Signed catalogue — closes GAP-012, GAP-013, GAP-014 |
| S03-G4 | Information model with attribute sheets | E2 | Attribute sheets — **closes GAP-016** |
| S03-G5 | Every S02 control traces to a requirement | E1 | Traceability matrix |
| S03-G6 | QA confirms every requirement is testable | E2 | QA testability review, signed |
| S03-G7 | Business stakeholders accept the requirement set | E2 | Acceptance record |

**Approvers:** Rajal (AP) · Shailja (AP, B) · Swapnali (AP, testability) · Mahesh (RV) ·
Aarti (RV) · Deepali (RV) · Kalpana (RV)

## 6. Current position in this repository — 🟡 Partial

**Present:** BRD overview and checklist, P0 capability BRD, PRD for R0, R0 scope, business
process catalogue, information model and rules, and a decision log. The structure is right.

**Open:**

| Gap | Impact |
|---|---|
| **GAP-008** | BR templates lack acceptance criteria. This is the gate-failing item: requirements cannot be estimated or tested, and S03-G1 fails |
| **GAP-016** | Information model attributes missing — blocks S06 logical modelling and Aarti's physical schema |
| GAP-012 | Quote validity and comparison rules missing |
| GAP-013 | Product matrix dimensions undefined |
| GAP-014 | AgentId / RM mapping model incomplete, including expired-certification behaviour |
| S03-E05 | No traceability matrix exists. A regulator asking "show me the control implementing suitability" has no answer path |

**Note the compounding:** GAP-008 (no AC) plus no traceability matrix means the requirements
baseline cannot support S12 certification even once the code exists. These are Product and BA
capacity items, independent of engineering, and should run alongside the foundation recovery.

## 7. Premature at this stage

Service decomposition · API design · database schema · technology choices · UI layout · sprint
plans.

S03 says *what the business promises*. S06 says *what concepts make it coherent*. S07 says *how
it is built*. Requirements that name a service or a table have skipped two stages and will
constrain the design for no business reason.
