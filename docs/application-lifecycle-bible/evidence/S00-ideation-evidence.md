# S00 — Ideation & Business Case · Retroactive Stage Evidence

**Stage definition:** [`stages/S00-ideation.md`](../stages/S00-ideation.md)
**Workstream:** WS-3 — AU Bank Insurance Distribution Platform ([charter](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md))
**Author:** Rajal — Principal Insurance Platform Product Owner (Board 3 / R1)
**Date:** 2026-08-16
**Entered under:** [Rule SM-3 — a stage may be entered late, but never skipped](../02-STAGE-MODEL.md#53-the-back-fill-rule)

**Retroactive stage verdict: `CLOSED-WITH-CONDITIONS`** — see §7.

---

## 1. What the stage requires

Six exit criteria ([S00 §5](../stages/S00-ideation.md#5-exit-gate--gate-s00)):

| # | Criterion | Level | Approvers |
|---|---|---|---|
| S00-G1 | Business case approved and funded | E2 | Sponsor (AP) · Rajal (AP) · Shailja (AP) · Kalpana (RV) · Mahesh (RV) |
| S00-G2 | Executive sponsor named with decision rights | E1 | as above |
| S00-G3 | Regulatory viability confirmed | E2 | as above |
| S00-G4 | Target outcomes defined and measurable | E1 | as above |
| S00-G5 | Alternatives considered and recorded | E1 | as above |
| S00-G6 | Programme governance operating | E1 | as above |

---

## 2. What already exists — inventory before adding anything

| Artefact | Path | Serves | Assessment |
|---|---|---|---|
| Project charter | [`00-project-charter.md`](../../au-bank-insurance-platform/00-project-charter.md) | S00-E01, S00-G6 | 🟢 Present, bounded, names the mandate |
| Product vision & target outcomes | [`02-product-vision-and-outcomes.md`](../../au-bank-insurance-platform/02-product-vision-and-outcomes.md) | S00-E01-S03, S00-G4 | 🟢 Present |
| Business problem statement (V2.0) | [`business-problem-statement.md`](../../context/business-problem-statement.md) | S00-E01-S01, S00-E02 | 🟢 Unusually strong; licence CA0515 cited, as-is loss named precisely |
| Stakeholder working session | [`01-stakeholder-working-session.md`](../../au-bank-insurance-platform/01-stakeholder-working-session.md) | S00-VT-01 | 🟢 Present |
| Working decisions & clarifications | [`07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md`](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) | S00-E04, decision rights | 🟡 §17 confirms the sponsor *role*, not the person |
| Decision log | [`DECISION-LOG.md`](../../au-bank-insurance-platform/DECISION-LOG.md) | S00-E04-S02 | 🟢 D-001…D-012 |
| Management readiness assessment | [`04-MANAGEMENT-READINESS.md`](../../au-bank-insurance-platform/po-drive/04-MANAGEMENT-READINESS.md) | S00-G6 | 🟢 Present |
| Executive sponsor lens (Dilip) | [`executive-sponsor-perspective/`](../../context/roles/principal-insurance-platform-product-owner/executive-sponsor-perspective/README.md) | Sponsor *reasoning*, not sponsor *authority* | 🟢 Present, and explicitly not a person |
| Investment & metrics model | [`02-investment-outcome-and-metrics-model.md`](../../context/roles/principal-insurance-platform-product-owner/executive-sponsor-perspective/02-investment-outcome-and-metrics-model.md) | The *method* for a business case | 🟢 Present — the method existed; the case did not |

**The finding this inventory produces:** the repository holds an excellent business case *method*
and no business case. That is the gap §4 closes.

---

## 3. What was missing

| Gap | Criterion it fails | Severity |
|---|---|---|
| **No cost model, benefit model, payback or funding envelope anywhere in the repository** | S00-G1 | Blocks the gate outright |
| **GAP-010 — executive sponsor unnamed.** Role confirmed ([WD §17](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md#17-executive-sponsorship)); individual is not | S00-G2 | Blocks the gate outright |
| No explicit options paper (build vs buy vs partner vs extend legacy) | S00-G5 | Blocks |
| GAP-021 — KPI dictionary incomplete; only "Policy Sold" is locked | S00-G4 | Partial |
| No funding line for the Foundation Recovery Increment | S00-G1 | Blocks the recovery, not just the gate |
| Steering terms of reference and first minuted meeting | S00-G6 | Partial |

**Consequence, stated in [S00 §6](../stages/S00-ideation.md#6-current-position-in-this-repository---partial):**
every decision that should escalate to a sponsor currently escalates to nobody. That is a
contributing cause of GAP-006 and GAP-007 — P0 gaps labelled *build freeze* — staying open while
building continued.

---

## 4. New evidence added by this document

### 4.1 The business case

Built to the method in the [investment and metrics model](../../context/roles/principal-insurance-platform-product-owner/executive-sponsor-perspective/02-investment-outcome-and-metrics-model.md).
Every value carries its epistemic state per §10 of that model:

| State | Meaning |
|---|---|
| **KNOWN** | Direct measured evidence in this repository |
| **ESTIMATE** | Range + method + confidence |
| **ASSUMPTION** | Owner + validation date |
| **UNKNOWN** | Measurement required before a named decision point |

> **No number below is invented.** Where AU SFB's actual customer counts, attach rates, commission
> rates, salary costs and cloud spend are required, they are marked **UNKNOWN** with the owner who
> holds them and the decision point by which they are needed. A business case that fabricates a
> payback period to look complete is worse than one that shows exactly which four numbers are
> missing — the second can be finished in a meeting.

#### Problem

AU SFB holds IRDAI Composite Corporate Agent licence CA0515 and distributes life, health and
general insurance through the AU Beema Portal. At the point of product selection the customer is
redirected to the insurer's portal and **the bank loses all visibility**: drop-off, underwriting
rejection, payment status and issuance are unknown to AU SFB.
[KNOWN — [business-problem-statement §3.1](../../context/business-problem-statement.md)]

#### Consequence of no action

| Consequence | State |
|---|---|
| Conversion cannot be measured, therefore cannot be improved | KNOWN — structural |
| Commission expected vs received cannot be reconciled at journey level | KNOWN — structural |
| Suitability and consent evidence sits with the insurer, not the bank; the bank is the licensed corporate agent and carries the obligation | KNOWN — regulatory |
| Post-redirect drop-off volume | **UNKNOWN** — owner: Bancassurance MIS. Needed by: pilot design |
| Annual premium and commission currently flowing through the redirect model | **UNKNOWN** — owner: Bancassurance Finance. Needed by: S00-G1 sign-off |

#### Investment classification

Primary: **Strategic foundation**. Secondary: **Revenue generating**, **Risk/control reducing**.

This classification is load-bearing. Classified as revenue-generating alone, the Foundation
Recovery Increment reads as pure cost and loses every prioritisation argument — which is
[mechanism 2](../01-POSITION-ASSESSMENT.md#7-how-this-happened--so-it-does-not-recur) of how the
foundation went missing in the first place.

#### Cost model

**One-time — effort, which this repository can evidence.** Currency conversion requires blended
rates the repository does not hold.

| Bucket | Effort | State | Basis |
|---|---:|---|---|
| Foundation Recovery Increment — S08 + S09 | **8–10 weeks elapsed** | ESTIMATE (medium confidence) | [Realignment plan §2 Move 3](../03-REALIGNMENT-PLAN.md#move-3--underpin-execute-s08-and-s09--810-weeks), sequenced week by week |
| Product/Compliance work in parallel (rule packs, AC, traceability, R0 matrix, S05 slice) | 6–8 weeks elapsed, non-competing capacity | ESTIMATE | This document and its five siblings are a substantial part of it |
| S10 integration (CBS, PG, AD, notifications) | **UNKNOWN** | UNKNOWN — owner: Mahesh. Needed by: R0 date commitment |
| S11 vertical slice through ~6 thin contexts + Flutter RM app | **UNKNOWN** | UNKNOWN — owner: Kalpana + Amit. Needed by: R0 date commitment |
| S12 hardening and certification | **UNKNOWN** | UNKNOWN — owner: Swapnali + Deepali |
| Retrofit penalty: adding tests and CI to ~20,200 lines written without them | Material, unquantified | ESTIMATE | 20,200 lines and test ratios 16–29% are KNOWN ([position assessment §3.1](../01-POSITION-ASSESSMENT.md#31-code-inventory-measured-2026-08-15)) |
| Team size and blended day rate | — | **UNKNOWN** — owner: Kalpana. **This is the single number that converts every row above into currency** |

**Recurring.** AWS ap-south-1 with ap-south-2 DR, EKS, Aurora Multi-AZ, DynamoDB, S3 with 7-year
Object Lock, observability and security tooling, 1SB transaction fees, operations and support.
Every line: **UNKNOWN** — owner: Shivanshi (cloud), Bancassurance (1SB commercials).
Needed by: **S09 gate**, because an environment design whose run cost nobody has priced is a
commitment made without a decision.

**Hidden / avoided.** Manual reconciliation effort · repeated RM follow-up on invisible cases ·
revenue and commission leakage · remediation of the quality and compliance debt catalogued in
[GAP-A through GAP-E](../01-POSITION-ASSESSMENT.md#4-the-five-structural-gaps) · the cost of
discovering a data-residency breach at audit rather than at design. All KNOWN as categories;
**UNKNOWN** as amounts.

#### Options considered — closes S00-G5

| # | Option | Coverage | Time to value | Strategic control | Reversibility | Why not / why |
|---|---|---|---|---|---|---|
| 1 | **Do nothing** — keep the AU Beema redirect model | Sale completes; bank sees nothing after redirect | Immediate | **None** post-redirect | n/a | Rejected: it is the problem statement. The bank carries the corporate-agent obligation while holding none of the evidence |
| 2 | **Process-only change** — tighten RM process, manual MIS on top of the portal | Partial visibility, lagging and manual | 4–8 weeks | Low | High | Rejected as a destination; **retained as an interim measure**, since it can start now and is the only thing that produces a real conversion baseline before R0 |
| 3 | **Extend the legacy portal** | Depends on portal ownership and extensibility | UNKNOWN | Low — the bank does not own the sale | Medium | Rejected: the visibility loss is architectural to the redirect model, not a missing feature of the portal |
| 4 | **Buy / white-label an aggregator platform** | High feature coverage quickly | Fastest to a working journey | **Low** — the customer relationship and journey definition sit with the vendor | Low — high switching cost | Rejected: the value proposition is *bank-owned* distribution integrated with bank identity, CBS, PG and compliance. Buying re-creates the redirect problem behind a nicer skin |
| 5 | **Build the bank-owned platform** — the current direction | Full, end to end, bank-controlled | Slowest | **Highest** | Medium | **Recommended.** The only option that satisfies the actual objective |
| 6 | **Hybrid: build the journey, aggregate the connectivity** — bank-owned platform with 1SB as a replaceable adapter | Full, with insurer connectivity accelerated | Faster than pure build | High, provided the adapter stays replaceable | Medium-high | **This is what is actually being built**, and it is the right choice. [WD §18](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md#18-1silverbullet-positioning) and [D-004](../../au-bank-insurance-platform/DECISION-LOG.md) already require replaceability, and the ports-and-adapters implementation honours it |

> **Recorded retrospectively and honestly.** Options 1–6 were not formally evaluated at S00 — this
> table reconstructs the reasoning from the decisions actually taken ([D-003, D-004](../../au-bank-insurance-platform/DECISION-LOG.md),
> [WD §18](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md#18-1silverbullet-positioning)).
> It is an E1 artefact recording a defensible rationale, not evidence that a comparison happened.
> I am not going to represent it as the latter.

#### Expected benefit — kept in four distinct classes

| Class | Benefit | Measurement | State |
|---|---|---|---|
| Financial | Commission income on policies the bank currently loses to post-redirect drop-off | Issued-policy count and premium, by insurer and channel | UNKNOWN baseline |
| Financial | Reduction in unreconciled and aged commission exceptions | Reconciliation exception count and ageing | UNKNOWN baseline |
| Customer | Fewer abandoned journeys through a single, resumable, pre-filled flow | Funnel completion by stage | UNKNOWN baseline |
| Operational | Every open case has a known next action, replacing manual chase | % of open journeys with a known next action; time-to-intervention | UNKNOWN baseline |
| Risk / control | The bank holds its own consent and suitability evidence for a regulated activity it is licensed for | Evidence retrieval drill within SLA ([CNS-R35](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#8-retention-and-retrieval)) | **Achievable and measurable at R0** |
| Risk / control | Change control on a regulated financial application becomes evidenceable to an auditor | CI run history; scan reports | **Achievable and measurable at S08** |
| Strategic | Aggregator replaceability preserved; no lock-in to 1SB | Adapter boundary held; ArchUnit enforced | KNOWN — already true |

**The two risk/control benefits are the honest headline for the Foundation Recovery Increment.**
They are deliverable, measurable, and do not depend on a single unknown financial input. The
revenue case is real but cannot be quantified until the funnel baseline exists — which is itself
one of the things R0 delivers.

#### KPI model — closes S00-G4 structurally, GAP-021 partially

Each KPI carries baseline, desired movement, source, owner, cadence and decision threshold, per
[§7 of the metrics model](../../context/roles/principal-insurance-platform-product-owner/executive-sponsor-perspective/02-investment-outcome-and-metrics-model.md#7-kpi-design-rule).

| ID | KPI | Baseline | Movement | Source | Owner | Cadence | Decision threshold |
|---|---|---|---|---|---|---|---|
| KPI-01 | **Policies Sold** — issued + confirmed + reconciled + persisted, all four ([D-007](../../au-bank-insurance-platform/DECISION-LOG.md)) | UNKNOWN | Increase | Policy service (#13) + reconciliation | Rajal | Weekly in pilot | Zero at end of pilot window → R0 has not proven the business case |
| KPI-02 | Stage-to-stage funnel conversion: lead → consent → suitability → quote → selected → proposal → UW → payment → issued | UNKNOWN | Increase | Journey orchestration (#9) | Rajal | Weekly | Any stage below 20% conversion → investigate that stage before scaling |
| KPI-03 | **Suitability bypass rate** | **0 by construction** | Maintain at 0 | Quote API audit events ([SUIT-R28](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#62-test-matrix--all-seven-cases-are-mandatory-tests)) | Shailja + Rajal | Continuous | **Any non-zero value halts the pilot.** Non-negotiable |
| KPI-04 | **Payments executed on an RM device** | **0 by construction** | Maintain at 0 | Payment service (#12) device attestation | Deepali + Rajal | Continuous | **Any non-zero value halts the pilot** |
| KPI-05 | Consent + suitability evidence completeness on submitted proposals | Target 100% | Maintain | Audit store (#16) | Shailja | Weekly | < 100% → stop submissions until the cause is fixed |
| KPI-06 | Time to issue: proposal submitted → policy issued | UNKNOWN | Decrease | Proposal (#11) + Policy (#13) | Rajal | Weekly | Outlier insurer > 2× panel median → partner conversation |
| KPI-07 | RM adoption: % of pilot RMs completing ≥ 1 journey per week | UNKNOWN | Increase | RM workspace (#2) | Rajal + Bancassurance | Weekly | < 50% → usability problem, not a training problem |
| KPI-08 | Reconciliation completeness: issued policies matched to a PG settlement | Target 100% | Maintain | Payment (#12) + Finance | Finance + Rajal | Weekly | Any unmatched issuance is an exception with a named owner |
| KPI-09 | **Gate criteria closed per week** (programme health) | Measured 0/7 and 0/6 at CR-009 | Increase | [GATE-EVIDENCE.yaml](../../governance/state/GATE-EVIDENCE.yaml) | Kalpana | Weekly | [Rule GM-1](../../governance/18-GOVERNANCE_METRICS.md) self-alarm |
| KPI-10 | Application CI green-run rate on `main` | **0 — no application CI at baseline** | Increase to > 95% | GitHub Actions | Amit + Swapnali | Weekly | < 95% sustained → the pipeline is being worked around |

KPI-03, KPI-04 and KPI-05 are **control KPIs**: their target is a constant, and any deviation is an
incident rather than a trend. KPI-09 and KPI-10 exist because this programme's specific failure
mode was rigour applied to documents and not to software.

#### Payback

**Cannot be computed today, and I will not manufacture it.** Payback requires: (a) blended cost per
delivery week, (b) the current premium and commission baseline, (c) an attach-rate assumption
validated against real AU SFB data. All three are **UNKNOWN**, owned by Kalpana (a) and
Bancassurance Finance (b, c).

**Decision-quality position:** per [§10 of the metrics model](../../context/roles/principal-insurance-platform-product-owner/executive-sponsor-perspective/02-investment-outcome-and-metrics-model.md#10-decision-discipline-under-uncertainty),
a reversible, bounded investment may proceed on explicit assumptions where a large irreversible
commitment may not. The Foundation Recovery Increment is bounded (8–10 weeks), reversible in the
sense that CI, IaC and test infrastructure retain value under *every* option including a later
pivot to buy, and is partly **regulatory mandatory** — for which financial ROI is explicitly not
the sole decision test. **The recovery increment can be funded before the payback model exists.
R0's full delivery commitment cannot.**

#### Funding line — Foundation Recovery Increment

| Field | Value |
|---|---|
| Line ID | `FRI-001` |
| Title | Foundation Recovery Increment — S08 Engineering Foundation + S09 Platform Foundation |
| Scope | [Realignment plan §2 Move 3](../03-REALIGNMENT-PLAN.md#move-3--underpin-execute-s08-and-s09--810-weeks), items 1–15 |
| Duration | 8–10 weeks (ESTIMATE) |
| Investment class | Strategic foundation + Regulatory mandatory |
| Requested by | Rajal (Product) + Kalpana (Delivery) |
| Approval authority | Executive Sponsor — **unnamed, see §5** |
| Tranche condition | Weeks 1–3 (CI + quality gates) funded on approval. Weeks 4–10 (test infrastructure, security scanning, IaC, environments) released on GATE-S08 criteria G1, G2 and G5 reaching `MET` |
| Stop condition | If GATE-S08 G1 is not `MET` by end of week 4, the increment is escalated to the sponsor rather than extended silently |
| What it buys | The ability to *evidence* anything at all. Every E3/E4 gate criterion in the programme depends on machinery this line installs |
| What it does not buy | Any bounded context, any feature, any LOB. [S08 §7](../stages/S08-engineering-foundation.md#7-premature-at-this-stage) |

> **Why the funding line is stated in tranches.** An open-ended foundation budget is how foundation
> work becomes indefinite, and a single-tranche budget is how it gets cut at week 5. Two tranches
> with a named release condition make the decision reviewable at the point where evidence exists.

### 4.2 Decision rights and escalation — closes S00-E04-S02 structurally

| Decision class | Accountable | Escalates to | Basis |
|---|---|---|---|
| R0 product scope, priority, acceptance | **Rajal** | Executive Sponsor | [Authority §2](../../context/roles/principal-insurance-platform-product-owner/03-authority-and-decision-rights.md#2-product-authority) |
| Material change to R0 scope, or a `Should` deferral with business consequence | Rajal, **with** the [Dilip sponsor lens](../../context/roles/principal-insurance-platform-product-owner/executive-sponsor-perspective/03-invocation-and-decision-contract.md) invoked first | Executive Sponsor | [R0-SCOPE §7](../../au-bank-insurance-platform/requirements/R0-SCOPE.md#7-executive-sponsor-checkpoint) |
| Service topology, technology selection | Mahesh | Repository owner / CTO authority | Authority §3 |
| Regulatory permissibility | **Shailja** — binding | Legal / Compliance human authority | [11-REVIEW_GATES §9](../../governance/11-REVIEW_GATES.md) |
| Security control adequacy and exceptions | **Deepali** — binding veto | Named human tie-breaker (Rule PA-2) | 11-REVIEW_GATES §7 |
| Test sufficiency | Swapnali | Kalpana → Sponsor | Gate model §5 |
| Operational readiness | Shivanshi | Kalpana → Sponsor | Gate model §5 |
| Physical data design, backup/DR | Aarti | Mahesh | Gate model §5 |
| Sequencing, critical path, capacity | **Kalpana** | Executive Sponsor | [DCS](../../governance/DELIVERY-CONTROL-SYSTEM.md) |
| **Material residual risk acceptance** | **A named human — never a persona, never an agent** | — | [Gate model §8](../04-GATE-AND-SIGNOFF-MODEL.md#8-waivers-and-exceptions) |
| Funding envelope, tranche release, stop decision | **Executive Sponsor** | Investment authority | This document §4.1 |
| Stage `PASSED` | **Mahesh + Rajal jointly** | — | [Gate model §4](../04-GATE-AND-SIGNOFF-MODEL.md#4-transition-procedure) |

**Escalation path:** persona owner → Kalpana (R12, may force the *timing* of a decision but never
its content, Rule PA-1) → Executive Sponsor → investment authority. A binding Security or
Compliance veto does not travel this path; it is resolved under Rule PA-2 by a named human
tie-breaker.

### 4.3 Steering cadence — proposed, closes S00-E04-S03 structurally

| Field | Proposal |
|---|---|
| Frequency | Fortnightly during the Foundation Recovery Increment; weekly from S11 entry |
| Membership | Executive Sponsor (chair) · Rajal · Kalpana · Mahesh · Shailja · Bancassurance Head · Finance representative |
| Quorum | Sponsor + Rajal + Kalpana, with Shailja mandatory for any agenda item touching a control |
| Decides | Tranche release · scope changes above PO2 · stop/continue on KPI decision thresholds · escalations under §4.2 |
| Does not decide | Anything reserved to a binding domain authority. Steering may not overrule Deepali or Shailja |
| Standing agenda | KPI-09 gate closure rate · KPI-10 CI green rate · open P0 gaps · blocked criteria with named blockers · risk register deltas |

**This is a proposal, not a record.** S00-G6 requires a first meeting minuted. No such meeting has
occurred, and I am not going to write minutes for one.

---

## 5. GAP-010 — what I can close, and what I cannot

**GAP-010 requires a named individual.** [WD §17](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md#17-executive-sponsorship)
confirms the **role** — Head of Insurance Business / Insurance Platform at AU Bank — and records
the individual as *to be confirmed*.

| What I close | What I do not close |
|---|---|
| The sponsor **role definition**, its decision rights, and its escalation path (§4.2) | The **name** |
| The decision classes that must reach the sponsor, and the trigger for each (§4.2) | |
| The steering structure the sponsor chairs (§4.3) | The first minuted meeting |
| The sponsor **reasoning lens** — [Dilip](../../context/roles/principal-insurance-platform-product-owner/executive-sponsor-perspective/README.md) — for rehearsing an investment challenge before a real decision | Any suggestion that the lens *is* a sponsor |
| The funding line and its tranche conditions, ready for a sponsor to approve (§4.1) | The **approval** of that funding line |

> **The Dilip lens is an AI perspective inside my package. It is not a person, it holds no budget,
> and it cannot approve `FRI-001`.** The identity rule in
> [R0-SCOPE §8](../../au-bank-insurance-platform/requirements/R0-SCOPE.md#8-sign-off-and-perspective-record)
> is explicit: an output produced by the lens must say *AI Executive Sponsor Perspective* and must
> never be represented as Dilip Kumar Vidyarthi personally approving anything. Writing a name into
> GAP-010's exit criterion to make a gate green would be exactly the fabrication this framework
> exists to prevent, and it would be the most damaging thing in this entire evidence set.

**GAP-010 remains OPEN.** Owner: Rajal, to obtain the name from AU Bank Bancassurance leadership.
Target: **2026-08-29**. Escalation if unmet: the funding line has no approver, so `FRI-001` cannot
be released and the recovery does not start — which is precisely the consequence that should force
the naming.

---

## 6. What remains genuinely open

| ID | Item | Criterion | Owner | Target | Evidence needed |
|---|---|---|---|---|---|
| **GAP-010** | Executive sponsor **named** | S00-G2 | Rajal → Bancassurance leadership | 2026-08-29 | RACI naming an individual (E1) |
| **S00-OPEN-01** | `FRI-001` funding line **approved** with envelope and tranche conditions | S00-G1 | Executive Sponsor (once named) | 2026-09-12 | Signed investment approval (E2) |
| **S00-OPEN-02** | Blended cost per delivery week; team size | S00-G1 | Kalpana | 2026-08-29 | Delivery capacity statement |
| **S00-OPEN-03** | Current premium, commission and post-redirect drop-off baseline | S00-G1, KPI-01/02 | Bancassurance Finance + MIS | 2026-09-12 | MIS extract |
| **S00-OPEN-04** | Recurring cloud and 1SB transaction run-cost model | S00-G1 | Shivanshi + Bancassurance | **S09 gate** | Costed environment design |
| **S00-OPEN-05** | Compliance opinion on licence viability, signed | S00-G3 | Shailja | 2026-09-12 | Written opinion (E2). The *substance* is in the business problem statement; the *signature* is not |
| **S00-OPEN-06** | Steering terms of reference published; first meeting minuted | S00-G6 | Rajal + Kalpana | 2026-09-12 | ToR + minutes (E1) |
| **S00-OPEN-07** | S00-VT-01 stakeholder validation (≥ 4 of 5+ independently describe the post-redirect loss) | S00-G4 | Rajal | 2026-09-26 | Interview record |
| **GAP-021** | KPI dictionary completed beyond the ten in §4.1 | S00-G4 | BI + Rajal | R0 pilot start | KPI dictionary |

**S00-OPEN-02 and S00-OPEN-03 together are the whole payback model.** Two numbers from two people.
Neither is a research task.

---

## 7. Retroactive stage verdict

> ## `CLOSED-WITH-CONDITIONS`

| Criterion | State | Basis |
|---|---|---|
| S00-G1 Business case approved and funded | **NOT MET** | The case now exists (§4.1) at E1. Approval and funding are human acts that have not occurred. S00-OPEN-01 |
| S00-G2 Executive sponsor named | **NOT MET** | GAP-010. Role, rights and escalation closed; the name is not mine to write |
| S00-G3 Regulatory viability confirmed | **PARTIAL** | Substance present and strong ([business problem statement §1](../../context/business-problem-statement.md#1-executive-summary--regulatory-framework)); signed Compliance opinion outstanding. S00-OPEN-05 |
| S00-G4 Target outcomes defined and measurable | **MET at E1** | Ten KPIs with baseline, movement, source, owner, cadence and decision threshold (§4.1). Six baselines are UNKNOWN and marked as such |
| S00-G5 Alternatives considered and recorded | **MET at E1, with a caveat** | Six options recorded (§4.1). Recorded retrospectively; not evidence that a comparison occurred at the time |
| S00-G6 Programme governance operating | **PARTIAL** | Decision rights and cadence defined (§4.2, §4.3); ToR unpublished, no minuted meeting. S00-OPEN-06 |

**Why `CLOSED-WITH-CONDITIONS` and not `OPEN`:** every artefact within Product's authority to
produce now exists and is specific enough to act on. The three unmet criteria fail on **human acts
outside my authority** — naming a person, approving money, and signing a Compliance opinion — not
on missing analysis. Marking this `OPEN` would imply Product has work left to do here; it does not.

**Why not `CLOSED`:** S00-G1 and S00-G2 are unmet, and a stage with two unmet criteria is not
closed. The [gate model's evidence rule](../04-GATE-AND-SIGNOFF-MODEL.md#2-evidence-the-rule-the-whole-model-rests-on)
does not bend for a stage that is inconvenient to leave open.

**Conditions carried forward:** GAP-010, S00-OPEN-01 through S00-OPEN-07 (§6). Under
[Rule GS-4](../04-GATE-AND-SIGNOFF-MODEL.md#6-sign-off-record-format), these are acceptance
criteria, tracked to closure — not caveats.

**Signature status:** `AI-DRAFTED — mandatory human signature outstanding`.
S00-G1 and S00-G3 require E2 human signatures (Sponsor, Shailja). No AI output satisfies either.
Silence does not approve this stage.

— **Rajal**, Principal Insurance Platform Product Owner, 2026-08-16
