# S04 — Product Definition & Release Slicing · Retroactive Stage Evidence

**Stage definition:** [`stages/S04-product-definition.md`](../stages/S04-product-definition.md)
**Workstream:** WS-3 ([charter](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md))
**Author:** Rajal — Principal Insurance Platform Product Owner (Board 3 / R1)
**Date:** 2026-08-16
**Closes:** GAP-012 (quote validity and compare rules) · GAP-013 (product matrix dimensions)

**Retroactive stage verdict: `CLOSED-WITH-CONDITIONS`** — see §8.

> **The critical output of this stage is not the in-scope list.** It is the **out-of-scope list with
> revisit triggers**, because that list is what AIGEM's scope-fit triage consults for the rest of
> the programme's life ([S04 §1](../stages/S04-product-definition.md#1-purpose)). That list now
> lives in [WS-3 charter §3](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md#3-scope) and is
> not duplicated here.

---

## 1. What the stage requires

| # | Criterion | Level |
|---|---|---|
| S04-G1 | R0 scope defined: in, out with triggers, and never | E2 |
| S04-G2 | PRD approved for R0 | E2 |
| S04-G3 | Backlog decomposed, prioritised, sized, dependency-mapped | E1 |
| S04-G4 | DoR and DoD agreed and published | E2 |
| S04-G5 | R0 delivers standalone business value | E2 |
| S04-G6 | KPIs defined with measurement mechanisms | E1 |
| S04-G7 | Technical enablers visible in the backlog and scheduled | E1 |

**Approvers:** Rajal (AP) · **Kalpana (AP, feasibility)** · Mahesh (RV) · Swapnali (RV) ·
Deepali (RV) · Shailja (RV) · Shivanshi (RV)

---

## 2. What already exists

| Criterion | Artefact | Path | Assessment |
|---|---|---|---|
| S04-G1 | R0 scope one-pager with in / out / metrics / dependencies | [`requirements/R0-SCOPE.md`](../../au-bank-insurance-platform/requirements/R0-SCOPE.md) | 🟢 Structure exactly right · 🟡 v0.3 carries three journeys Day 1; superseded by assisted-first (§3) |
| S04-G2 | PRD for R0 | [`PRD-R0-DISTRIBUTION-PLATFORM.md`](../../au-bank-insurance-platform/requirements/PRD-R0-DISTRIBUTION-PLATFORM.md) | 🟢 |
| S04-G1 | Locked working decisions A1–A12 / D-001–D-012 | [`07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md`](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) · [`DECISION-LOG.md`](../../au-bank-insurance-platform/DECISION-LOG.md) | 🟢 Segment, LOB, channel, sold definition, insurer strategy, payment rule all locked |
| S04-G3 | Programme TODO master list with waves | [`po-drive/03-PROGRAMME-TODO.md`](../../au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md) | 🟡 A Product checklist, not a sized, dependency-mapped backlog |
| S04-G3 | Generated lifecycle backlog + Jira import | [`backlog/BACKLOG.yaml`](../backlog/README.md) | 🟢 Substantial, generated from the stage model, drift-checked in CI |
| S04-G4 | Definition of Ready / Done | [`12-DEFINITION_OF_READY.md`](../../governance/12-DEFINITION_OF_READY.md) · [`13-DEFINITION_OF_DONE.md`](../../governance/13-DEFINITION_OF_DONE.md) | 🟢 |
| S04-G6 | R0 success metrics | [`R0-SCOPE.md §5`](../../au-bank-insurance-platform/requirements/R0-SCOPE.md#5-success-metrics-mvp) | 🟡 Metrics named; source system and owner absent |
| S04-G6 | KPI model with baseline / movement / source / owner / cadence / threshold | [S00 evidence §4.1](./S00-ideation-evidence.md#41-the-business-case) | 🟢 **New** — ten KPIs |
| S04-G5 | Sponsor checkpoint contract | [`R0-SCOPE.md §7`](../../au-bank-insurance-platform/requirements/R0-SCOPE.md#7-executive-sponsor-checkpoint) | 🟢 |

---

## 3. What was missing

| # | Defect | Criterion |
|---|---|---|
| D1 | **GAP-013 — product matrix dimensions undefined.** R0's catalogue content is not specified, so `BR-PROD-010` ("matrix returns eligible products") has nothing to return | S04-G1, S04-G2 |
| D2 | **GAP-012 — quote validity and compare rules missing.** `BR-COMP-010` says an expired quote cannot proceed; nothing defines expiry | S04-G1 |
| D3 | No product-**routing** rules: which journey a given insurer/product combination follows (in-platform vs redirect) | S04-G1 |
| D4 | **S04-VT-06 fails.** The backlog contains **no CI, IaC, environment or observability enablers**. Foundation was treated as overhead and therefore never scheduled | **S04-G7** |
| D5 | KPI measurement mechanism unspecified: which system emits each metric, and which story adds the instrumentation | S04-G6 |
| D6 | R0 channel scope inconsistent between `R0-SCOPE.md` v0.3 (three journeys Day 1) and the current Product direction (assisted-first) | S04-G1 |

> **D4 is the S04 defect that produced the S08 and S09 holes.** [S04-VT-06](../stages/S04-product-definition.md#4-validation-tests)
> exists *because of this programme's history*: foundation work that lives outside the backlog as
> assumed overhead is foundation work that never gets scheduled.

---

## 4. New evidence

### 4.1 R0 channel scope — resolving D6

**Product decision:** R0 is **assisted-first**. One channel, one product class, one insurer must
reach a demonstrable sale before DIY is added; hybrid follows only after assisted and DIY both have
stable state, identity and hand-off contracts.

| Channel | R0 | Revisit |
|---|---|---|
| RM-assisted | ✅ | — |
| Customer self-service (DIY) | ⛔ | **R1** — after the assisted journey completes a real sale in pilot |
| Hybrid / mode switching | ⛔ | **R2** — after assisted and DIY both have stable hand-off contracts |

This **supersedes** [`R0-SCOPE.md` §2 A2](../../au-bank-insurance-platform/requirements/R0-SCOPE.md#2-working-decisions-locked-unless-overturned)
and D-002's Day-1 framing for R0 only. It **does not remove** DIY or hybrid from the product; it
sequences them behind a proven journey.

**Reopening basis** under [authority §8](../../context/roles/principal-insurance-platform-product-owner/03-authority-and-decision-rights.md#8-existing-decision-protection):
*scope/stage change* and *new material cost*. Three journeys × sixteen missing bounded contexts ×
an absent engineering foundation is not a deliverable R0, and the [position assessment](../01-POSITION-ASSESSMENT.md)
is the new evidence. Recorded as [DEC-20260816-03](../../governance/registers/DECISION-REGISTER.md).
The superseded decision is preserved, not overwritten.

### 4.2 The R0 product matrix — closes GAP-013

#### 4.2.1 Matrix dimensions

Eleven dimensions. Every row in the Product Catalogue (#8) carries all eleven.

| # | Dimension | Type | Example values | Drives |
|---|---|---|---|---|
| 1 | `lob` | Enum | `LIFE`, `HEALTH`, `GENERAL` | Journey selection |
| 2 | `productClass` | Enum | `TERM`, `SAVINGS_ENDOWMENT`, `ULIP`, `ANNUITY` | **Suitability outcome mapping** ([pack §4](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#4-product-class-outcomes)) |
| 3 | `insurerCode` | Enum | Group A / Group B panel | Routing, credentials |
| 4 | `insurerGroup` | Enum | `A` (1SB-integrated) · `B` (catalogue + redirect) | **Product routing** (§4.4) |
| 5 | `bankProductName` | String | Bank-facing name | RM and customer display |
| 6 | `insurerProductCode` | String | Insurer's identifier | Aggregator call |
| 7 | `variantCode` | String | Plan option / benefit variant | Quote request |
| 8 | `eligibility` | Structured | `minAge`, `maxAge`, `minSumAssured`, `maxSumAssured`, `minTermYears`, `maxTermYears`, `minAnnualIncome`, `occupationExclusions[]`, `smokerPermitted` | Catalogue filter, pre-quote |
| 9 | `salesStatus` | Enum | `ACTIVE`, `SUSPENDED`, `WITHDRAWN` | Whether it can be sold today |
| 10 | `supportedJourneys` | Array | `ASSISTED`, `DIY`, `HYBRID` | Channel filter |
| 11 | `aggregatorAvailability` | Enum | `QUOTE_AND_PROPOSAL`, `QUOTE_ONLY`, `NONE` | What the platform can actually do |

Plus effective dating on every row: `effectiveFrom`, `effectiveTo`.

**Extends** [WD §7](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md#7-product-catalogue),
which listed nine catalogue fields, by splitting eligibility into structured sub-fields (so the
filter is executable) and adding `productClass` (so the suitability outcome can bind to a product
without a lookup table nobody maintains).

#### 4.2.2 The R0 population rule

> **R0 contains exactly one `(insurerGroup=A, lob=LIFE, productClass=TERM)` slot, populated with
> one insurer and one product from the confirmed Group A panel.**

| Field | R0 value |
|---|---|
| `lob` | `LIFE` — [D-001](../../au-bank-insurance-platform/DECISION-LOG.md) |
| `productClass` | `TERM` only |
| `insurerGroup` | `A` only |
| `insurerCode` | **One insurer** from {ICICI Prudential, HDFC Life, Bajaj Allianz} — **UNKNOWN** pending §6 |
| `bankProductName` / `insurerProductCode` / `variantCode` | **UNKNOWN** pending §6 |
| `eligibility` | **UNKNOWN** — insurer-published; obtained during S10 |
| `salesStatus` | `ACTIVE` |
| `supportedJourneys` | `[ASSISTED]` |
| `aggregatorAvailability` | `QUOTE_AND_PROPOSAL` — R0 requires the full in-platform path |

**Why one insurer, not three.** R0's purpose is to prove one journey end to end. Three insurers
multiplies integration surface, defect surface and comparison UX without proving anything the first
does not. Multi-insurer comparison is genuinely valuable and is **R1** — and `AC-COMP-010-1`
already specifies its behaviour so the design does not have to change to add it.

> **The specific insurer, product and eligibility values are commercial facts I do not hold and
> will not invent.** They are UNKNOWN with a named owner (§6). What is closed by GAP-013 is the
> **matrix dimensions and the R0 population rule** — which is exactly the gap's exit criterion,
> *"Matrix v0 for Life catalogue"*. Fabricating a product name and an age band would produce a
> catalogue that looks complete and is wrong at the first insurer conversation.

#### 4.2.3 Catalogue behaviour rules

| ID | Rule | Test |
|---|---|---|
| `CAT-R01` | A product with `salesStatus ≠ ACTIVE` MUST NOT be returned by the eligible-products call, and MUST NOT be quotable even if requested directly. | Set `SUSPENDED`; the product disappears from the list and a direct quote returns `409 PRODUCT_NOT_SELLABLE`. |
| `CAT-R02` | Eligibility filtering MUST run **after** the suitability outcome filter, never instead of it. | A product the customer is eligible for but whose class is `NOT_SUITABLE` is not returned. |
| `CAT-R03` | A product whose `supportedJourneys` excludes the current channel MUST NOT be returned. | R0 is `ASSISTED`; a DIY-only product is absent. |
| `CAT-R04` | Effective dating MUST be respected on read: a row outside its effective window is invisible. | Set `effectiveFrom` in the future; product absent. |
| `CAT-R05` | R0 catalogue content MUST be loadable by config or admin seed; a full admin UI is out of R0. | Seed loads from configuration; no UI is required. |
| `CAT-R06` | Zero eligible products MUST produce an explicit empty state with the reason, and **no quote call**. | `AC-PROD-010-2`. |

### 4.3 Quote rules — closes GAP-012

| ID | Rule | Test |
|---|---|---|
| `QR-01` | An offer's validity is the **shorter of** the insurer's stated validity and **7 calendar days** from `quoteReceivedAt`. Where the insurer states none, 7 days applies. | Insurer states 3 days → `validUntil` = +3d. Insurer states 30 days → `validUntil` = +7d. Insurer states none → +7d. |
| `QR-02` | An offer past `validUntil` MUST NOT be selectable and MUST NOT proceed to proposal. | `409 QUOTE_EXPIRED` with a refresh path — `AC-COMP-010-3`. |
| `QR-03` | A quote MUST be re-requested, not extended. There is no renewal of an expired offer. | No API accepts a validity extension. |
| `QR-04` | **Re-quote triggers** — any of these invalidates all offers on the journey: change to sum assured, term, premium frequency, smoker status, or the customer's DOB correction; a new or superseded suitability evaluation; a `SUSPENDED`/`WITHDRAWN` catalogue transition on the quoted product. | Change sum assured; all offers move to `INVALIDATED` and a re-quote is required. |
| `QR-05` | **Comparison basis** — offers are comparable only when normalised to the same sum assured, term and premium frequency. Where a returned offer differs on any of the three, it is displayed with the difference called out explicitly and is **excluded from the ranked comparison**. | Return an offer at a different term; it renders in a "not directly comparable" group. |
| `QR-06` | **Ranking rule** — the default ordering is **ascending annual premium for the normalised basis**. The rule is disclosed on screen. | The displayed order matches ascending premium; the rule text is visible without interaction. |
| `QR-07` | Ranking MUST NOT be influenced by commission, insurer commercial arrangement, or any bank-internal preference. No such field is an input to ordering. | Inspect the ordering function's inputs: normalised premium and nothing else. |
| `QR-08` | Where offers tie on premium, the tie-break is ascending insurer response time, then insurer code alphabetically — deterministic, never random. | Two identical premiums produce a stable, reproducible order. |
| `QR-09` | An offer MUST display premium, sum assured, term, frequency and the key benefit set in the bank's canonical field names. Insurer-specific field names MUST NOT surface to the RM or customer. | `AC-QUOTE-020-3`. |
| `QR-10` | A `PARTIAL` result MUST present the offers received and name the insurers that failed, with a normalised reason. Partial failure MUST NOT suppress successful offers. | `AC-QUOTE-020-2`. |
| `QR-11` | Every quote request MUST carry a valid suitability evaluation ID. | [SUIT-R20](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#1-the-gate-stated-once-precisely). |
| `QR-12` | Quote requests MUST be idempotent on a caller-supplied `Idempotency-Key`; a replay returns the original `quoteJobId` and makes no second aggregator call. | `AC-QUOTE-010-2`. |

> **QR-07 is a Product decision with a compliance edge, and I am stating it deliberately.** A bank
> distributing multiple insurers under a corporate agency licence, ordering offers by anything
> other than a disclosed customer-relevant basis, is a mis-selling exposure. Ordering by commission
> is not a feature request I will entertain later; it is on the [never list](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md#33-never)
> in spirit and I am asking Shailja to confirm it belongs there in fact.

### 4.4 Product-routing rules — closes D3

Which journey a product follows. Derived from `insurerGroup` and `aggregatorAvailability`, never
hard-coded per insurer.

| ID | Rule | Journey |
|---|---|---|
| `PR-01` | `insurerGroup = A` ∧ `aggregatorAvailability = QUOTE_AND_PROPOSAL` → **full in-platform journey**: suitability → quote → compare → proposal → payment → issuance | R0 |
| `PR-02` | `insurerGroup = A` ∧ `aggregatorAvailability = QUOTE_ONLY` → quote in-platform, then **controlled redirect** for proposal onward, with the journey marked `REDIRECTED_AT_PROPOSAL` | R1 |
| `PR-03` | `insurerGroup = B` → catalogue and suitability in-platform, then **controlled redirect** after recommendation; journey marked `REDIRECTED_AT_RECOMMENDATION` | R1 |
| `PR-04` | Every redirect MUST record: journey state at redirect, target insurer, timestamp, actor, and the consent covering it (`CNS-RDR`). **The bank does not lose the record even where it loses the journey** | R1 |
| `PR-05` | A redirect in an assisted journey MUST deliver the link to the **customer's** device. The RM never completes a purchase on the insurer's site. | R1 — [WD §6](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md#6-redirection-strategy-non-integrated-insurers) |
| `PR-06` | Routing MUST be data-driven from the catalogue row. Adding an insurer MUST NOT require a code change. | Add a Group B row by configuration; PR-03 applies with no deployment. |

**Only PR-01 and PR-06 are exercised in R0.** PR-02 through PR-05 are specified now because
`AC-PROD-010-1` and the catalogue schema must accommodate them, and because
[PR-04 is the whole point of the platform](../../context/business-problem-statement.md#31-as-is-state-au-beema-portal-redirect-model):
the legacy model loses the record at redirect, and ours must not — even in the release where the
redirect itself is out of scope.

### 4.5 Technical enablers made visible — closes D4 (S04-VT-06)

Foundation work as **named backlog items with owners and gate criteria**, not overhead.

| Enabler | Type | Owner | Gate criterion | Funded under |
|---|---|---|---|---|
| Application CI: build + test every module on every PR | `INFRA` | Amit | S08-G1 | `FRI-001` T1 |
| Branch protection; no merge without green | `INFRA` | Amit + Shivanshi | S08-G2 | `FRI-001` T1 |
| JaCoCo thresholds enforced (build fails); QA-001 closed | `QA` | Swapnali | S08-G3 | `FRI-001` T1 |
| ArchUnit + static analysis enforced in CI | `QA` | Amit | S08-G4 | `FRI-001` T1 |
| Secret scanning, SAST, SCA, image scanning, SBOM | `SEC` | Deepali | S08-G5 | `FRI-001` T2 |
| Testcontainers (PostgreSQL) | `QA` | Swapnali | S08-G6 | `FRI-001` T2 |
| WireMock harness for 1SB (closes TD-014) | `QA` | Swapnali | S08-G6 | `FRI-001` T2 |
| Contract tests across the integration ↔ persistence seam | `QA` | Amit | S08-G6 | `FRI-001` T2 |
| E2E harness — delivers WS-1 criterion 4.1 properly | `QA` | Swapnali | S08-G6, **WS-1 4.1** | `FRI-001` T2 |
| PII-in-logs automated test | `SEC` | Deepali | S08-G7 | `FRI-001` T2 |
| Performance harness — unblocks WS-1 criterion 4.6 | `QA` | Amit | **WS-1 4.6** | `FRI-001` T2 |
| Terraform: VPC, subnets, SGs, KMS, ECR, EKS — **ap-south-1** | `INFRA` | Shivanshi | S09 | `FRI-001` T2 |
| Three environments — dev, UAT, prod — with promotion | `INFRA` | Shivanshi | S09 | `FRI-001` T2 |
| AWS Secrets Manager wired for real (closes TD-006) | `SEC` | Deepali + Shivanshi | S09 | `FRI-001` T2 |
| Observability: metrics, structured logs with correlation IDs, traces | `INFRA` | Shivanshi | S09 | `FRI-001` T2 |
| Deploy pipeline with **exercised** rollback | `INFRA` | Shivanshi | S09 | `FRI-001` T2 |
| **S3 Object Lock, 7-year, compliance mode** | `COMP` | Shivanshi + Aarti | S09, [CNS-R34](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#8-retention-and-retrieval) | `FRI-001` T2 |
| Flutter RM application — R0 journey screens | `FUNC` | Amit + Design | S11 | R0 delivery |
| Design system implementation from the [S05 token spec](./S05-experience-evidence.md#46-design-system-specification) | `FUNC` | Design + Amit | S11 | R0 delivery |

**Nineteen enablers.** S04-VT-06 asked for a non-zero, sized, scheduled count. The count was zero;
it is now nineteen, each with an owner, a gate criterion and a funding tranche. Sizing is
**Kalpana's**, and this table is her input.

### 4.6 KPI measurement mechanism — closes D5

The ten KPIs are in [S00 evidence §4.1](./S00-ideation-evidence.md#41-the-business-case). Each is
bound to an emitting system and an instrumentation story, per
[S04-E05-S02](../stages/S04-product-definition.md#3-epics-and-stories).

| KPI | Emitting system | Instrumentation story | Stage |
|---|---|---|---|
| KPI-01 Policies Sold | Policy (#13) + reconciliation | Emit `POLICY_SOLD` only when all four conditions hold (`AC-POL-010-2`) | S11 |
| KPI-02 Funnel conversion | Journey orchestration (#9) | Emit a stage-transition event on every `stageHistory` append | S11 |
| KPI-03 Suitability bypass rate | Quotation (#10) | Emit an audit event on every gate refusal ([SUIT-R28](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#62-test-matrix--all-seven-cases-are-mandatory-tests)) | S11 |
| KPI-04 RM-device payments | Payment (#12) | Emit `deliveryTarget` on every session; **any value other than `CUSTOMER_DEVICE` is an incident** | S11 |
| KPI-05 Evidence completeness | Audit (#16) | Assert consent + suitability references on every submitted proposal | S11 |
| KPI-06 Time to issue | Proposal (#11) + Policy (#13) | Timestamp both ends | S11 |
| KPI-07 RM adoption | RM workspace BFF (#2) | Per-RM journey-completion counter | S11 |
| KPI-08 Reconciliation completeness | Payment (#12) + Finance | Match issued policies to PG settlements | S11 |
| KPI-09 Gate criteria closed per week | [`GATE-EVIDENCE.yaml`](../../governance/state/GATE-EVIDENCE.yaml) | Already emitted by governance CI | **Now** |
| KPI-10 CI green-run rate | GitHub Actions | Available once S08-G1 is met | **S08** |

**KPI-09 and KPI-10 can be measured before a single business feature ships**, which matters: they
are the two that tell us whether the recovery is working.

---

## 5. What R0 does *not* contain, and why that is the important half

Full list: [WS-3 charter §3.2 and §3.3](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md#32-out-of-scope-with-revisit-triggers).
Sixteen out-of-scope items, each with a `revisit_at`; eleven permanent exclusions.

**Against [S04-VT-03](../stages/S04-product-definition.md#4-validation-tests)** — *zero items with
no revisit condition*: all sixteen carry one.

---

## 6. What remains genuinely open

| ID | Item | Criterion | Owner | Target |
|---|---|---|---|---|
| S04-OPEN-01 | **The R0 insurer, product and variant, named.** GAP-013's dimensions are closed; the content is a commercial fact | S04-G1, S04-G2 | Bancassurance + Rajal | 2026-09-12 |
| S04-OPEN-02 | R0 product eligibility values (age, sum assured, term, income bands) from the insurer | S04-G1 | Bancassurance + Mahesh (S10) | 2026-09-26 |
| S04-OPEN-03 | GAP-015 — 1SB Distributor ID, UAT keys, IP allowlist, confirmed panel | Dependency | Bancassurance | Tracked as DEP-002 |
| S04-OPEN-04 | Backlog **sized** and dependency-mapped; critical path drawn | S04-G3 | **Kalpana** | 2026-09-12 |
| S04-OPEN-05 | Pilot population: which branches, how many RMs, which customers, over what period | S04-E01-S03 | Rajal + Bancassurance | 2026-09-26 |
| S04-OPEN-06 | Launch and rollback criteria for R0 | S04-E01-S04 | Rajal + Kalpana | 2026-09-26 |
| S04-OPEN-07 | S04-VT-01 walkthrough: a real RM confirms a complete sale is possible with R0 only | S04-G5 (E2) | Rajal + Bancassurance | 2026-10-10 |
| S04-OPEN-08 | Shailja's confirmation that QR-07 (no commission-influenced ranking) is a compliance obligation, not only a Product preference | S04-G1 | Shailja | 2026-09-12 |
| S04-OPEN-09 | `R0-SCOPE.md` updated to v0.4 reflecting assisted-first (§4.1) | S04-G1 | Rajal | 2026-08-29 |
| GAP-021 | KPI dictionary beyond the ten | S04-G6 | BI + Rajal | R0 pilot start |

**S04-OPEN-09 is mine and it is near-term.** I have superseded a decision in this document;
leaving `R0-SCOPE.md` v0.3 asserting the old position creates exactly the two-sources-of-truth
problem [WD's own rules](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md#how-teams-must-use-this-document)
warn about. I did not edit it in this pass because it is a Product SSOT that deserves a versioned
revision, not an inline patch inside an evidence exercise.

---

## 7. A note on what I did not do

I did not rewrite `R0-SCOPE.md` or `PRD-R0-DISTRIBUTION-PLATFORM.md`. Both are good documents that
need a version bump, not replacement, and the realignment principle is explicit: *nothing already
built is deleted; everything already built is re-parented*
([realignment §1](../03-REALIGNMENT-PLAN.md#1-the-governing-decision-underpin-do-not-demolish)).
The same principle applies to documents.

---

## 8. Retroactive stage verdict

> ## `CLOSED-WITH-CONDITIONS`

| Criterion | State | Basis |
|---|---|---|
| S04-G1 R0 scope: in / out with triggers / never | **MET at E1** | [WS-3 §3](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md#3-scope): 17 in, 16 out with triggers, 11 never. Channel scope resolved (§4.1). E2 signature outstanding |
| S04-G2 PRD approved for R0 | **PARTIAL** | PRD exists; needs a v0.4 aligning to assisted-first and the R0 matrix. S04-OPEN-09 |
| S04-G3 Backlog decomposed, prioritised, **sized**, dependency-mapped | **PARTIAL** | Decomposition and priority exist; **sizing and the critical path are Kalpana's** and outstanding. S04-OPEN-04 |
| S04-G4 DoR and DoD agreed and published | **MET** | AIGEM 12 and 13 |
| S04-G5 R0 delivers standalone business value | **NOT MET as evidence** | The slice is designed to (§4.1–4.4). S04-G5 requires an E2 walkthrough record with a real RM. S04-OPEN-07 |
| S04-G6 KPIs with measurement mechanisms | **MET at E1** | Ten KPIs (S00 §4.1) each bound to an emitting system and an instrumentation story (§4.6) |
| **S04-G7 Technical enablers visible and scheduled** | **MET at E1** | Nineteen enablers with owners, gate criteria and funding tranches (§4.5). **S04-VT-06 moves from FAIL to PASS**, pending Kalpana's sizing |

**Gaps closed on content:** GAP-012 (12 quote rules), GAP-013 (11 matrix dimensions + R0 population
rule + 6 catalogue rules). Product routing rules PR-01…PR-06 close D3, which was not registered as
a gap and should have been.

**The most consequential change in this stage is §4.5.** S04-VT-06 was failing, and its failure is
the direct mechanism by which S08 and S09 went missing: foundation treated as overhead is
foundation never scheduled. Nineteen named, owned, gate-bound, tranche-funded enablers is the
countermeasure, and it only holds if Kalpana sizes them into a real plan.

**Conditions carried forward:** S04-OPEN-01 through -09; GAP-021.

**Signature status:** `AI-DRAFTED — mandatory human signature outstanding`.
S04-G1, G2, G4 and G5 require E2. **Kalpana is an approver here on feasibility**, not a reviewer.
Silence does not approve this stage.

— **Rajal**, Principal Insurance Platform Product Owner, 2026-08-16
