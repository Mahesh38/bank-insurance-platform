# S00 — Ideation & Business Case

**AIGEM stage:** L0 — Discovery · **Owner:** Executive Sponsor + Rajal (Product)
**Central question:** *Should we fund this at all?*

---

## 1. Purpose

Convert a business idea into a **funded, bounded mandate with a named owner**. Most failed
programmes are not failures of execution; they are failures of this stage, where nobody wrote
down what success would look like or who was accountable for it.

For this platform the idea is specific: AU SFB holds an IRDAI Composite Corporate Agent licence
(CA0515) and currently loses all visibility the moment a customer is redirected to an insurer's
portal. The bank cannot see drop-offs, underwriting rejections, payment status, or issuance. The
idea is to own that journey end to end.

## 2. Entry criteria

- [ ] A business problem is articulated by someone with budget authority
- [ ] The problem is plausibly in the organisation's licence and mandate

## 3. Epics and stories

### S00-E01 — Problem definition and opportunity sizing · *Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S00-E01-S01 | Articulate the as-is problem with evidence | Current-state process documented; the specific business loss named (post-redirect blindness); quantified where data exists |
| S00-E01-S02 | Size the opportunity | Addressable customer base by segment; attach-rate assumptions stated as assumptions; revenue range with a stated confidence |
| S00-E01-S03 | Define the target outcome | 3–5 outcome statements, each measurable; each has a baseline and a target date |
| S00-E01-S04 | Identify the alternatives considered | At least two alternatives (buy, partner, extend legacy) with why each was not chosen |

### S00-E02 — Regulatory and licence viability · *Shailja*

| ID | Story | Acceptance criteria |
|---|---|---|
| S00-E02-S01 | Confirm the activity is within licence | Licence number, class, and permitted activities cited; open-architecture multi-insurer distribution confirmed permissible |
| S00-E02-S02 | Identify showstopper obligations | Any obligation that would make the concept unviable is surfaced now, not at S12 |
| S00-E02-S03 | Confirm data-handling viability | Categories of regulated data identified at concept level (PII, financial, health); residency constraint stated |

### S00-E03 — Business case and funding · *Sponsor + Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S00-E03-S01 | Build the cost model | Build, run and change costs over 3 years; explicitly includes foundation and compliance cost, not only feature cost |
| S00-E03-S02 | Build the benefit model | Revenue, cost-to-serve, and control benefits, separately stated |
| S00-E03-S03 | State the investment decision | Payback period, funding envelope, tranche conditions; what would cause the programme to stop |
| S00-E03-S04 | Obtain funding approval | Recorded approval from the investment authority, dated |

### S00-E04 — Governance establishment · *Rajal + Kalpana*

| ID | Story | Acceptance criteria |
|---|---|---|
| S00-E04-S01 | **Name the executive sponsor** | A named individual, with decision rights and an escalation path, recorded in the RACI |
| S00-E04-S02 | Establish the RACI | Every decision class has a named accountable person |
| S00-E04-S03 | Define the decision cadence | Steering frequency, membership, quorum, and what it decides |
| S00-E04-S04 | Establish the KPI tree | Outcomes decompose to measurable KPIs; each KPI has a source system and an owner |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S00-VT-01 | The problem is real, not assumed | Interview 5+ stakeholders across RM, branch and operations | ≥ 4 independently describe the same post-redirect visibility loss |
| S00-VT-02 | The opportunity is credible | Independent review of the sizing assumptions by Finance | Assumptions accepted or revised; no unsupported multiplier survives |
| S00-VT-03 | The activity is permissible | Compliance opinion against the licence | Written opinion: permissible, with conditions listed |
| S00-VT-04 | Someone is accountable | Ask any team member "who is the sponsor?" | Same name from ≥ 80% |
| S00-VT-05 | Success is measurable | Walk each outcome to a KPI, a source, and an owner | No outcome ends in "improved experience" with no measure |

## 5. Exit gate — GATE-S00

| # | Criterion | Evidence level | Evidence artefact |
|---|---|---|---|
| S00-G1 | Business case approved and funded | E2 | Signed investment approval with envelope and tranche conditions |
| S00-G2 | Executive sponsor named with decision rights | E1 | RACI naming an individual |
| S00-G3 | Regulatory viability confirmed | E2 | Compliance opinion, signed |
| S00-G4 | Target outcomes defined and measurable | E1 | KPI tree with owners and baselines |
| S00-G5 | Alternatives considered and recorded | E1 | Options paper with rationale |
| S00-G6 | Programme governance operating | E1 | Steering terms of reference; first meeting minuted |

**Approvers:** Sponsor (AP) · Rajal (AP) · Shailja (AP) · Kalpana (RV) · Mahesh (RV)

## 6. Current position in this repository — 🟡 Partial

**Done:** project charter, product vision and outcomes, stakeholder working session, and a
business problem statement of unusually high quality are all present in
`docs/au-bank-insurance-platform/`. The problem articulation (S00-E01) is genuinely strong.

**Open:**

| Gap | Detail |
|---|---|
| **GAP-010** | **Executive sponsor unnamed.** The role is confirmed; the person is not. S00-G2 fails |
| GAP-021 | KPI dictionary incomplete — the "policy sold" definition is locked, the rest is not |
| S00-E03 | No cost or benefit model is present in the repository at all. S00-G1 cannot be evidenced |

**Consequence:** every decision that should escalate to a sponsor currently escalates to nobody,
which is a contributing cause of the P0 gaps that stayed open while building continued.

## 7. Premature at this stage

Architecture · technology selection · service decomposition · any code · detailed requirements ·
vendor selection · sprint planning.

An architecture diagram at S00 is not early progress; it forecloses options before the
information needed to choose exists.
