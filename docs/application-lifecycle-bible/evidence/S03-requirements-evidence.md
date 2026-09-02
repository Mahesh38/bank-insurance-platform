# S03 — Business Requirements & Process Design · Retroactive Stage Evidence

**Stage definition:** [`stages/S03-business-requirements.md`](../stages/S03-business-requirements.md)
**Workstream:** WS-3 ([charter](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md))
**Author:** Rajal — Principal Insurance Platform Product Owner (Board 3 / R1)
**Date:** 2026-08-16
**Closes:** GAP-008 (acceptance criteria) · GAP-016 (information-model attributes)

**Retroactive stage verdict: `CLOSED-WITH-CONDITIONS`** — see §8.

> **The test of this stage** ([S03 §1](../stages/S03-business-requirements.md#1-purpose)): hand a
> requirement to an engineer and a tester separately and see whether they produce the same system.
> Everything below is written to that test. Where I could not make a criterion observable, I have
> said so rather than softened the wording.

---

## 1. What the stage requires

| # | Criterion | Level | Closes |
|---|---|---|---|
| S03-G1 | BRD complete with acceptance criteria on **every** requirement | E2 | **GAP-008** |
| S03-G2 | To-be processes modelled including exception paths | E1 | |
| S03-G3 | Business rules catalogue approved | E2 | GAP-012, GAP-013, GAP-014 |
| S03-G4 | Information model with attribute sheets | E2 | **GAP-016** |
| S03-G5 | Every S02 control traces to a requirement | E1 | |
| S03-G6 | QA confirms every requirement is testable | E2 | |
| S03-G7 | Business stakeholders accept the requirement set | E2 | |

**Approvers:** Rajal (AP) · Shailja (AP, B) · **Swapnali (AP, testability)** · Mahesh (RV) ·
Aarti (RV) · Deepali (RV) · Kalpana (RV)

---

## 2. What already exists

| Criterion | Artefact | Path | Assessment |
|---|---|---|---|
| S03-G1 | BRD overview and chapter map | [`requirements/BRD-OVERVIEW.md`](../../au-bank-insurance-platform/requirements/BRD-OVERVIEW.md) | 🟢 Structure is right |
| S03-G1 | BRD checklist | [`BRD-OVERVIEW-CHECKLIST.md`](../../au-bank-insurance-platform/requirements/BRD-OVERVIEW-CHECKLIST.md) | 🟢 |
| S03-G1 | **P0 capabilities with AC** — 24 requirements across 14 BR prefixes | [`BRD-P0-CAPABILITIES.md`](../../au-bank-insurance-platform/requirements/BRD-P0-CAPABILITIES.md) | 🟡 **Better than the gap register implies.** ACs exist and are mostly observable — but they are bullet assertions, not Given/When/Then, and eight carry an unresolved **confirm** marker |
| S03-G1 | PRD for R0 | [`PRD-R0-DISTRIBUTION-PLATFORM.md`](../../au-bank-insurance-platform/requirements/PRD-R0-DISTRIBUTION-PLATFORM.md) | 🟢 |
| S03-G2 | Business process catalogue (BP-xxx) | [`knowledge-base/05-business-processes-catalogue.md`](../../au-bank-insurance-platform/knowledge-base/05-business-processes-catalogue.md) | 🟢 |
| S03-G2 | Process and journey canvas (CJ / RMJ / JRN) | [`04-process-and-journey-canvas.md`](../../au-bank-insurance-platform/04-process-and-journey-canvas.md) | 🟢 |
| S03-G3 | Working decisions carrying the locked business rules | [`07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md`](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) | 🟢 D-001…D-012 |
| S03-G3 | Consent and suitability rules | [consent pack](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md) · [suitability pack](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md) | 🟢 **New at S02** — 86 rules with tests |
| S03-G3 | Quote rules (GAP-012) and product matrix (GAP-013) | [S04 evidence §4](./S04-product-definition-evidence.md) | 🟢 **New** — see that file; they are product-definition artefacts |
| S03-G4 | Information model and rules — entities and relationships | [`knowledge-base/07-information-model-and-rules.md`](../../au-bank-insurance-platform/knowledge-base/07-information-model-and-rules.md) | 🟡 Entities present; **attributes absent** — GAP-016 |
| S03-G5 | Traceability example (4 rows) | [`BRD-P0-CAPABILITIES.md` §Traceability example](../../au-bank-insurance-platform/requirements/BRD-P0-CAPABILITIES.md#traceability-example) | 🔴 An *example*, not a matrix |

**Correction to the gap register.** GAP-008 reads *"BR templates lack AC"*. That is now inaccurate:
`BRD-P0-CAPABILITIES.md` v0.3 carries acceptance criteria for all 24 P0 requirements. The real
defects are narrower and I am restating them honestly in §3 rather than closing a gap against a
description that no longer matches the artefact.

---

## 3. What was actually missing

| # | Defect | Criterion | Why it matters |
|---|---|---|---|
| D1 | ACs are **assertion bullets, not Given/When/Then**. "Expired quote cannot proceed" does not say what the actor sees, what the API returns, or what happens next | S03-G1, S03-VT-02 | Two people read it two ways, which is the exact failure the stage exists to prevent |
| D2 | **Eight unresolved `confirm` markers** inside published ACs (BR-SEC-030 AC4, BR-CUST-010 AC1, BR-LEAD-020 AC2, BR-SUIT-020 AC2, BR-QUOTE-010 AC1, and others) | S03-G1 | A requirement with an open question inside its AC cannot be estimated or tested |
| D3 | **Failure and exception behaviour largely absent.** [S03-E01-S05](../stages/S03-business-requirements.md#3-epics-and-stories) requires timeout, rejection, abandonment, duplicate and partial-failure behaviour per requirement | S03-G2, S03-VT-03 | Every happy path in an insurance journey creates a real operational case |
| D4 | **No traceability matrix.** A regulator asking *"show me the control implementing suitability"* has no answer path | S03-G5, S03-VT-06 | Compounds with D1: an unverifiable requirement set cannot support S12 certification even once code exists |
| D5 | **No attribute sheets** — GAP-016. Blocks S06 logical modelling and Aarti's physical schema | S03-G4 | |
| D6 | **GAP-014** — `agentId` / RM mapping model, including expired-certification behaviour | S03-G3 | `agentId` is mandatory on regulated records; nothing defines where it comes from or what happens when it lapses |

---

## 4. New evidence — R0 acceptance criteria in Given/When/Then

Scope: the **R0 assisted Term journey** only
([WS-3 §2.2](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md#22-the-release-slice-this-workstream-is-cut-to--r0)).
ID scheme `AC-<BR-ID>-<n>`. These **supplement** `BRD-P0-CAPABILITIES.md`; they do not replace it.

### 4.1 Identity and access

| ID | Given / When / Then | Source |
|---|---|---|
| `AC-SEC-010-1` | **Given** an unauthenticated caller **When** any R0 journey API is called **Then** the response is `401` and no journey state is created or read | BR-SEC-010 |
| `AC-SEC-010-2` | **Given** an authenticated RM session **When** any journey action is performed **Then** every resulting audit event carries the same stable `actorId` for that RM across the whole journey | BR-SEC-010 |
| `AC-SEC-010-3` | **Given** an RM whose session has expired **When** a journey API is called **Then** `401` is returned and the journey is left at its last committed step with no partial write | BR-SEC-010 |
| `AC-SEC-020-1` | **Given** an RM in role `RM` **When** they attempt to read a lead owned by a different RM outside their hierarchy **Then** `403 FORBIDDEN` is returned and the attempt is audited | BR-SEC-020 |
| `AC-SEC-020-2` | **Given** an RM in role `RM` **When** they attempt to change distributor configuration **Then** `403 FORBIDDEN` with a stable error code | BR-SEC-020 |
| `AC-SEC-030-1` | **Given** any of lead-create, consent, suitability, quote, proposal, payment or status-check **When** the action completes with any outcome **Then** an audit event exists carrying `actorId`, action, `journeyId`, timestamp and outcome | BR-SEC-030 |
| `AC-SEC-030-2` | **Given** a proposal or payment action **When** the audit event is written **Then** it carries `distributorId` sourced server-side and `agentId`, and a request supplying `distributorId` has that value ignored | BR-SEC-030 |
| `AC-SEC-040-1` | **Given** the full R0 test suite runs **When** all emitted logs are scanned for PAN, Aadhaar, full mobile, email, DOB and payment-URL patterns **Then** zero matches are found | BR-SEC-040, [S08-VT-06](../stages/S08-engineering-foundation.md#4-validation-tests) |
| `AC-SEC-030-3` | **Given** the audit store is unavailable **When** a **consent** write is attempted **Then** the business action fails closed and no downstream call occurs | [CNS-R37](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#9-failure-behaviour) — **resolves D2 for consent** |

> **`AC-SEC-030-3` resolves the `confirm` marker in BR-SEC-030 AC4 for consent only.** Fail-open on
> *non-consent* audit events remains Deepali's and Shailja's determination (S03-OPEN-02).

### 4.2 Customer and lead

| ID | Given / When / Then | Source |
|---|---|---|
| `AC-CUST-010-1` | **Given** an RM searching by CIF, registered mobile or PAN **When** exactly one ETB customer matches **Then** name and policy-permitted identifiers are returned, and the search is audited | BR-CUST-010 — **resolves D2**: the three keys are now named |
| `AC-CUST-010-2` | **Given** a search with no match **When** results are rendered **Then** an explicit empty state is shown, no customer record is created, and no journey may start | BR-CUST-010 |
| `AC-CUST-010-3` | **Given** a search matching a non-ETB identity **When** results are rendered **Then** the customer is shown as ineligible for R0 with the reason, and no journey may start | BR-CUST-010, [D-009](../../au-bank-insurance-platform/DECISION-LOG.md) |
| `AC-CUST-020-1` | **Given** a selected ETB customer with a valid `CNS-DP` consent **When** the journey is created **Then** party fields available from CIF are prefilled and `prefillSource` + `prefillAt` are recorded | BR-CUST-020 |
| `AC-CUST-020-2` | **Given** no valid `CNS-DP` consent **When** prefill is attempted **Then** `403 CONSENT_REQUIRED` and no CIF data enters the journey | [CNS-R](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#2-consent-events-in-r0) |
| `AC-CUST-020-3` | **Given** a prefilled field the RM corrects **When** the correction is saved **Then** both the original CIF value and the corrected value are retained, with actor and timestamp | BR-CUST-020 |
| `AC-LEAD-010-1` | **Given** an authenticated RM and a selected ETB customer **When** a lead is created for LOB `LIFE` **Then** a unique `leadId` and `journeyId` are returned and the lead is owned by the creating RM | BR-LEAD-010 |
| `AC-LEAD-020-1` | **Given** an in-progress lead **When** the owning RM reopens it **Then** the journey resumes at the last *incomplete* step, with all prior step data intact | BR-LEAD-020 |
| `AC-LEAD-020-2` | **Given** a lead with no activity for **90 days** **When** it is next read **Then** its status is `DORMANT`; it may be reopened, and reopening re-validates consent and suitability currency | BR-LEAD-020 — **resolves D2**: TTL is now 90 days, aligned to [CNS-R21](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#6-validity-expiry-and-reuse) |
| `AC-LEAD-030-1` | **Given** a journey at any stage **When** its status is read **Then** the status names the value-stream stage, and the full status history with timestamps and actors is queryable | BR-LEAD-030 |

### 4.3 Consent and suitability — the two hard gates

Full behaviour is in the rule packs; these are the journey-level criteria that consume them.

| ID | Given / When / Then | Source |
|---|---|---|
| `AC-CONSENT-010-1` | **Given** a journey with no `CNS-SOL` consent **When** the suitability questionnaire is requested **Then** `403 CONSENT_REQUIRED` naming the consent event, with an action path to capture it | [CNS §9](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#9-failure-behaviour) |
| `AC-CONSENT-010-2` | **Given** a consent capture request **When** it carries no verified OTP transaction **Then** it is rejected and no consent row is written | CNS-R10 |
| `AC-CONSENT-010-3` | **Given** a captured consent **When** the record is retrieved **Then** it carries statement text, version, SHA-256 hash, CIF, OTP txn ID, `capturedAt`, source IP, actor and `agentId` | CNS-R01, §5 |
| `AC-CONSENT-010-4` | **Given** a stored consent record **When** an UPDATE or DELETE is attempted using the application's database role **Then** it fails on privilege, not on application logic | CNS-R16 |
| `AC-CONSENT-020-1` | **Given** a consent captured 91 days ago **When** the journey attempts to progress **Then** `403 CONSENT_EXPIRED` with the capture date and a re-consent path | CNS-R21 |
| `AC-CONSENT-030-1` | **Given** a mid-journey consent withdrawal **When** the next journey API is called **Then** `403 JOURNEY_HALTED_CONSENT_WITHDRAWN`, and no further insurer call occurs | CNS-R29 |
| `AC-SUIT-010-1` | **Given** a questionnaire submission missing any of the 12 mandatory inputs **When** submitted **Then** `422 SUITABILITY_INCOMPLETE` listing the missing input IDs, and no evaluation row is written | SUIT-R01 |
| `AC-SUIT-010-2` | **Given** the reference profile `SUIT-TC-REF-01` **When** evaluated twice, on different days, by different actors **Then** `recommendedCover`, `protectionGap`, `affordabilityRatio` and all class outcomes are identical | SUIT-R05, §3.4 |
| `AC-SUIT-020-1` | **Given** a completed evaluation **When** it is retrieved **Then** it carries all 12 raw answers, the derived values, per-class outcomes with **every** fired reason code, `algorithmVersion` and `questionnaireVersion` | SUIT-R02, R19a |
| `AC-SUIT-030-1` | **Given** a quote request with **no** suitability evaluation ID **When** the quote API is called directly, bypassing UI and BFF **Then** `403 SUITABILITY_EVALUATION_REQUIRED` and no aggregator call is made | **SUIT-R20, R25** |
| `AC-SUIT-030-2` | **Given** an evaluation older than 30 days **When** a quote is requested **Then** `403 SUITABILITY_EVALUATION_EXPIRED` | SUIT-R20 |
| `AC-SUIT-030-3` | **Given** an evaluation bound to CIF `A` **When** a quote is requested for CIF `B` **Then** `403 SUITABILITY_SUBJECT_MISMATCH` **and** a security event is emitted | SUIT-R20, R28 |
| `AC-SUIT-030-4` | **Given** outcome `NOT_SUITABLE` for the requested class **When** a quote is requested **Then** `403 PRODUCT_CLASS_NOT_SUITABLE`, and the same refusal occurs through UI, API, admin console and direct service call | SUIT-R38 |
| `AC-SUIT-030-5` | **Given** the Suitability service is unavailable **When** a quote is requested **Then** `503 SUITABILITY_SERVICE_UNAVAILABLE` and **no quote is generated** | **SUIT-R26 — fail closed** |
| `AC-SUIT-030-6` | **Given** any attempt to disable the gate by feature flag, configuration, environment variable or request header in an environment holding customer data **When** a quote is requested **Then** the gate still applies | **SUIT-R40** |
| `AC-SUIT-020-2` | **Given** an RM attempting to override a `NOT_SUITABLE` outcome **When** the attempt is made through any surface **Then** it is refused; **no override path exists in R0** | **SUIT-R38 — resolves D2** for the BR-SUIT-020 AC2 `confirm` marker |

### 4.4 Catalogue, quote and comparison

| ID | Given / When / Then | Source |
|---|---|---|
| `AC-PROD-010-1` | **Given** a customer with an `ACTIVE` evaluation **When** eligible products are requested **Then** only products whose class outcome is `SUITABLE` or `SUITABLE_WITH_CAUTION`, and whose matrix eligibility the customer satisfies, are returned | BR-PROD-010, [R0 matrix](./S04-product-definition-evidence.md#42-the-r0-product-matrix--closes-gap-013) |
| `AC-PROD-010-2` | **Given** zero eligible products **When** the list is rendered **Then** an explicit empty state is shown with the reason and **no quote call is made** | BR-PROD-010 |
| `AC-QUOTE-010-1` | **Given** a valid evaluation, a selected eligible product and an `Idempotency-Key` **When** a quote is requested **Then** a bank `quoteJobId` is returned immediately and no aggregator identifier is exposed to the caller | BR-QUOTE-010 |
| `AC-QUOTE-010-2` | **Given** the same `Idempotency-Key` replayed within its window **When** the quote is requested again **Then** the original `quoteJobId` is returned and exactly one aggregator call has been made | BR-QUOTE-010 |
| `AC-QUOTE-010-3` | **Given** an R0 quote request **When** it is constructed **Then** it targets **exactly one** Group A insurer per request; multi-insurer fan-out is R1 | **Resolves D2** for the BR-QUOTE-010 AC1 `confirm` marker |
| `AC-QUOTE-020-1` | **Given** a quote job in progress **When** its status is polled **Then** exactly one of `PENDING`, `PARTIAL`, `COMPLETED`, `FAILED`, `TIMEOUT` is returned in bank vocabulary | BR-QUOTE-020 |
| `AC-QUOTE-020-2` | **Given** a job where insurer A succeeded and insurer B failed **When** results are read **Then** status is `PARTIAL`, A's offer is present, and B's failure is reported with a normalised reason | BR-QUOTE-020 |
| `AC-QUOTE-020-3` | **Given** a returned offer **When** it is rendered **Then** premium, sum assured, term, frequency and key benefits appear in the bank's canonical field names, not the insurer's | BR-QUOTE-020 |
| `AC-COMP-010-1` | **Given** ≥ 2 offers **When** compared **Then** the comparison basis is displayed, the ranking rule is disclosed, and no offer is visually privileged other than by the disclosed rule | BR-COMP-010, [S05 §4](./S05-experience-evidence.md) |
| `AC-COMP-010-2` | **Given** a selected offer **When** the proposal step is entered **Then** the selection is persisted with timestamp and actor, and proposal cannot start without it | BR-COMP-010 |
| `AC-COMP-010-3` | **Given** an offer past its validity **When** it is selected **Then** `409 QUOTE_EXPIRED`, the offer is marked expired in the UI, and a refresh path is offered | BR-COMP-010, [quote rules](./S04-product-definition-evidence.md#43-quote-rules--closes-gap-012) |

### 4.5 Proposal, payment, issuance

| ID | Given / When / Then | Source |
|---|---|---|
| `AC-PROP-010-1` | **Given** a selected offer **When** the proposal form is requested **Then** the dynamic schema for that offer is returned and rendered without a hardcoded insurer-specific screen as the only path | BR-PROP-010 |
| `AC-PROP-010-2` | **Given** mapped source data **When** the form is rendered **Then** fields available from customer, suitability and quote are prefilled and marked as prefilled | BR-PROP-010 |
| `AC-PROP-020-1` | **Given** a partially completed proposal **When** it is saved and later reopened **Then** every entered value is restored and validation state is recomputed, not restored | BR-PROP-020 |
| `AC-PROP-030-1` | **Given** a submission with no `agentId` **When** submitted **Then** `422 AGENT_ATTRIBUTION_MISSING` and **no insurer call is made** | BR-PROP-030 |
| `AC-PROP-030-2` | **Given** an RM whose IRDAI SP certification expired before the submission timestamp **When** submitted **Then** `403 AGENT_CERTIFICATION_EXPIRED`, no insurer call, and an ops task is raised | **New — closes GAP-014's behavioural half** |
| `AC-PROP-030-3` | **Given** no valid `CNS-SHR` consent for the target insurer **When** submitted **Then** `403 CONSENT_REQUIRED` and no insurer call | CNS-R22 |
| `AC-PROP-030-4` | **Given** a successful submission **When** it completes **Then** bank proposal and application references are returned and the audit event carries `agentId` and server-sourced `distributorId` | BR-PROP-030 |
| `AC-UW-010-1` | **Given** a submitted application **When** status is refreshed **Then** a normalised bank status and the insurer's raw substatus are both returned | BR-UW-010 |
| `AC-UW-010-2` | **Given** an application the insurer does not recognise **When** status is refreshed **Then** a clean `not found` business state is returned, not a 5xx | BR-UW-010 |
| `AC-PAY-010-1` | **Given** a payable application **When** a payment session is created **Then** an HTTPS `paymentUrl` is returned to the client, and the URL appears in **no** log at any level | BR-PAY-010 |
| `AC-PAY-010-2` | **Given** an RM-assisted journey **When** payment is initiated **Then** the payment link is delivered to the customer's CBS-registered device, and the **RM surface renders no payment form and no card or UPI input at any point** | **BR-PAY-010, [D-006](../../au-bank-insurance-platform/DECISION-LOG.md), RBI** |
| `AC-PAY-010-3` | **Given** a non-payable application **When** a session is requested **Then** `409` with a business reason | BR-PAY-010 |
| `AC-PAY-020-1` | **Given** a payment attempt **When** it completes or fails **Then** the platform records `initiated`, `success` or `failure` with the PG reference, and the RM sees the state without seeing payment instrument data | BR-PAY-020 |
| `AC-PAY-020-2` | **Given** a failed payment **When** retry is permitted by rule **Then** a new session is created against the same application, and both attempts are retained | BR-PAY-020 |
| `AC-POL-010-1` | **Given** the insurer issues a policy **When** issuance is received **Then** policy number and status are visible to the RM and the journey moves to `POLICY_ISSUED` | BR-POL-010 |
| `AC-POL-010-2` | **Given** an issued policy **When** `Policy Sold` is evaluated **Then** it is `true` only if issuance, API confirmation, PG reconciliation **and** persistence in bank audit stores are all present — **all four** | [D-007](../../au-bank-insurance-platform/DECISION-LOG.md) |
| `AC-REP-010-1` | **Given** a pilot reporting window **When** the funnel is produced **Then** counts exist for leads, consents, suitability completions, quotes, proposals, payments and issued policies, each reconcilable to journey records | BR-REP-010 |

### 4.6 Failure and exception behaviour — closes D3

[S03-VT-03](../stages/S03-business-requirements.md#4-validation-tests) requires **zero** requirements
with only a happy path.

| ID | Failure class | Given / When / Then |
|---|---|---|
| `AC-EXC-01` | Insurer timeout | **Given** an aggregator call exceeding its timeout **When** it elapses **Then** the job moves to `TIMEOUT`, the RM sees a retry path, no partial insurer state is presented as complete, and the timeout is audited |
| `AC-EXC-02` | Partial fan-out failure | Covered by `AC-QUOTE-020-2` |
| `AC-EXC-03` | Duplicate submission | **Given** a repeated proposal submission with the same idempotency key **When** submitted **Then** the original reference is returned and exactly one insurer submission exists |
| `AC-EXC-04` | Underwriting rejection | **Given** the insurer rejects **When** status is refreshed **Then** the journey moves to `UW_DECLINED` with the insurer's reason preserved verbatim and a normalised bank reason, and an ops task is raised |
| `AC-EXC-05` | Payment failure | **Given** a PG failure **When** the outcome is received **Then** the journey stays at `PAYMENT_PENDING`, retry is offered per rule, and the failure reason is recorded |
| `AC-EXC-06` | Payment succeeded, issuance did not | **Given** a successful payment and no issuance within the insurer SLA **When** the SLA elapses **Then** an ops task `PAID_NOT_ISSUED` is raised with the PG reference, and the journey is **not** counted as sold |
| `AC-EXC-07` | Consent withdrawal mid-journey | Covered by `AC-CONSENT-030-1` and [CNS-R30](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#7-withdrawal) |
| `AC-EXC-08` | Suitability invalidated mid-journey | **Given** an evaluation invalidated by [SUIT-R32](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#63-invalidation-and-re-evaluation-triggers) **When** the journey has not reached `PROPOSAL_SUBMITTED` **Then** it is blocked pending re-evaluation; **When** it has, the journey is unaffected (SUIT-R34) |
| `AC-EXC-09` | Abandonment | **Given** no activity for 90 days **When** the lead is next read **Then** status is `DORMANT`; reopening re-validates consent and suitability currency |
| `AC-EXC-10` | CBS unavailable | **Given** CBS is unreachable **When** customer search is attempted **Then** a degraded state is shown, **no journey starts**, and no customer data is fabricated or cached beyond policy |
| `AC-EXC-11` | Audit store unavailable | Consent path: `AC-SEC-030-3`, fail closed. Non-consent path: **S03-OPEN-02**, Deepali + Shailja |
| `AC-EXC-12` | Aggregator returns malformed payload | **Given** a response failing schema validation **When** received **Then** it is rejected at the adapter boundary, normalised to a bank error, the raw payload is retained for dispute, and no malformed value enters the canonical model |

---

## 5. Attribute sheets — closes GAP-016

Business-level attribute definitions for the R0 P0 objects: name, type, optionality, validation,
classification and source of truth. **Physical design — column types, indexes, partitioning,
grants — is Aarti's**, and this sheet is her input, not her output.

Consent and Suitability are **not repeated here**; their complete field sets are in
[consent pack §5](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#5-the-consent-evidence-record)
and [suitability pack §5](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#5-the-evaluation-record).

**Classification key:** `PUB` public · `INT` internal · `CONF` confidential · `RES` restricted (PII).

### 5.1 Customer (journey snapshot — the bank's CBS is the SoT)

| Attribute | Type | Opt | Validation | Class | SoT |
|---|---|---|---|---|---|
| `customerCif` | String(20) | M | CBS format | RES | CBS |
| `fullName` | String(140) | M | non-empty | RES | CBS |
| `dateOfBirth` | Date | M | age 18–65 at next birthday | RES | CBS |
| `gender` | Enum | M | — | RES | CBS |
| `registeredMobile` | String(10) | M | Indian mobile format | RES | CBS |
| `email` | String(254) | O | RFC 5322 | RES | CBS |
| `panMasked` | String | O | PAN format; **stored masked** | RES | CBS |
| `residentialAddress` | Structured | M | pincode valid | RES | CBS |
| `etbRelationshipTypes` | Array[Enum] | M | ≥ 1 element | INT | CBS |
| `prefillSource` | Enum | M | `CBS` | INT | Platform |
| `prefillAt` | Timestamp | M | ISO-8601 + offset | INT | Platform |
| `rmCorrectedFields` | Array[{field, originalValue, newValue, actorId, at}] | O | append-only | RES | Platform |

### 5.2 Lead / Journey

| Attribute | Type | Opt | Validation | Class | SoT |
|---|---|---|---|---|---|
| `leadId` | String(20) | M | unique | INT | Platform |
| `journeyId` | UUID | M | unique | INT | Platform |
| `customerCif` | String(20) | M | FK | RES | CBS |
| `lob` | Enum | M | `LIFE` in R0 | INT | Platform |
| `channel` | Enum | M | `ASSISTED` in R0 | INT | Platform |
| `ownerActorId` | String | M | valid RM | INT | Identity (#3) |
| `ownerAgentId` | String | M | valid, **unexpired** SP licence | CONF | Identity (#3) |
| `currentStage` | Enum | M | one of the 11 R0 stages | INT | Platform |
| `stageHistory` | Array[{stage, enteredAt, actorId}] | M | append-only | INT | Platform |
| `status` | Enum `ACTIVE\|DORMANT\|HALTED\|COMPLETED\|DECLINED` | M | — | INT | Platform |
| `lastActivityAt` | Timestamp | M | drives 90-day dormancy | INT | Platform |
| `consentIds` | Array[UUID] | M | ≥ 1 | INT | Consent (#6) |
| `suitabilityEvaluationId` | UUID | O until suitability | FK, `ACTIVE` | INT | Suitability (#7) |
| `distributorId` | String | M | **server-side secret only** | CONF | Config |

### 5.3 Quote / Offer

| Attribute | Type | Opt | Validation | Class | SoT |
|---|---|---|---|---|---|
| `quoteJobId` | UUID | M | unique | INT | Quotation (#10) |
| `journeyId` | UUID | M | FK | INT | Platform |
| `suitabilityEvaluationId` | UUID | **M** | `ACTIVE`, ≤ 30d, CIF-matched | INT | Suitability (#7) |
| `idempotencyKey` | String(64) | M | client-supplied, unique per journey | INT | Caller |
| `insurerCode` | Enum | M | Group A panel | INT | Catalogue (#8) |
| `productCode` | String | M | in R0 matrix | INT | Catalogue (#8) |
| `requestedSumAssured` | Currency | M | within product min/max | CONF | Platform |
| `requestedTermYears` | Integer | M | within product range | INT | Platform |
| `premiumFrequency` | Enum | M | product-supported | INT | Platform |
| `jobStatus` | Enum | M | 5 values (`AC-QUOTE-020-1`) | INT | Quotation (#10) |
| `offers` | Array[Offer] | O | — | CONF | Quotation (#10) |
| `Offer.premiumAmount` | Currency | M | > 0 | CONF | Insurer |
| `Offer.validUntil` | Timestamp | M | per [quote rules](./S04-product-definition-evidence.md#43-quote-rules--closes-gap-012) | INT | Quotation (#10) |
| `Offer.insurerQuoteRef` | String | M | — | CONF | Insurer |
| `selectedOfferId` | UUID | O until selection | one of `offers` | INT | Platform |

### 5.4 Proposal

| Attribute | Type | Opt | Validation | Class | SoT |
|---|---|---|---|---|---|
| `proposalId` | UUID | M | unique | INT | Proposal (#11) |
| `journeyId` / `selectedOfferId` | UUID | M | FK | INT | Platform |
| `consentIdShr` | UUID | **M** | `ACTIVE`, insurer-scoped | INT | Consent (#6) |
| `agentId` | String | **M** | unexpired at submission | CONF | Identity (#3) |
| `schemaVersion` | String | M | insurer schema version | INT | Integration Hub (#14) |
| `answers` | Map | M | validated against schema | RES | Platform |
| `nominees` | Array[{name, dob, relationship, share%}] | M | shares sum to 100 | RES | Platform |
| `status` | Enum `DRAFT\|SUBMITTED\|UW_PENDING\|UW_DECLINED\|APPROVED` | M | — | INT | Proposal (#11) |
| `insurerApplicationRef` | String | O until submitted | — | CONF | Insurer |
| `submittedAt` | Timestamp | O until submitted | — | INT | Platform |

### 5.5 Payment

| Attribute | Type | Opt | Validation | Class | SoT |
|---|---|---|---|---|---|
| `paymentSessionId` | UUID | M | unique | INT | Payment (#12) |
| `proposalId` | UUID | M | FK | INT | Platform |
| `amount` / `currency` | Currency / ISO-4217 | M | = offer premium; `INR` | CONF | Platform |
| `paymentUrlRef` | Opaque handle | M | **the URL itself is never persisted in a loggable field** | CONF | Payment (#12) |
| `deliveryTarget` | Enum `CUSTOMER_DEVICE` | M | **only this value is valid** | INT | Platform |
| `deliveredToMobile` | String(10) | M | = CBS registered mobile | RES | CBS |
| `status` | Enum `INITIATED\|SUCCESS\|FAILURE\|EXPIRED` | M | — | INT | Payment (#12) |
| `pgTransactionRef` | String | O until outcome | — | CONF | AU Bank PG |
| `attempts` | Array[{at, status, reason}] | M | append-only | INT | Payment (#12) |

### 5.6 Policy

| Attribute | Type | Opt | Validation | Class | SoT |
|---|---|---|---|---|---|
| `policyId` | UUID | M | unique | INT | Policy (#13) |
| `policyNumber` | String | M | insurer-issued | CONF | Insurer |
| `proposalId` / `journeyId` | UUID | M | FK | INT | Platform |
| `issuedAt` | Timestamp | M | — | INT | Insurer |
| `issuanceConfirmationRef` | String | M | API/webhook confirmation | CONF | Insurer |
| `reconciliationStatus` | Enum `PENDING\|MATCHED\|EXCEPTION` | M | — | INT | Finance |
| `auditPersistedAt` | Timestamp | M | — | INT | Audit (#16) |
| **`isSold`** | Boolean | M | **derived, not set**: true only when issuance ∧ confirmation ∧ reconciliation `MATCHED` ∧ `auditPersistedAt` are all present | INT | Platform |
| `documentRef` | S3 ref | O | — | RES | Policy (#13) |

> **`isSold` is derived, never written.** [D-007](../../au-bank-insurance-platform/DECISION-LOG.md)
> defines four conditions; a settable flag is how three-of-four quietly becomes "sold" under
> reporting pressure, and KPI-01 is the primary commercial metric.

---

## 6. Traceability matrix — closes D4, satisfies S03-G5

**Capability → requirement → acceptance criterion → the stage that proves it.**
Bidirectional: no requirement without a capability, no S02 control without a requirement.

| Capability | Requirement | Acceptance criteria | S02 control / rule | Proven at |
|---|---|---|---|---|
| Identity, Access, Audit (BR-SEC) | BR-SEC-010 RM authentication | `AC-SEC-010-1/2/3` | Attribution | S11 |
| | BR-SEC-020 Role-based access | `AC-SEC-020-1/2` | Least privilege | S11 |
| | BR-SEC-030 Audit trail | `AC-SEC-030-1/2/3` | Immutable audit; attribution | S11 / S12 |
| | BR-SEC-040 PII protection | `AC-SEC-040-1` | PII masking | **S08** (`S08-VT-06`) |
| Customer Management (BR-CUST) | BR-CUST-010 Search | `AC-CUST-010-1/2/3` | ETB scope | S11 |
| | BR-CUST-020 Prefill | `AC-CUST-020-1/2/3` | `CNS-DP` | S11 |
| Lead Management (BR-LEAD) | BR-LEAD-010/020/030 | `AC-LEAD-010-1`, `AC-LEAD-020-1/2`, `AC-LEAD-030-1` | — | S11 |
| **Consent (BR-CONSENT)** | BR-CONSENT-010 Capture | `AC-CONSENT-010-1/2/3/4` | **CNS-R01–R20** | S11 |
| | BR-CONSENT-020 Validity | `AC-CONSENT-020-1` | CNS-R21–R26 | S11 |
| | BR-CONSENT-030 Withdrawal | `AC-CONSENT-030-1` | CNS-R27–R32 | S11 |
| | *(retention)* | — | CNS-R33–R36 | **S09** |
| **Suitability (BR-SUIT)** | BR-SUIT-010 Capture | `AC-SUIT-010-1/2` | SUIT-R01–R08 | S11 |
| | BR-SUIT-020 Recommendation | `AC-SUIT-020-1/2` | SUIT-R09–R19a, R38 | S11 |
| | BR-SUIT-030 **Hard gate** | `AC-SUIT-030-1…6` | **SUIT-R20–R28, R40** | **S11, 100% branch coverage (S08-VT-07)** |
| Product Catalogue (BR-PROD) | BR-PROD-010/020 | `AC-PROD-010-1/2` | R0 matrix | S11 |
| Quote (BR-QUOTE / BR-COMP) | BR-QUOTE-010/020 | `AC-QUOTE-010-1/2/3`, `AC-QUOTE-020-1/2/3` | Suitability gate; attribution | S11 |
| | BR-COMP-010 | `AC-COMP-010-1/2/3` | No dark patterns | S11 |
| Proposal (BR-PROP) | BR-PROP-010/020/030 | `AC-PROP-010-1/2`, `AC-PROP-020-1`, `AC-PROP-030-1…4` | Attribution; `CNS-SHR`; GAP-014 | S11 |
| Underwriting (BR-UW) | BR-UW-010 | `AC-UW-010-1/2` | — | S11 |
| **Payment (BR-PAY)** | BR-PAY-010 | `AC-PAY-010-1/2/3` | **RBI device isolation** | **S11, with a device-isolation test** |
| | BR-PAY-020 | `AC-PAY-020-1/2` | — | S11 |
| Policy (BR-POL) | BR-POL-010 | `AC-POL-010-1/2` | `Policy Sold`, all four | S11 / S12 |
| Reporting (BR-REP) | BR-REP-010 | `AC-REP-010-1` | KPI-01/02 | S11 |
| Integration Hub (BR-INT) | BR-INT-010/020/030 | *(WS-1 owned)* | Adapter boundary; 7-yr raw evidence | S10 / **S09** for retention |
| **All exception paths** | — | `AC-EXC-01…12` | Fail-closed behaviour | S11 / S12 |

### 6.1 Reverse check — every S02 control has a requirement

| Control | Requirement | Status |
|---|---|---|
| Suitability hard-gate (403 without evaluation ID) | BR-SUIT-030 | ✅ `AC-SUIT-030-1…6` |
| Consent evidence (append-only, full field set) | BR-CONSENT-010 | ✅ `AC-CONSENT-010-1…4` |
| Attribution (`distributorId` server-side only) | BR-SEC-030, BR-PROP-030 | ✅ `AC-SEC-030-2`, `AC-PROP-030-4` |
| Payment device isolation | BR-PAY-010 | ✅ `AC-PAY-010-2` |
| Data residency | *(no business requirement — infrastructure)* | ⚠️ **S09 criterion, not an S03 requirement.** Recorded so it is not lost |
| 7-year retention | BR-INT-030 (partial) | ⚠️ Business requirement covers raw evidence only; consent/suitability retention is CNS-R33/SUIT-R24, an **S09** control |
| PII masking | BR-SEC-040 | ✅ `AC-SEC-040-1` |

**Zero orphan controls.** The two ⚠️ rows are controls whose enforcement point is infrastructure
rather than business behaviour — correctly placed at S09, and named here so the traceability walk
does not silently drop them.

---

## 7. What remains genuinely open

| ID | Item | Criterion | Owner | Target |
|---|---|---|---|---|
| S03-OPEN-01 | **Swapnali's testability review** of every AC in §4 (S03-VT-01: 100% observable) | S03-G6 (E2) | Swapnali | 2026-09-12 |
| S03-OPEN-02 | Fail-open vs fail-closed for **non-consent** audit writes (BR-SEC-030 AC4 `confirm`) | S03-G1 | Deepali + Shailja | 2026-09-12 |
| S03-OPEN-03 | GAP-014 sourcing half: where `agentId` and certification expiry come from. The *behaviour* is closed by `AC-PROP-030-2` | S03-G3 | Rajal + Ops + WS-2 | 2026-09-26 |
| S03-OPEN-04 | Remaining `confirm` markers in `BRD-P0-CAPABILITIES.md` not addressed in §4 | S03-G1 | Rajal + BA | 2026-09-26 |
| S03-OPEN-05 | Aarti's physical mapping of the §5 attribute sheets | S03-G4 (E2) | Aarti | 2026-09-26 |
| S03-OPEN-06 | S03-VT-02 run: two people independently describe behaviour from the AC and match | S03-G1 | Swapnali + Amit | 2026-09-26 |
| S03-OPEN-07 | Business stakeholder acceptance of the requirement set | S03-G7 (E2) | Rajal + Bancassurance | 2026-10-10 |
| S03-OPEN-08 | ULIP and Savings ACs (R1 classes) | — | Rajal | R1 |
| S03-OPEN-09 | Traceability maintained by tool and cadence, not as a one-off document (S03-E05-S03) | S03-G5 | Rajal + Kalpana | 2026-10-10 |

**S03-OPEN-09 is the one that decays.** §6 is accurate on 2026-08-16. Without a mechanism it will
be wrong within two sprints, and a stale traceability matrix is worse than none because it is
trusted.

---

## 8. Retroactive stage verdict

> ## `CLOSED-WITH-CONDITIONS`

| Criterion | State | Basis |
|---|---|---|
| S03-G1 BRD with AC on every requirement | **MET at E1, pending E2** | 24 P0 requirements had ACs; §4 adds 60 Given/When/Then criteria plus 12 exception criteria and resolves 5 of 8 `confirm` markers. **GAP-008 closed on content**; S03-G1 needs Compliance + QA signature |
| S03-G2 To-be processes with exception paths | **MET** | Process catalogue and journey canvas existed; `AC-EXC-01…12` supplies the exception behaviour that was missing |
| S03-G3 Business rules catalogue approved | **PARTIAL** | Consent and suitability rules ([S02](./S02-regulatory-evidence.md)); quote rules and product matrix ([S04](./S04-product-definition-evidence.md)) — GAP-012, GAP-013 closed on content. **GAP-014 half-closed**: behaviour defined, sourcing open |
| S03-G4 Information model with attribute sheets | **MET at E1** | §5 covers all six R0 objects; consent and suitability are in their packs. **GAP-016 closed on content**; Aarti's physical mapping outstanding |
| S03-G5 Every S02 control traces to a requirement | **MET** | §6 and the reverse check in §6.1. Zero orphan controls |
| S03-G6 QA confirms every requirement testable | **NOT MET** | S03-OPEN-01. **Swapnali is an approver here, not a reviewer** — this criterion is hers |
| S03-G7 Business stakeholders accept | **NOT MET** | S03-OPEN-07 |

**Gaps closed on content:** GAP-008, GAP-016, and (via S02/S04) GAP-006, GAP-007, GAP-012,
GAP-013. **GAP-014 half.** *Content-complete is not closed* — five of these require an E2 signature
I cannot supply.

**The judgement I want on record.** GAP-008 as written — *"BR templates lack AC"* — was already
stale; ACs existed. The real defects were ambiguity, unresolved `confirm` markers and absent
failure paths, and a gap register that describes a defect inaccurately hides the defect while
appearing to track it. I have restated it in the [gap register](../../au-bank-insurance-platform/po-drive/02-GAP-REGISTER.md)
rather than closing it against a description that no longer matched the artefact.

**Conditions carried forward:** S03-OPEN-01 through -09; GAP-014 sourcing half.

**Signature status:** `AI-DRAFTED — mandatory human signature outstanding`.
S03-G1, G3, G4, G6 and G7 all require E2. Swapnali (AP, testability) and Shailja (AP, B) must both
conclude. Silence does not approve this stage.

— **Rajal**, Principal Insurance Platform Product Owner, 2026-08-16
