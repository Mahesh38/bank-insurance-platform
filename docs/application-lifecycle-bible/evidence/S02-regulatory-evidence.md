# S02 — Regulatory, Risk & Compliance Framing · Retroactive Stage Evidence

**Stage definition:** [`stages/S02-regulatory-framing.md`](../stages/S02-regulatory-framing.md)
**Workstream:** WS-3 ([charter](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md))
**Stage owner:** Shailja S — Compliance & Risk Head (Board 6)
**Contributing author:** Rajal — Product (Board 3 / R1), for the two rule packs S02-E03 and S02-E04
assign jointly to Compliance **and Product**
**Date:** 2026-08-16

**Retroactive stage verdict: `CLOSED-WITH-CONDITIONS`** — see §7.

> **Authority statement, up front.** This is **Shailja's stage**. I am not entitled to conclude on
> regulatory permissibility, and nothing below does. What I am entitled to do — and what
> [S02-E03 and S02-E04](../stages/S02-regulatory-framing.md#3-epics-and-stories) explicitly assign
> to *Shailja + Rajal* — is supply the business behaviour, the journey binding points, the data
> fields and the failure semantics, in a form precise enough that a Compliance reviewer can accept,
> amend or reject each rule individually. That is what §4 delivers.
>
> **Every permissibility conclusion in this document is `AI-DRAFTED — Shailja to ratify`.**

---

## 1. What the stage requires

| # | Criterion | Level | Closes |
|---|---|---|---|
| S02-G1 | Regulatory registry complete with obligation IDs | E1 | |
| S02-G2 | Control catalogue maps every obligation to a control and its evidence | E2 | |
| **S02-G3** | **Consent rule pack v1 approved** | E2 | **GAP-006** |
| **S02-G4** | **Suitability rule pack v1 approved** | E2 | **GAP-007** |
| S02-G5 | Data classification and retention schedule approved | E2 | |
| S02-G6 | Data residency requirement stated and binding | E1 | |
| S02-G7 | Risk register established with acceptance authorities | E1 | |
| S02-G8 | Non-waivable control list agreed | E2 | |

**Approvers:** Shailja (AP, B, **human**) · Deepali (AP) · Rajal (RV) · Mahesh (RV) · Aarti (RV) ·
Swapnali (RV) · Shivanshi (RV)

---

## 2. What already exists

| Criterion | Artefact | Path | Assessment |
|---|---|---|---|
| S02-G1 | Regulatory registry | [`shailja-s-compliance-risk-head/02-regulatory-registry.md`](../../context/roles/shailja-s-compliance-risk-head/02-regulatory-registry.md) | 🟢 |
| S02-G1 | IRDAI CA0515 obligations, RBI payment isolation, mandatory suitability, consent/disclosure | [`business-problem-statement.md §1.2, §9.1`](../../context/business-problem-statement.md#12-regulatory--compliance-directives) | 🟢 Four hard-gates stated precisely, including the 403 behaviour |
| S02-G2 | Control catalogue | [`03-control-catalogue.md`](../../context/roles/shailja-s-compliance-risk-head/03-control-catalogue.md) | 🟢 Controls C1–C10 referenced across the bible |
| S02-G2 | Evidence policy | [`06-evidence-policy.md`](../../context/roles/shailja-s-compliance-risk-head/06-evidence-policy.md) | 🟢 |
| S02-G5 | Security/compliance canon incl. data handling | [`07-SECURITY-COMPLIANCE-CANON.md`](../07-SECURITY-COMPLIANCE-CANON.md) | 🟢 |
| S02-G5 | PII encryption, masking, 7-year retention statements | [`business-problem-statement.md §9.2`](../../context/business-problem-statement.md#92-security--data-governance-standards) | 🟡 Stated at policy level; **no field-level classification matrix, no per-class disposal method** |
| S02-G6 | Data residency — ap-south-1 primary, ap-south-2 DR, all data, backups, logs and archives | Same §9.2 | 🟢 Stated · 🔴 **not implemented** — no IaC exists ([GAP-B](../01-POSITION-ASSESSMENT.md#gap-b--there-is-no-environment-or-infrastructure-foundation--critical)) |
| S02-G7 | Risk taxonomy | [`04-risk-taxonomy.md`](../../context/roles/shailja-s-compliance-risk-head/04-risk-taxonomy.md) | 🟢 |
| S02-G7 | Risk register | [`registers/RISK-REGISTER.md`](../../governance/registers/RISK-REGISTER.md) | 🟢 RISK-001…011 |
| S02-G7 | Risk acceptance authority; human exception model | [`07-human-exception-and-risk-acceptance.md`](../../context/roles/shailja-s-compliance-risk-head/07-human-exception-and-risk-acceptance.md) | 🟢 |
| S02-G8 | Non-waivable list | [`04-GATE-AND-SIGNOFF-MODEL.md §8`](../04-GATE-AND-SIGNOFF-MODEL.md#8-waivers-and-exceptions) | 🟢 Six items, stated · 🟡 not separately signed by Security **and** Compliance as S02-VT-07 requires |
| S02-E02 | Security control model, DevSecOps, threat/incident policy | [`deepali-principal-security-architect/`](../../context/roles/deepali-principal-security-architect/README.md) 04–09 | 🟢 |

**Shailja's package is strong.** The registry, catalogue, taxonomy, evidence policy and exception
model are all present and better than most programmes hold at this stage.

---

## 3. What was missing

| Gap | Criterion | Severity | Impact |
|---|---|---|---|
| **GAP-006 — consent rules not executable** | S02-G3 | **P0, build freeze** | The Consent service could not be specified, built or tested. Control C2 had no implementation basis |
| **GAP-007 — suitability content undefined** | S02-G4 | **P0, build freeze** | The gate was "locked" as a principle with no content. Control C1 had no implementation basis. **The delivered quote path therefore has no suitability gate** |
| Field-level data classification matrix | S02-G5 | High | S07 encryption scope and S09 residency scope cannot be bounded without it |
| Per-class retention schedule and disposal method | S02-G5 | High | "7 years" is stated; what expires when, and how it is destroyed, is not |
| Non-waivable list signed by Security **and** Compliance | S02-G8, S02-VT-07 | Medium | The list exists; the signature does not |

[S02 §6](../stages/S02-regulatory-framing.md#6-current-position-in-this-repository---partial-with-two-p0-holes)
calls GAP-006 and GAP-007 *"the single highest-priority content gap in the programme"* and notes
that both are **Product and Compliance work requiring no engineering capacity**. That is why they
are closed here rather than scheduled behind the foundation recovery.

---

## 4. New evidence added

### 4.1 Consent rule pack — [`rule-packs/consent-rule-pack.md`](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md)

`CONSENT-PACK-v1.0` · 38 rules, `CNS-R01`–`CNS-R38`, each with an ID, a normative statement and an
executable pass/fail test.

| Section | Content | Satisfies |
|---|---|---|
| §2 | Five consent events with journey binding points and blocking effects | S02-E03-S01 |
| §3 | Statement identity, immutable versioning, MAJOR/MINOR change semantics, in-flight journey handling, language variants | S02-E03-S02 |
| §4 | Capture per channel: assisted OTP to the **customer's** device; DIY affirmative action + OTP. Identical evidence either way. CNS-R13 forbids the RM entering the customer's OTP | S02-E03-S03 |
| §5 | The 24-field evidence record — statement text and hash, CIF, OTP txn ID, timestamp, IP, actor, agent, distributorId — plus append-only enforcement **by database grant, not by code** (CNS-R16–R18) | S02-E03-S04 |
| §6 | Validity windows (90d / 30d / none), CIF binding, cross-journey reuse rules | S02-E03-S05 |
| §7 | Withdrawal, including CNS-R30: post-submission withdrawal is recorded and escalated, **never represented to the customer as retracting a submission** | S02-E03-S05 |
| §8 | 7-year retention from journey terminal state, S3 Object Lock compliance mode, ap-south-1/2, 4-hour retrieval SLA | S02-E03-S06 |
| §9 | Complete failure behaviour: nine conditions, HTTP status, error code, RM-facing message, journey effect. CNS-R37 — **fail closed** | S02-VT-03 |

**Against [S02-VT-03](../stages/S02-regulatory-framing.md#4-validation-tests)** — *"give the pack to
an engineer and a tester independently; both produce the same behaviour description"*: every rule
carries its own test, and §9 fixes the observable contract. I believe it passes; **Swapnali's Board
5 verdict is where that is actually decided**, and she has flagged the ambiguity she found — see
[board-5-qa-swapnali.md](../../governance/change-requests/CR-010/verdicts/board-5-qa-swapnali.md).

### 4.2 Suitability rule pack — [`rule-packs/suitability-rule-pack.md`](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md)

`SUITABILITY-PACK-v1.0` · algorithm `SUIT-ALGO-LIFE-v1.0` · 48 rules, `SUIT-R01`–`SUIT-R48`.

| Section | Content | Satisfies |
|---|---|---|
| §2 | 12 inputs — 4 derived from CBS, 8 asked. Mandatory-field rules; the bank-known-value-wins rule (SUIT-R03) | S02-E04-S01 |
| §3 | Deterministic arithmetic: income bands → multiple, age → factor, human-life-value cover computation, affordability ratio. A worked reference case, `SUIT-TC-REF-01`, as a mandatory unit test | S02-E04-S02 |
| §4 | Outcomes **per product class** — Term, Savings, ULIP — with `SUITABLE` / `SUITABLE_WITH_CAUTION` / `NOT_SUITABLE` / `INSUFFICIENT_DATA`, most-restrictive precedence, and all fired reason codes retained | S02-E04-S02 |
| §5 | The 18-field evaluation record, including the mandatory `CNS-SOL` consent link and an immutable customer-facing PDF | S02-E04-S05 |
| §6 | **The hard gate.** Four conditions; a seven-case test matrix; server-side enforcement at the quote API (SUIT-R25); **fail closed on service unavailability** (SUIT-R26); **100% branch coverage with no waiver** (SUIT-R27) | S02-E04-S03 |
| §6.3 | Six invalidation and re-evaluation triggers, with SUIT-R34 protecting submitted proposals from retroactive invalidation | S02-E04-S06 |
| §7 | Override policy: **`NOT_SUITABLE` cannot be overridden by anyone in R0** (SUIT-R38); the gate cannot be disabled by flag, config or header in any environment holding customer data (SUIT-R40); caution outcomes proceed on customer-device disclosure, not override; customer-initiated divergence is recorded, not treated as an exception; replacement cases require disclosure and ops review | S02-E04-S04 |

**Against [S02-VT-04 and S02-VT-05](../stages/S02-regulatory-framing.md#4-validation-tests)** —
*"deterministic"* and *"what exactly makes a suitability ID valid?"*: SUIT-R05 requires purity;
§3.4 supplies a worked reference case; and the answer to VT-05 is a four-condition rule stated in
one sentence at §1 of the pack, not a judgement.

### 4.3 What the packs deliberately do **not** decide

Each pack ends with an open-items section rather than a false completeness. Five items each:

| Pack | Open items | All owned by |
|---|---|---|
| Consent | Consolidation of `CNS-DP` + `CNS-SOL`; approved legal wording; whether the 90/30-day windows are acceptable; post-issuance withdrawal obligations; DPDP Consent Manager interoperability | **Shailja** |
| Suitability | Whether the income-multiple model discharges the IRDAI obligation; whether `NOT_SUITABLE` may ever be overridden in a later release; disclosure wording; whether 30 days is acceptable; insurer-specific suitability requirements | **Shailja** (+ Bancassurance, Sponsor) |

**None of the ten blocks building the services.** All ten are calibration, wording or
interoperability questions over data models, control points and failure semantics they do not
change. That is the entire point of specifying structure before text: it lets engineering start
while Compliance finishes.

---

## 5. The permissibility conclusion — Shailja's to make

I record the following as **findings for Board 6 to ratify**, not conclusions:

| # | Finding | Why it matters | Shailja's call |
|---|---|---|---|
| F1 | The R0 activity — multi-insurer life distribution under CA0515 — appears within licence, on the open-architecture provisions cited in [business-problem-statement §1.1](../../context/business-problem-statement.md#11-irdai-registration--open-architecture-mandate) | S00-G3 depends on it | **PENDING** |
| F2 | The consent pack's evidence record appears to satisfy "explicitly captured, timestamped, and stored immutably with full audit trails" | S02-G3 | **PENDING** |
| F3 | The suitability pack's hard gate appears to satisfy "product recommendations strictly preceded by documented customer suitability analysis" | S02-G4 | **PENDING** |
| F4 | **A delivered, hardened quote path currently exists with no suitability gate in front of it.** This is a live regulatory exposure, not a future one | It is the reason WS-1 Phase 5 is stopped | **PENDING — I recommend a risk-register entry with a named human risk owner** |
| F5 | Data residency is stated as a requirement and **has no technical control behind it**; there is no IaC and Render.com's region is unchosen and unverified | Non-waivable per [gate model §8](../04-GATE-AND-SIGNOFF-MODEL.md#8-waivers-and-exceptions) | **PENDING** |
| F6 | 7-year retention is stated as a policy and **has no technical control behind it**; no S3 Object Lock configuration exists | Statutory obligation | **PENDING** |

> **F4 is the one I would put in front of Compliance first.** [Position assessment §4 GAP-C](../01-POSITION-ASSESSMENT.md#gap-c--the-compliance-hard-gates-that-make-this-business-legal-are-not-implemented--critical)
> records it: we delivered a quote path and moved it to hardening while a P0 gap labelled *build
> freeze* was open. The quote path we hardened is one that, in production, would generate quotes
> without the suitability gate IRDAI requires. This is not a missing feature — it is a **hardened
> path that is not lawfully shippable**, and hardening it further does not change that. The
> mitigation available today is that it is not in production and Phase 5 is stopped. Whether that
> mitigation is sufficient, and whether the exposure needs a named human risk owner now, is
> Shailja's determination.

---

## 6. What remains genuinely open

| ID | Item | Criterion | Owner | Target | Evidence needed |
|---|---|---|---|---|---|
| **GAP-006** | Consent rule pack **signed** | S02-G3 (E2) | **Shailja** | 2026-09-12 | Compliance signature. Content is complete |
| **GAP-007** | Suitability rule pack **signed** | S02-G4 (E2) | **Shailja** | 2026-09-12 | Compliance signature. Content is complete |
| S02-OPEN-01 | Ten open items inside the two packs (§4.3) | S02-G3, S02-G4 | Shailja (+ Legal, Bancassurance, Sponsor) | 2026-09-12 | Written determinations |
| S02-OPEN-02 | Field-level data classification matrix across the R0 information model | S02-G5 | Shailja + Deepali + Aarti | 2026-09-26 | Classification matrix, 100% of sampled fields (S02-VT-06) |
| S02-OPEN-03 | Per-class retention schedule with legal basis and disposal method | S02-G5 | Shailja + Aarti | 2026-09-26 | Retention schedule |
| S02-OPEN-04 | Non-waivable control list signed by Security **and** Compliance | S02-G8, S02-VT-07 | Deepali + Shailja | 2026-09-12 | Signed list, no reservations |
| S02-OPEN-05 | Risk-register entry for F4, with a **named human** risk owner and expiry | S02-G7 | Shailja | **2026-08-29** | RISK-0xx row |
| S02-OPEN-06 | Residency implemented, not merely stated (F5) | S02-G6 | Shivanshi + Deepali | **S09 gate** | Terraform + region evidence (E4) |
| S02-OPEN-07 | 7-year Object Lock implemented (F6) | S02-G5 | Shivanshi + Aarti | **S09 gate** | Bucket configuration evidence (E4) |
| S02-OPEN-08 | S02-VT-03 and VT-04 executed independently (engineer + tester; 20 profiles by two people) | S02-G3, S02-G4 | Swapnali + Amit | 2026-09-26 | Test records |

**S02-OPEN-05 has the nearest date deliberately.** A known regulatory exposure with no named human
risk owner is the specific condition the exception model exists to prevent, and it should not wait
on the rule packs' signature.

---

## 7. Retroactive stage verdict

> ## `CLOSED-WITH-CONDITIONS`

| Criterion | State | Basis |
|---|---|---|
| S02-G1 Regulatory registry | **MET** | Shailja's registry + business problem statement §1.2 |
| S02-G2 Control catalogue with evidence per control | **MET at E2** | Control catalogue + evidence policy, both signed within Shailja's package |
| **S02-G3 Consent rule pack approved** | **NOT MET** | Pack complete and testable (§4.1); **Compliance signature outstanding**. GAP-006 stays open |
| **S02-G4 Suitability rule pack approved** | **NOT MET** | Pack complete and testable (§4.2); **Compliance signature outstanding**. GAP-007 stays open |
| S02-G5 Data classification and retention approved | **PARTIAL** | Policy-level statements exist; field-level matrix and per-class schedule do not. S02-OPEN-02, -03 |
| S02-G6 Data residency stated and binding | **MET as a statement, UNIMPLEMENTED as a control** | Stated in §9.2 and on the non-waivable list; no technical control exists. S02-OPEN-06 |
| S02-G7 Risk register with acceptance authorities | **MET**, with a gap in *use* | Register and authority model present; F4 has no risk-register entry. S02-OPEN-05 |
| S02-G8 Non-waivable list agreed | **PARTIAL** | List published; dual signature absent. S02-OPEN-04 |

**Why `CLOSED-WITH-CONDITIONS`:** the two P0 content gaps that made this the programme's worst
stage are now **content-complete** — 38 consent rules and 48 suitability rules, every one with an
executable test. Every remaining item is a signature, a matrix, or an infrastructure control with a
named owner and a date.

**Why the P0 gaps are not closed:** because a rule pack is an E2 artefact and E2 means *reviewed and
signed*. An AI drafting Compliance's reasoning does not discharge a mandatory human Compliance
signature, and [CR-010 §2](../../governance/change-requests/CR-010-context-module-and-safe-autopilot.md#2-non-negotiable-automation-boundary)
forbids treating silence as approval. **GAP-006 and GAP-007 remain OPEN, and therefore
[Rule SM-4](../02-STAGE-MODEL.md#54-freeze-semantics) continues to freeze S11 entry.** That freeze
is the correct outcome and I am not looking for a way around it.

**Conditions carried forward:** GAP-006, GAP-007, S02-OPEN-01 through -08.

**Signature status:** `AI-DRAFTED — mandatory human signature outstanding`.
S02 approvers are **Shailja (AP, B, human)** and Deepali (AP). Six of eight criteria require E2.
No AI output satisfies any of them. Silence does not approve this stage.

— **Rajal**, Principal Insurance Platform Product Owner, 2026-08-16
*(contributing author; stage owner is Shailja S, Board 6)*
