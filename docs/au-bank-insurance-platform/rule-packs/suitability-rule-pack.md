# Suitability Rule Pack v1 — AU Bank Insurance Distribution Platform

**Pack ID:** `SUITABILITY-PACK-v1.0` · **Algorithm ID:** `SUIT-ALGO-LIFE-v1.0`
**Closes:** [GAP-007 — suitability content & override unknown](../po-drive/02-GAP-REGISTER.md) (P0, build-freeze)
**Satisfies:** [S02-E04](../../application-lifecycle-bible/stages/S02-regulatory-framing.md#3-epics-and-stories) · gate criterion S02-G4
**Author:** Rajal — Principal Insurance Platform Product Owner (Board 3 / R1)
**Date:** 2026-08-16
**Scope of this version:** R0 — Life LOB (Term, Savings/Endowment, ULIP), ETB customers, RM-assisted

> **Authority boundary.** The suitability *hard-gate* is already a locked decision
> ([D-005](../DECISION-LOG.md), [WD §8](../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md#8-suitability--need-analysis)):
> bypassing suitability before quote is described in our own baseline as illegal. What was missing
> was **content** — the questions, the arithmetic, the outcome bands, and what "valid evaluation
> ID" means. That is what this pack supplies.
>
> Product owns the question set, the deterministic model and the outcome semantics. **Compliance
> owns whether this model discharges the IRDAI suitability obligation**, and owns the override
> policy in §7. **Signature status:** `AI-DRAFTED — mandatory human signature outstanding`.

---

## 1. The gate, stated once, precisely

> **SUIT-R20 — Quote endpoints return `403` unless the request carries a suitability evaluation ID
> that is `ACTIVE`, issued within 30 days, bound to the same `customerCif`, and whose outcome
> permits the requested product class.**

Four conditions. All four must hold. Each is separately testable in §6. Everything else in this
pack exists to make that sentence executable.

---

## 2. The need-analysis question set

Twelve inputs. Four are derived from CBS and never asked; eight are asked. A questionnaire longer
than this in an RM-assisted branch conversation is completed dishonestly, which defeats its
purpose.

### 2.1 Derived inputs (no question asked)

| ID | Input | Type | Source | If unavailable |
|---|---|---|---|---|
| `SQ-D01` | Age at next birthday | Integer | CBS DOB | **INSUFFICIENT_DATA** — journey blocked |
| `SQ-D02` | Customer relationship tenure (months) | Integer | CBS | Default 0; no block |
| `SQ-D03` | Existing AU-distributed life cover (sum assured) | Currency | Policy service | Default 0; recorded as `ASSUMED_ZERO` |
| `SQ-D04` | Known outstanding AU loan liability | Currency | CBS | Default 0; recorded as `ASSUMED_ZERO` |

### 2.2 Asked questions

| ID | Question | Answer type | Mandatory | Validation |
|---|---|---|---|---|
| `SQ-01` | Annual income (self-declared, ₹) | Currency band, 8 bands | ✅ | > 0; band boundaries in §3.1 |
| `SQ-02` | Number of financially dependent persons | Integer 0–10 | ✅ | 0 ≤ n ≤ 10 |
| `SQ-03` | Total existing life cover from **all** insurers (₹) | Currency | ✅ | ≥ 0; ≥ `SQ-D03` or flagged inconsistent |
| `SQ-04` | Total outstanding liabilities across all lenders (₹) | Currency | ✅ | ≥ 0; ≥ `SQ-D04` or flagged inconsistent |
| `SQ-05` | Primary financial objective | Enum: `PROTECTION`, `SAVINGS_GOAL`, `RETIREMENT`, `WEALTH_GROWTH`, `TAX_PLANNING` | ✅ | one value |
| `SQ-06` | Investment horizon | Enum: `LT_5Y`, `Y5_10`, `Y10_20`, `GT_20Y` | ✅ | one value |
| `SQ-07` | Attitude to investment risk | Enum: `CAPITAL_PROTECTION`, `LOW`, `MODERATE`, `HIGH` | ✅ | one value |
| `SQ-08` | Premium the customer is comfortable committing annually (₹) | Currency | ✅ | > 0 |
| `SQ-09` | Tobacco use in the last 12 months | Boolean | ✅ | — |
| `SQ-10` | Occupation category | Enum: `SALARIED`, `SELF_EMPLOYED_PROF`, `SELF_EMPLOYED_BUSINESS`, `AGRICULTURE`, `OTHER` | ✅ | one value |
| `SQ-11` | Has the customer been declined or rated by any life insurer? | Enum: `NO`, `YES`, `DONT_KNOW` | ✅ | — |
| `SQ-12` | Is the customer purchasing to replace an existing policy? | Boolean | ✅ | If true → §7.4 replacement disclosure |

| Rule | Statement | Test |
|---|---|---|
| **SUIT-R01** | All twelve inputs MUST be present before an evaluation is computed. A partial questionnaire produces no evaluation ID. | Submit 11 of 12 → `422 SUITABILITY_INCOMPLETE`, listing the missing IDs. No evaluation row is written. |
| **SUIT-R02** | Answers MUST be stored verbatim alongside the outcome, with the questionnaire version. | Retrieve an evaluation; all 12 raw answers and `questionnaireVersion` are present. |
| **SUIT-R03** | Where a self-declared value (`SQ-03`, `SQ-04`) is lower than the bank-known value (`SQ-D03`, `SQ-D04`), the **higher** value MUST be used and the discrepancy recorded. | Set `SQ-03` = 0 with `SQ-D03` = 500000. Computation uses 500000; `dataInconsistencyFlags` contains `EXISTING_COVER_UNDERSTATED`. |
| **SUIT-R04** | `SQ-11 = YES` or `DONT_KNOW` MUST set `underwritingRiskFlag = true` on the evaluation. It does not by itself change the outcome. | Answer `YES`; flag set; outcome computed by §4 unchanged. |

---

## 3. Deterministic computation

The model is arithmetic, not judgement. Two people running it by hand on the same inputs get the
same answer — that is [S02-VT-04](../../application-lifecycle-bible/stages/S02-regulatory-framing.md#4-validation-tests).

### 3.1 Income bands

| Band | Annual income (₹) | `incomeMultiple` |
|---|---|---|
| `B1` | < 3,00,000 | 10 |
| `B2` | 3,00,000 – 4,99,999 | 12 |
| `B3` | 5,00,000 – 9,99,999 | 15 |
| `B4` | 10,00,000 – 19,99,999 | 18 |
| `B5` | 20,00,000 – 34,99,999 | 20 |
| `B6` | 35,00,000 – 49,99,999 | 20 |
| `B7` | 50,00,000 – 99,99,999 | 18 |
| `B8` | ≥ 1,00,00,000 | 15 |

The multiple tapers at both ends: low bands because affordability binds before need does, high
bands because insurer underwriting limits bind. Band boundaries are inclusive-lower,
exclusive-upper.

### 3.2 Age adjustment

| Age at next birthday | `ageFactor` |
|---|---|
| 18–30 | 1.10 |
| 31–40 | 1.00 |
| 41–50 | 0.85 |
| 51–60 | 0.65 |
| 61–65 | 0.45 |
| < 18 or > 65 | **INELIGIBLE** — see SUIT-R08 |

### 3.3 The computation

```
recommendedCover  = ROUND_UP_TO_LAKH(
                       (annualIncome × incomeMultiple × ageFactor)
                       + liabilities
                       + (dependants × 5,00,000)
                    )

protectionGap     = MAX(0, recommendedCover − existingCover)

affordabilityRatio = declaredPremium ÷ annualIncome
```

Where `liabilities = MAX(SQ-04, SQ-D04)` and `existingCover = MAX(SQ-03, SQ-D03)` per SUIT-R03.

| Rule | Statement | Test |
|---|---|---|
| **SUIT-R05** | The computation MUST be pure: same inputs → same outputs, with no dependence on time, actor, or system state other than the algorithm version. | Run the same 20 profiles twice, on two days, by two evaluators. Outputs identical, byte for byte. |
| **SUIT-R06** | `ROUND_UP_TO_LAKH` rounds up to the next multiple of 1,00,000. | 12,34,567 → 13,00,000. 12,00,000 → 12,00,000. |
| **SUIT-R07** | The algorithm version MUST be recorded on every evaluation and MUST be immutable once published. | Every evaluation has `algorithmVersion`; publishing a change under an existing version is rejected. |
| **SUIT-R08** | Age < 18 or > 65 at next birthday MUST produce outcome `NOT_SUITABLE` with reason `AGE_OUTSIDE_ELIGIBLE_RANGE`, and no quote may proceed. | Age 17 and age 66 → `NOT_SUITABLE`; quote returns 403. |

### 3.4 Worked example (the reference test case)

> Age 38 · income ₹12,00,000 (B4, multiple 18, ageFactor 1.00) · 2 dependants · existing cover
> ₹5,00,000 · liabilities ₹30,00,000 · declared premium ₹25,000.
>
> `recommendedCover = ROUND_UP_TO_LAKH(12,00,000 × 18 × 1.00 + 30,00,000 + 10,00,000)`
> `= ROUND_UP_TO_LAKH(2,56,00,000) = ₹2,56,00,000`
> `protectionGap = 2,56,00,000 − 5,00,000 = ₹2,51,00,000`
> `affordabilityRatio = 25,000 ÷ 12,00,000 = 0.0208`

This case is `SUIT-TC-REF-01` and is a mandatory unit test on the Suitability service.

---

## 4. Product-class outcomes

An evaluation produces one outcome **per product class**, not one overall. A customer may be
`SUITABLE` for Term and `NOT_SUITABLE` for ULIP in the same evaluation, and the gate is applied per
class.

### 4.1 Outcome vocabulary

| Outcome | Meaning | Quote permitted |
|---|---|---|
| `SUITABLE` | The product class matches the stated objective, horizon, risk attitude and affordability | ✅ |
| `SUITABLE_WITH_CAUTION` | Match is defensible but at least one caution condition applies; disclosure required | ✅ with disclosure (§7.2) |
| `NOT_SUITABLE` | The product class conflicts with a stated input | ⛔ **hard block** |
| `INSUFFICIENT_DATA` | A mandatory derived input was unavailable | ⛔ hard block |

### 4.2 Term Life

| Rule | Condition | Outcome |
|---|---|---|
| **SUIT-R09** | `protectionGap > 0` AND age eligible | `SUITABLE` |
| **SUIT-R10** | `protectionGap = 0` (customer already adequately covered) | `SUITABLE_WITH_CAUTION`, reason `COVER_ALREADY_ADEQUATE` |
| **SUIT-R11** | `affordabilityRatio > 0.15` | `SUITABLE_WITH_CAUTION`, reason `PREMIUM_HIGH_RELATIVE_TO_INCOME` |
| **SUIT-R12** | `affordabilityRatio > 0.30` | `NOT_SUITABLE`, reason `PREMIUM_UNAFFORDABLE` |

### 4.3 Savings / Endowment

| Rule | Condition | Outcome |
|---|---|---|
| **SUIT-R13** | `SQ-05 ∈ {SAVINGS_GOAL, RETIREMENT, TAX_PLANNING}` AND `SQ-06 ≥ Y5_10` AND `affordabilityRatio ≤ 0.20` | `SUITABLE` |
| **SUIT-R14** | `SQ-06 = LT_5Y` | `NOT_SUITABLE`, reason `HORIZON_TOO_SHORT_FOR_PRODUCT_CLASS` |
| **SUIT-R15** | `protectionGap > 0` AND `SQ-05 = PROTECTION` | `SUITABLE_WITH_CAUTION`, reason `PROTECTION_NEED_UNMET_BY_THIS_CLASS` |

### 4.4 ULIP

| Rule | Condition | Outcome |
|---|---|---|
| **SUIT-R16** | `SQ-07 ∈ {MODERATE, HIGH}` AND `SQ-06 ∈ {Y10_20, GT_20Y}` AND `SQ-05 ∈ {WEALTH_GROWTH, RETIREMENT}` AND `affordabilityRatio ≤ 0.20` | `SUITABLE` |
| **SUIT-R17** | `SQ-07 = CAPITAL_PROTECTION` | `NOT_SUITABLE`, reason `RISK_ATTITUDE_INCOMPATIBLE` |
| **SUIT-R18** | `SQ-06 ∈ {LT_5Y, Y5_10}` | `NOT_SUITABLE`, reason `HORIZON_TOO_SHORT_FOR_PRODUCT_CLASS` |
| **SUIT-R19** | `SQ-07 = LOW` AND all other conditions of SUIT-R16 met | `SUITABLE_WITH_CAUTION`, reason `RISK_ATTITUDE_BELOW_PRODUCT_PROFILE` |

**Precedence:** where more than one rule fires for a class, the **most restrictive** outcome wins
(`NOT_SUITABLE` > `SUITABLE_WITH_CAUTION` > `SUITABLE`), and **every** fired reason code is
recorded, not only the winning one.

| Rule | Statement | Test |
|---|---|---|
| **SUIT-R19a** | All fired reason codes MUST be persisted in rule-ID order. | A profile firing SUIT-R11 and SUIT-R15 records both codes; outcome is the more restrictive. |

---

## 5. The evaluation record

| # | Field | Type | Optionality |
|---|---|---|---|
| 1 | `suitabilityEvaluationId` | UUID | Mandatory — **this is the ID the quote gate checks** |
| 2 | `customerCif` | String | Mandatory |
| 3 | `journeyId` | UUID | Mandatory |
| 4 | `questionnaireVersion` | String | Mandatory |
| 5 | `algorithmVersion` | String | Mandatory (`SUIT-ALGO-LIFE-v1.0`) |
| 6 | `answers` | Map of 12 inputs, verbatim | Mandatory |
| 7 | `derivedValues` | `recommendedCover`, `protectionGap`, `affordabilityRatio`, `incomeMultiple`, `ageFactor` | Mandatory |
| 8 | `outcomesByProductClass` | Map: class → `{outcome, reasonCodes[]}` | Mandatory |
| 9 | `underwritingRiskFlag` | Boolean | Mandatory |
| 10 | `dataInconsistencyFlags` | Array | Mandatory (may be empty) |
| 11 | `evaluatedByActorId` | String | Mandatory (assisted) |
| 12 | `evaluatedByAgentId` | String (SP licence) | Mandatory (assisted) |
| 13 | `evaluatedAt` | Timestamp (UTC, ISO-8601 + offset) | Mandatory |
| 14 | `validUntil` | Timestamp | Mandatory = `evaluatedAt` + 30 days |
| 15 | `status` | Enum `ACTIVE\|EXPIRED\|INVALIDATED\|SUPERSEDED` | Mandatory |
| 16 | `invalidationReason` | Enum (§6.3) | Optional |
| 17 | `consentIdSol` | UUID → `CNS-SOL` | **Mandatory** |
| 18 | `pdfArtefactRef` | S3 object reference | Mandatory |

| Rule | Statement | Test |
|---|---|---|
| **SUIT-R21** | An evaluation MUST NOT be created without a valid `CNS-SOL` consent (field 17). | Attempt evaluation with no `CNS-SOL` → `403 CONSENT_REQUIRED`. This is [CNS-R37](./consent-rule-pack.md#9-failure-behaviour) applied at the suitability boundary. |
| **SUIT-R22** | A human-readable suitability PDF MUST be generated at evaluation time and stored immutably; it is the artefact shown to the customer and retained for audit. | Every evaluation has a resolvable `pdfArtefactRef`; the PDF contains all 12 answers, the derived values, and every outcome with its reason codes. |
| **SUIT-R23** | The evaluation store MUST be append-only, on the same terms as [CNS-R16–R18](./consent-rule-pack.md#51-immutability). | UPDATE/DELETE by the application role fails on privilege. |
| **SUIT-R24** | Retention: 7 years from journey terminal state, write-once, AWS India region. | Same test as [CNS-R33–R34](./consent-rule-pack.md#8-retention-and-retrieval). |

---

## 6. The hard gate

### 6.1 The rule

**SUIT-R20** (restated from §1): quote endpoints return `403` unless the request carries a
suitability evaluation ID that is `ACTIVE`, issued within 30 days, bound to the same `customerCif`,
and whose outcome for the requested product class is `SUITABLE` or `SUITABLE_WITH_CAUTION`.

### 6.2 Test matrix — all seven cases are mandatory tests

| # | Condition | Expected | Error code |
|---|---|---|---|
| 1 | No evaluation ID on the request | `403` | `SUITABILITY_EVALUATION_REQUIRED` |
| 2 | Evaluation ID does not exist | `403` | `SUITABILITY_EVALUATION_NOT_FOUND` |
| 3 | Evaluation `evaluatedAt` older than 30 days | `403` | `SUITABILITY_EVALUATION_EXPIRED` |
| 4 | Evaluation `customerCif` ≠ request CIF | `403` | `SUITABILITY_SUBJECT_MISMATCH` + **security event** |
| 5 | Outcome for requested class is `NOT_SUITABLE` | `403` | `PRODUCT_CLASS_NOT_SUITABLE` |
| 6 | Outcome is `INSUFFICIENT_DATA` | `403` | `SUITABILITY_INSUFFICIENT_DATA` |
| 7 | Status is `INVALIDATED`, `EXPIRED` or `SUPERSEDED` | `403` | `SUITABILITY_EVALUATION_INVALID` |

| Rule | Statement | Test |
|---|---|---|
| **SUIT-R25** | The gate MUST be enforced **server-side at the quote API**, not in the UI, not in the BFF alone, and not by the aggregator. | Call the quote API directly, bypassing the UI and the BFF, with no evaluation ID. `403`. |
| **SUIT-R26** | The gate MUST fail closed. If the Suitability service is unavailable, quotes are refused, not permitted. | Take the Suitability service down; quote requests return `503 SUITABILITY_SERVICE_UNAVAILABLE`. **No quote is generated.** |
| **SUIT-R27** | Control-path code implementing SUIT-R20 MUST hold **100% branch coverage**, with no waiver. | Coverage report filtered to the gate class shows 100% branch. This is [S08-E02-S06 / S08-VT-07](../../application-lifecycle-bible/stages/S08-engineering-foundation.md#4-validation-tests). |
| **SUIT-R28** | Every gate refusal MUST emit an audit event carrying the actor, CIF, product class, evaluation ID (if any) and reason code. | Trigger each of the seven cases; seven audit events exist with distinct reason codes. |

> **SUIT-R26 is the rule that would have prevented this programme's central defect.** A gate that
> fails open under load is a gate that is absent exactly when volume is highest.

### 6.3 Invalidation and re-evaluation triggers

| Rule | Trigger | Effect |
|---|---|---|
| **SUIT-R29** | 30 days elapse since `evaluatedAt` | `status → EXPIRED` |
| **SUIT-R30** | `CNS-SOL` consent withdrawn | `status → INVALIDATED`, reason `CONSENT_WITHDRAWN` |
| **SUIT-R31** | A new evaluation is created for the same CIF | Prior evaluation `status → SUPERSEDED`; the new ID must be used |
| **SUIT-R32** | A MAJOR change to `questionnaireVersion` or `algorithmVersion` is published | All `ACTIVE` evaluations under the prior version → `INVALIDATED`, reason `MODEL_VERSION_SUPERSEDED` |
| **SUIT-R33** | Customer-reported change to income, dependants, existing cover or liabilities | RM MUST re-evaluate; the platform offers the path, it cannot detect the change |
| **SUIT-R34** | An evaluation whose journey has reached `PROPOSAL_SUBMITTED` MUST NOT be invalidated by SUIT-R29 or SUIT-R32 | Age the evaluation past 30 days post-submission; the in-flight proposal is unaffected and retains its evidence |

**SUIT-R34 mirrors [CNS-R05](./consent-rule-pack.md#32-version-change-and-in-flight-journeys).** A
submitted proposal is a completed regulated act; retroactively invalidating its basis creates an
audit contradiction, not a control.

### 6.4 Reuse

| Rule | Statement | Test |
|---|---|---|
| **SUIT-R35** | An `ACTIVE` evaluation MAY be reused across journeys for the same CIF within its 30-day window. | Abandon journey 1; start journey 2 at T+10d for the same CIF. The existing evaluation is reused; no re-questionnaire. |
| **SUIT-R36** | An evaluation MUST NOT be reused across CIFs under any circumstances. | Covered by test case 4 in §6.2. |
| **SUIT-R37** | Reuse MUST be recorded: each journey referencing an evaluation appends a usage row (`journeyId`, `usedAt`, `actorId`). | Reuse an evaluation across two journeys; two usage rows exist. |

---

## 7. Override and exception path

This is the section where a suitability control usually fails in practice, because a bank that
makes overriding easy has an advisory process with a bypass button.

### 7.1 What may never be overridden

| Rule | Statement |
|---|---|
| **SUIT-R38** | `NOT_SUITABLE` MUST NOT be overridden by any actor at any level in R0. There is no override path, no approval workflow, and no configuration flag. The quote is refused. |
| **SUIT-R39** | `INSUFFICIENT_DATA` MUST NOT be overridden. The missing input is obtained or the journey stops. |
| **SUIT-R40** | The hard gate itself (SUIT-R20) MUST NOT be disabled by feature flag, configuration, environment variable, or header in any environment that holds customer data. | Search the codebase for any conditional that can disable the gate; assert none exists. Attempt to pass a bypass header in UAT; `403`. |

**Test for SUIT-R38:** attempt an override through every surface — UI, API, admin console, direct
service call. All four refused. Absence of a feature is proven by attempting to use it.

### 7.2 `SUITABLE_WITH_CAUTION` — proceed with disclosure, not with override

A caution outcome is not an exception. It permits the quote, and it obliges a disclosure.

| Rule | Statement | Test |
|---|---|---|
| **SUIT-R41** | Where any product class returns `SUITABLE_WITH_CAUTION`, the customer MUST be shown the specific caution reason in plain language before proceeding, and MUST acknowledge it. | Trigger SUIT-R11; the disclosure names the affordability concern specifically, not a generic warning. |
| **SUIT-R42** | The acknowledgement MUST be captured on the customer's own device with OTP verification, on the same terms as consent ([CNS-R10, CNS-R13](./consent-rule-pack.md#4-capture-mechanism-by-channel)). | The RM cannot acknowledge a caution disclosure on the RM device. |
| **SUIT-R43** | The acknowledgement record MUST carry the reason codes acknowledged, the disclosure text and version, timestamp, and OTP transaction. | Retrieve the record; all fields present. |

### 7.3 Customer-initiated deviation

A customer may want a product the model did not recommend, where that product is nonetheless
`SUITABLE` or `SUITABLE_WITH_CAUTION` for them. This is legitimate and is **not** an override.

| Rule | Statement | Test |
|---|---|---|
| **SUIT-R44** | The customer MAY select any product whose class outcome is `SUITABLE` or `SUITABLE_WITH_CAUTION`, including one the model did not rank first. The selection is recorded with `selectionDivergedFromRecommendation = true` and a customer-stated reason code. | Select a non-top-ranked eligible product; the flag and reason code persist on the journey. |
| **SUIT-R45** | A diverged selection MUST NOT change the outcome of any class. The model result is immutable evidence of what was assessed. | After divergence, re-read the evaluation; outcomes unchanged. |

### 7.4 Replacement (`SQ-12 = true`)

| Rule | Statement | Test |
|---|---|---|
| **SUIT-R46** | Where the customer is replacing an existing policy, a replacement disclosure MUST be presented and acknowledged on the customer's device before quote, covering loss of accrued benefit, fresh waiting/contestability periods, and re-underwriting risk. | Set `SQ-12 = true`; quote is refused until the replacement acknowledgement exists (`403 REPLACEMENT_DISCLOSURE_REQUIRED`). |
| **SUIT-R47** | Replacement cases MUST be flagged for post-sale operations review. | An ops task of type `POLICY_REPLACEMENT_REVIEW` exists for every such journey. |

### 7.5 The exception that does exist

There is exactly one, and it is not a suitability override.

| Rule | Statement |
|---|---|
| **SUIT-R48** | Where the Suitability service produces an evaluation the business believes is **wrong because of a model defect** (not because the answer is inconvenient), the path is a **defect report against the algorithm version**, raised to Product and Compliance, resolved by publishing a corrected algorithm version under change control. It is never resolved for an individual customer in flight. |

**Rationale.** Per-case exceptions to a suitability model are indistinguishable, at audit, from
mis-selling. A model that is wrong for one customer is wrong for a population, and the correct
remedy operates on the population.

---

## 8. Traceability

| Rule range | Obligation | BRD requirement | Bounded context | Proven at |
|---|---|---|---|---|
| SUIT-R01–R04 | IRDAI documented need analysis | [BR-SUIT-010](../requirements/BRD-P0-CAPABILITIES.md#br-suit-010--capture-assessment) | #7 Suitability & Recommendation | S11 |
| SUIT-R05–R08 | Deterministic, reproducible advice | BR-SUIT-010 AC2 | #7 | S11 |
| SUIT-R09–R19a | IRDAI suitability of recommendation | [BR-SUIT-020](../requirements/BRD-P0-CAPABILITIES.md#br-suit-020--recommendation) | #7, #8 Product Catalogue | S11 |
| SUIT-R20–R28 | **IRDAI: suitability before quote** | [BR-SUIT-030](../requirements/BRD-P0-CAPABILITIES.md#br-suit-030--ineligibility), [BR-QUOTE-010](../requirements/BRD-P0-CAPABILITIES.md#br-quote--br-comp--quote--comparison-must) | #7, #10 Quotation | **S11, 100% branch coverage** |
| SUIT-R29–R37 | Currency of advice | BR-SUIT-010 | #7, #9 Journey Orchestration | S11 |
| SUIT-R38–R48 | Mis-selling prevention; customer protection | BR-SUIT-020 AC2 | #7, #16 Audit | S11 / S12 |

---

## 9. What this pack does not decide

| ID | Open item | Owner | Needed by |
|---|---|---|---|
| OPEN-SUIT-01 | Whether the income-multiple table and age factors discharge the IRDAI suitability obligation, or whether a prescribed model applies | Shailja | S11 entry |
| OPEN-SUIT-02 | Whether `NOT_SUITABLE` may ever be overridden with senior authorisation in a later release (R0 position: never) | Shailja + Sponsor | R1 |
| OPEN-SUIT-03 | Exact approved disclosure wording for §7.2 and §7.4 | Shailja + Legal | S05 copy deck |
| OPEN-SUIT-04 | Whether the 30-day validity window is regulator-acceptable | Shailja | S11 entry |
| OPEN-SUIT-05 | Insurer-specific suitability requirements that exceed this model (Group A panel) | Bancassurance + Shailja | S10 |

**None of these blocks building the Suitability service.** All five are calibration or wording
questions over a model whose structure, storage, gate and failure behaviour they do not change.

---

## 10. Ratification

| Authority | Required conclusion | Status |
|---|---|---|
| Shailja / Risk & Compliance (Board 6) | Whether this model discharges the IRDAI suitability obligation; override policy; disclosure wording | **PENDING — human signature mandatory (T4)** |
| Rajal / Product (Board 3) | Question set, outcome semantics, gate behaviour, divergence and replacement handling | **APPROVED as drafted — Rajal, 2026-08-16** |
| Deepali / Security (Board 4) | SUIT-R25, R26, R40 — server-side enforcement, fail-closed, no bypass | PENDING |
| Swapnali / QA (Board 5) | SUIT-R27 100% branch coverage; all seven §6.2 cases automatable | PENDING |
| Mahesh / Architecture (Board 1) | Gate placement at the quote API boundary; service availability coupling implied by SUIT-R26 | PENDING |

**Signature status:** `AI-DRAFTED — mandatory human signature outstanding`.
Silence does not approve this pack. Until Shailja signs, S02-G4 is **not** met and
[GAP-007](../po-drive/02-GAP-REGISTER.md) is *content-complete, ratification-pending* — not closed.

— **Rajal**, Principal Insurance Platform Product Owner, 2026-08-16
