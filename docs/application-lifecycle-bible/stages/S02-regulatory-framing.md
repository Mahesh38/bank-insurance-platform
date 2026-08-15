# S02 — Regulatory, Risk & Compliance Framing

**AIGEM stage:** L1 — Business Design · **Owner:** Shailja S (Compliance & Risk Head, Board 6)
**Central question:** *What are we legally obliged to do, and what may we never do?*

---

## 1. Purpose

In a regulated financial business this stage is **load-bearing**, and skipping or deferring it is
the most expensive mistake available. Obligations discovered at S12 are rework; obligations
discovered in production are enforcement action.

The output is not a summary of regulations. It is an **executable rule set** — precise enough
that an engineer can implement it and a tester can prove it. "Suitability must precede
recommendation" is a regulation. *"Quote endpoints return 403 unless a suitability evaluation ID
issued within 30 days for this customer and product class is presented"* is a rule pack.

> This is the stage where this repository's most serious defect lives: the suitability and consent
> rule packs (GAP-006, GAP-007) were never produced, yet the quote path was built and hardened.

## 2. Entry criteria

- [ ] GATE-S01 passed: actors, capabilities and journeys mapped
- [ ] Regulatory viability opinion from S00 available

## 3. Epics and stories

### S02-E01 — Regulatory registry · *Shailja*

| ID | Story | Acceptance criteria |
|---|---|---|
| S02-E01-S01 | Catalogue applicable regulation | IRDAI corporate agency, RBI cyber-security and payment, DPDP/privacy, banking and AML as applicable; each with citation |
| S02-E01-S02 | Extract obligations | Each obligation gets an ID, source citation, plain-language statement, and an owner |
| S02-E01-S03 | Map obligations to journey steps | Every obligation attaches to where in the journey it binds |
| S02-E01-S04 | Establish the regulatory change watch | Named owner, cadence, and a route from a change into the backlog |

### S02-E02 — Control catalogue · *Shailja + Deepali*

| ID | Story | Acceptance criteria |
|---|---|---|
| S02-E02-S01 | Define a control per obligation | Every obligation has ≥ 1 control; every control names its obligation |
| S02-E02-S02 | Classify controls as preventive/detective/corrective | Preventive controls become hard gates in code |
| S02-E02-S03 | Define the evidence for each control | What artefact proves it operates — automated test preferred |
| S02-E02-S04 | Identify non-waivable controls | The list that no authority may waive; published and agreed |

### S02-E03 — **Consent rule pack** · *Shailja + Rajal* · **closes GAP-006**

| ID | Story | Acceptance criteria |
|---|---|---|
| S02-E03-S01 | Define consent events | Which actions require consent, from which actor, at which journey point |
| S02-E03-S02 | Define consent content and versioning | Statement text, version ID, language variants; how a version change affects in-flight journeys |
| S02-E03-S03 | Define the capture mechanism per channel | Assisted: SMS/OTP to the customer's device. DIY: in-app checkbox + OTP. Evidence captured identically in both |
| S02-E03-S04 | Define the consent evidence record | Statement text, version, CIF, OTP transaction ID, timestamp, IP — append-only, immutable |
| S02-E03-S05 | Define withdrawal and expiry | Can consent be withdrawn, what happens to in-flight and completed journeys, how long consent remains valid |
| S02-E03-S06 | Define retention and retrieval | 7-year retention; retrieval SLA for a regulator request |

### S02-E04 — **Suitability rule pack** · *Shailja + Rajal* · **closes GAP-007**

| ID | Story | Acceptance criteria |
|---|---|---|
| S02-E04-S01 | Define the need-analysis questionnaire | Questions, answer types, mandatory fields, per customer segment |
| S02-E04-S02 | Define the suitability algorithm | How answers map to a suitability outcome per product class; deterministic and reproducible |
| S02-E04-S03 | Define the hard-gate rule | **Quote is refused without a valid suitability evaluation ID.** Validity window and scope stated |
| S02-E04-S04 | Define override rules | May an RM override an unsuitable outcome? By whom, with what record, and what disclosure |
| S02-E04-S05 | Define the suitability evidence record | Answers, outcome, evaluator, timestamp, version of the algorithm used |
| S02-E04-S06 | Define re-evaluation triggers | What invalidates a prior suitability result |

### S02-E05 — Data classification and protection framing · *Shailja + Deepali*

| ID | Story | Acceptance criteria |
|---|---|---|
| S02-E05-S01 | Classify every data element | Public / internal / confidential / restricted; PII, financial and health flagged |
| S02-E05-S02 | Define handling rules per class | Encryption, masking, logging prohibition, access restriction, cross-border prohibition |
| S02-E05-S03 | Define the retention schedule | Per data class, with the legal basis and the disposal method |
| S02-E05-S04 | Define data residency requirements | India-only for regulated data, including backups, logs and archives |

### S02-E06 — Risk framework · *Shailja*

| ID | Story | Acceptance criteria |
|---|---|---|
| S02-E06-S01 | Define the risk taxonomy | Regulatory, financial, operational, security, reputational |
| S02-E06-S02 | Establish the risk register | Each risk: description, likelihood, impact, owner, treatment, review date |
| S02-E06-S03 | Define risk acceptance authority | Who may accept what level; material risk reserved to a named human |
| S02-E06-S04 | Define the exception process | Waiver requirements, expiry discipline, compensating controls |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S02-VT-01 | Every obligation has a control | Traverse the registry | Zero obligations with no control |
| S02-VT-02 | Every control has evidence defined | Traverse the catalogue | Zero controls with no evidence type |
| S02-VT-03 | Consent rules are implementable | Give the pack to an engineer and a tester independently | Both produce the same behaviour description; no clarification needed |
| S02-VT-04 | Suitability rules are deterministic | Run 20 sample customer profiles through the algorithm by hand, twice, by two people | Identical outcomes both times |
| S02-VT-05 | The hard-gate is unambiguous | Ask: what exactly makes a suitability ID "valid"? | Answer is a rule, not a judgement |
| S02-VT-06 | Data classification is complete | Sample 30 fields from the information model | 100% classified |
| S02-VT-07 | Non-waivable list is agreed | Security and Compliance both sign it | Signed; no reservations |

## 5. Exit gate — GATE-S02

| # | Criterion | Evidence level | Evidence artefact |
|---|---|---|---|
| S02-G1 | Regulatory registry complete with obligation IDs | E1 | Registry document |
| S02-G2 | Control catalogue maps every obligation to a control and its evidence | E2 | Catalogue, signed by Compliance + Security |
| S02-G3 | **Consent rule pack v1 approved** | E2 | Signed rule pack — **closes GAP-006** |
| S02-G4 | **Suitability rule pack v1 approved** | E2 | Signed rule pack — **closes GAP-007** |
| S02-G5 | Data classification and retention schedule approved | E2 | Classification matrix + retention schedule |
| S02-G6 | Data residency requirement stated and binding | E1 | Written requirement |
| S02-G7 | Risk register established with acceptance authorities | E1 | Risk register + authority matrix |
| S02-G8 | Non-waivable control list agreed | E2 | Signed list |

**Approvers:** Shailja (AP, B, **human**) · Deepali (AP) · Rajal (RV) · Mahesh (RV) · Aarti (RV) ·
Swapnali (RV) · Shivanshi (RV)

## 6. Current position in this repository — 🟡 Partial, with two P0 holes

**Present and strong:** Shailja's package (`docs/context/roles/shailja-s-compliance-risk-head/`)
carries a regulatory registry, control catalogue, risk taxonomy, decision policy, evidence policy
and exception model. The business problem statement documents the IRDAI and RBI obligations
clearly.

**Missing, at P0:**

| Gap | Impact |
|---|---|
| **GAP-006 — consent rules not executable** | Consent service cannot be specified, built, or tested. Control C2 has no implementation basis |
| **GAP-007 — suitability content undefined** | The gate is "locked" as a principle with no content. Control C1 has no implementation basis. **The delivered quote path therefore has no suitability gate** |
| Data classification | Partial — no field-level classification matrix |
| Retention schedule | Partial — 7 years is stated; per-class schedule and disposal method are not |

**This is the single highest-priority content gap in the programme.** Both packs are Product and
Compliance work requiring no engineering capacity, and both block S11 entry under Rule SM-4. They
can and should start immediately, in parallel with the S08/S09 foundation work.

## 7. Premature at this stage

Control implementation · security architecture · encryption mechanism selection · audit schema
design.

S02 states *what must be true*. How it is enforced is S07 (architecture) and S08–S11
(implementation). Choosing a mechanism here forecloses designs that might satisfy the obligation
better.
