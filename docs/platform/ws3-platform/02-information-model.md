# WS-3 — Platform Information Model

**Workstream:** WS-3 — AU Bank Insurance Distribution Platform (proposed under CR-010)
**Stage:** S06 — Domain & Information Architecture (epic S06-E04)
**Owner:** Aarti — Principal Insurance Data & Database Architect · **Architecture co-owner:** Mahesh
**Status:** AI-DRAFTED. Aarti is an **AP** approver at S06
([`04-GATE-AND-SIGNOFF-MODEL.md §5`](../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md));
her signature and the human Architecture signature are outstanding.

**Companion:** [`01-domain-model-and-invariants.md`](./01-domain-model-and-invariants.md) —
aggregates, state machines and invariants that this model must be able to represent.

---

## 1. Scope and honest limits

This is the **logical** information model for the R0 slice: entities, attributes, types,
classification, retention and system of record. It is what S06-E04 asks for and what
[`stages/S06-domain-architecture.md §6`](../../application-lifecycle-bible/stages/S06-domain-architecture.md)
records as *"Partial — blocked on GAP-016 attribute sheets"*.

Two limits are stated up front rather than glossed:

1. **This does not close GAP-016 on its own.** GAP-016 is an S03 deliverable
   ([`stages/S03-business-requirements.md`](../../application-lifecycle-bible/stages/S03-business-requirements.md),
   story S03-E04-S02) owned by Rajal and the BA. This document supplies the architecture-side
   logical model so S06 is no longer blocked on it, and records the residual as **OPEN-D7** in
   [`01-domain-model-and-invariants.md §10`](./01-domain-model-and-invariants.md). Formal GAP-016
   closure requires Product/BA ratification of the business validation rules per attribute.
2. **This is not a physical schema.** Column types, indexes, partitioning and datastore selection
   are S07/S09 and belong to Aarti with Mahesh. Where a physical decision already exists in the
   repository — `V1__init_schema.sql` — it is cited, not re-specified.

---

## 2. Classification and retention vocabulary

### 2.1 Data classification

Uses the four-level scheme required by
[`stages/S02-regulatory-framing.md`](../../application-lifecycle-bible/stages/S02-regulatory-framing.md)
story S02-E05-S01, with the PII/financial/health flags it requires.

| Class | Meaning | Handling rule |
|---|---|---|
| `PUBLIC` | Disclosable without restriction | — |
| `INTERNAL` | Bank-internal, non-personal | No external exposure |
| `CONFIDENTIAL` | Personal data or commercially sensitive | Encrypted at rest; masked in logs; access on need |
| `RESTRICTED` | Statutorily protected — Aadhaar, PAN, health, biometric, full financial detail | Encrypted at rest with a dedicated CMK; **never** in a queryable column outside the encrypted payload store; never in any log; access audited per read |

Flags: `[P]` personal data · `[F]` financial · `[H]` health · `[K]` KYC identifier.

### 2.2 Retention classes

| Class | Horizon | Immutability | Basis |
|---|---|---|---|
| `RET-7Y-IMMUTABLE` | Event time + 7 years | Write-once (S3 Object Lock / INSERT-only role) | IRDAI record-keeping; control C7 |
| `RET-7Y` | Record close + 7 years | Mutable during life, archived after | IRDAI record-keeping |
| `RET-POLICY+7Y` | Policy termination + 7 years | Mutable during life | Long-tail policy servicing and dispute |
| `RET-OPERATIONAL` | 90 days | Mutable | Operational telemetry; no regulated content |
| `RET-TRANSIENT` | ≤ 24 hours | Mutable | Idempotency keys, schema cache, OTP challenge |

> **Rule:** every attribute in §4 carries exactly one retention class. An attribute with no class is
> a defect, because it is an attribute nobody can lawfully dispose of.

### 2.3 System-of-record notation

`SoR` names the **one** context authorised to write the attribute. Everything else reads it via
contract. This is the S06-E04-S02 ownership rule and validation test S06-VT-06.

---

## 3. Data ownership matrix

S06-E04-S02 / S06-VT-06: *exactly one context may write each field.*

| Information set | Sole writer (SoR) | Readers | Read mechanism |
|---|---|---|---|
| Bank customer master (CIF) | **CBS — external** | Customer context | Synchronous CBS facade; the platform never writes CBS |
| Customer profile snapshot | Customer | Journey, Proposal, Suitability | API |
| Lead and its assignment history | Lead | RM BFF, Journey, Reporting | API + events |
| Consent grant and evidence | Consent | Suitability, Quotation, Proposal, Audit | API (reference only) |
| Suitability assessment and outcome | Suitability | Quotation, Journey, Audit | API |
| Product, insurer and eligibility matrix | Product Catalogue | Suitability, Quotation, Admin | API + cache |
| Workforce identity, roles, SP certification | **Identity & Access (WS-2)** | every context | PDP decision + principal claims |
| Quote, offers, selection | Quotation | Journey, Proposal, Reporting | API + events |
| Proposal, UW case, requirements | Proposal & UW | Journey, Policy, Reporting | API + events |
| Payment attempts and reconciliation | Payment | Journey, Policy, Finance reporting | API + events |
| Policy record and document references | Policy & Issuance | Journey, Customer, Reporting | API + events |
| Journey stage and external references | Journey Orchestration | BFFs, Reporting | API |
| Routing policy per LOB/product | Integration Hub | — | Config |
| Provider job/correlation, raw payloads | **1SB Adapter (WS-1)** | Audit (read), Operations | Internal |
| Audit events | Audit & Compliance | Compliance, Reporting | Query API, read-only |
| Configuration, feature flags, rule packs | Administration & Config | every context | Config pull |

**Ambiguities resolved explicitly**

| Question | Answer |
|---|---|
| Who owns `distributorId`? | Administration & Config holds the value; **Integration Hub injects it**. No domain service and no caller may supply it (INV-DIS-01) |
| Who owns the customer's mobile number? | CBS is SoR. The Consent evidence record keeps an **immutable copy at capture time**, because consent evidence must reflect the contact used, not today's value |
| Who owns `applicationNumber`? | The insurer mints it; **Proposal** is the platform SoR for it; Journey and the adapter hold it as an external reference |
| Who owns premium amount? | Quotation owns the quoted amount; **Payment owns the amount actually charged**. INV-PAY-03 asserts they match |
| Who owns policy documents? | Policy & Issuance owns the reference; object storage owns the bytes; the 1SB adapter's raw archive is a separate, provider-scoped record and is not the policy document |

---

## 4. Canonical entity attribute sheets

Notation: `Cls` = classification · `Ret` = retention class · `Req` = mandatory.
`⚑` marks an attribute that may never appear in a log or in an unencrypted queryable column.

### 4.1 Customer — SoR: Customer context (profile snapshot); CBS for the master

| Attribute | Type | Req | Cls | Ret | Notes |
|---|---|---|---|---|---|
| `customerId` | ULID | ✅ | INTERNAL | RET-POLICY+7Y | Bank-minted, opaque |
| `cifNumber` | string(20) | ✅ | CONFIDENTIAL [P] | RET-POLICY+7Y | CBS key; not exposed to Flutter |
| `fullName` | string(140) | ✅ | CONFIDENTIAL [P] ⚑ | RET-POLICY+7Y | |
| `dateOfBirth` | date | ✅ | CONFIDENTIAL [P] ⚑ | RET-POLICY+7Y | Drives eligibility |
| `gender` | enum | ✅ | CONFIDENTIAL [P] | RET-POLICY+7Y | |
| `pan` | string(10) | ✅ | RESTRICTED [P][K] ⚑ | RET-POLICY+7Y | Encrypted store only |
| `aadhaarRef` | token | ⭘ | RESTRICTED [P][K] ⚑ | RET-POLICY+7Y | Tokenised reference, never the number |
| `mobileNumber` | E.164 | ✅ | CONFIDENTIAL [P] ⚑ | RET-POLICY+7Y | |
| `email` | string(254) | ⭘ | CONFIDENTIAL [P] ⚑ | RET-POLICY+7Y | |
| `address` | structured | ✅ | CONFIDENTIAL [P] ⚑ | RET-POLICY+7Y | pincode, state, city, line1..3 |
| `annualIncome` | money(INR) | ✅ | RESTRICTED [P][F] ⚑ | RET-POLICY+7Y | Suitability and insurer input |
| `tobaccoUse` | boolean | ✅ | RESTRICTED [P][H] ⚑ | RET-POLICY+7Y | Health attribute |
| `snapshotTakenAt` | timestamptz | ✅ | INTERNAL | RET-POLICY+7Y | Snapshot semantics: the profile used by a journey is frozen at journey start |
| `sourceSystem` | enum | ✅ | INTERNAL | RET-POLICY+7Y | `CBS` for R0 (ETB only) |

### 4.2 Lead — SoR: Lead context

| Attribute | Type | Req | Cls | Ret | Notes |
|---|---|---|---|---|---|
| `leadId` | ULID | ✅ | INTERNAL | RET-7Y | |
| `customerId` | ref | ✅ | INTERNAL | RET-7Y | ETB only in R0 |
| `state` | enum | ✅ | INTERNAL | RET-7Y | §4.1 of the domain model |
| `lob` | enum | ✅ | INTERNAL | RET-7Y | `TERM` for R0 |
| `source` | enum | ✅ | INTERNAL | RET-7Y | RM, campaign, self-service |
| `assignedRmId` | ref | ⭘ | INTERNAL | RET-7Y | WS-2 principal |
| `assignmentHistory[]` | entity list | ✅ | INTERNAL | RET-7Y | `{rmId, assignedAt, assignedBy, reason}` — append-only |
| `followUps[]` | entity list | ⭘ | CONFIDENTIAL [P] | RET-7Y | Free-text notes may contain personal detail |
| `expiresAt` | timestamptz | ✅ | INTERNAL | RET-7Y | Ageing horizon; value is configuration (OPEN-D4) |
| `convertedJourneyId` | ref | ⭘ | INTERNAL | RET-7Y | Set once (INV-LED-02) |

### 4.3 Consent — SoR: Consent context · **append-only**

| Attribute | Type | Req | Cls | Ret | Notes |
|---|---|---|---|---|---|
| `consentId` | ULID | ✅ | INTERNAL | RET-7Y-IMMUTABLE | |
| `customerId` / `cifNumber` | ref / string | ✅ | CONFIDENTIAL [P] | RET-7Y-IMMUTABLE | CIF captured verbatim as evidence |
| `purpose[]` | enum list | ✅ | INTERNAL | RET-7Y-IMMUTABLE | e.g. data sharing with insurer, marketing |
| `statementText` | text | ✅ | INTERNAL | RET-7Y-IMMUTABLE | **Verbatim text shown**, not a key |
| `statementVersion` | string(20) | ✅ | INTERNAL | RET-7Y-IMMUTABLE | |
| `channel` | enum | ✅ | INTERNAL | RET-7Y-IMMUTABLE | RM-assisted, self-service |
| `otpTxnId` | string(64) | ✅ | CONFIDENTIAL | RET-7Y-IMMUTABLE | Verification evidence |
| `capturedAt` | timestamptz | ✅ | INTERNAL | RET-7Y-IMMUTABLE | |
| `sourceIp` | inet | ✅ | CONFIDENTIAL [P] | RET-7Y-IMMUTABLE | Required by the business statement §9.1 |
| `contactUsed` | E.164/email | ✅ | CONFIDENTIAL [P] ⚑ | RET-7Y-IMMUTABLE | Frozen copy — see §3 |
| `state` | enum | ✅ | INTERNAL | RET-7Y-IMMUTABLE | Grant state; evidence itself never changes |
| `withdrawnAt` / `withdrawnBy` / `withdrawalReason` | — | ⭘ | INTERNAL | RET-7Y-IMMUTABLE | New row, not an update |
| `validUntil` | timestamptz | ✅ | INTERNAL | RET-7Y-IMMUTABLE | Value is Compliance configuration (OPEN-D3) |

Every field in this table is required by control **C2**. The set is complete against the business
statement §9.1 list (statement text, version ID, CIF, OTP transaction ID, timestamp, IP address).

### 4.4 SuitabilityAssessment — SoR: Suitability context

| Attribute | Type | Req | Cls | Ret | Notes |
|---|---|---|---|---|---|
| `suitabilityId` | ULID | ✅ | INTERNAL | RET-7Y | The identifier INV-QUO-01 checks |
| `customerId` | ref | ✅ | INTERNAL | RET-7Y | |
| `lob` | enum | ✅ | INTERNAL | RET-7Y | |
| `questionnaireVersion` | string(20) | ✅ | INTERNAL | RET-7Y | Versioned rule pack (GAP-007) |
| `answers[]` | entity list | ✅ | RESTRICTED [P][F][H] ⚑ | RET-7Y | `{questionId, value}`; income, dependants, existing cover, health |
| `outcome` | enum | ✅ | INTERNAL | RET-7Y | `ELIGIBLE` / `NOT_ELIGIBLE` |
| `recommendedProducts[]` | ref list | ⭘ | INTERNAL | RET-7Y | Catalogue references |
| `evaluatedAt` | timestamptz | ✅ | INTERNAL | RET-7Y | |
| `validUntil` | timestamptz | ✅ | INTERNAL | RET-7Y | Assessment validity window (OPEN-D4) |
| `state` | enum | ✅ | INTERNAL | RET-7Y | §4.3 of the domain model |
| `override` | structured | ⭘ | INTERNAL | RET-7Y | `{actorId, reason, at}` — INV-SUI-02 |
| `evidenceDocumentRef` | uri | ⭘ | CONFIDENTIAL | RET-7Y | Suitability PDF per R0-SCOPE §3 |

### 4.5 Quote and Offer — SoR: Quotation context

| Attribute | Type | Req | Cls | Ret | Notes |
|---|---|---|---|---|---|
| `quoteId` | ULID | ✅ | INTERNAL | RET-7Y | |
| `journeyId`, `customerId`, `suitabilityId`, `consentId` | ref | ✅ | INTERNAL | RET-7Y | `suitabilityId` is the gate reference |
| `lob`, `mode`, `category` | enum | ✅ | INTERNAL | RET-7Y | `mode` = SINGLE/MULTI; `category` per canonical model |
| `requestedCover` | money(INR) | ✅ | CONFIDENTIAL [F] | RET-7Y | Sum assured or premium per category |
| `members[]` | entity list | ✅ | RESTRICTED [P][H] ⚑ | RET-7Y | role, sequence, DOB, gender, tobacco, income, pincode |
| `state`, `validUntil` | enum / ts | ✅ | INTERNAL | RET-7Y | |
| `externalRefs` | map | ✅ | INTERNAL | RET-7Y | `{provider, jobId}` — never returned to callers (ID-03) |
| **Offer** `offerId` | ULID | ✅ | INTERNAL | RET-7Y | |
| `insurerCode`, `productCode`, `productName` | string | ✅ | PUBLIC/INTERNAL | RET-7Y | |
| `premiumAmount`, `premiumFrequency`, `taxAmount` | money / enum | ✅ | CONFIDENTIAL [F] | RET-7Y | |
| `sumAssured`, `coverTerm`, `premiumPayingTerm` | money / int | ✅ | CONFIDENTIAL [F] | RET-7Y | |
| `outOfBound` | boolean | ✅ | INTERNAL | RET-7Y | Existing 1SB flag, canonicalised |
| `offerState` | enum | ✅ | INTERNAL | RET-7Y | `QUOTED / SELECTED / NOT_SELECTED / INVALID` |
| `errorSummary` | string(500) | ⭘ | INTERNAL | RET-7Y | Per-insurer partial failure |

The offer attribute set maps 1:1 onto the existing `integration_job_offer` table in
[`V1__init_schema.sql`](../../../services/bank-persistence-service/src/main/resources/db/migration/V1__init_schema.sql),
which is evidence that the canonical shape is already implementable rather than theoretical.

### 4.6 Proposal and UnderwritingCase — SoR: Proposal & UW context

| Attribute | Type | Req | Cls | Ret | Notes |
|---|---|---|---|---|---|
| `proposalId` | ULID | ✅ | INTERNAL | RET-POLICY+7Y | |
| `quoteId`, `offerId`, `journeyId`, `consentId` | ref | ✅ | INTERNAL | RET-POLICY+7Y | INV-PRP-02 |
| `applicationNumber` | string(50) | ⭘ | INTERNAL | RET-POLICY+7Y | Insurer-minted |
| `schemaId`, `schemaVersion` | string | ✅ | INTERNAL | RET-POLICY+7Y | Dynamic form contract |
| `formValuesRef` | uri | ✅ | RESTRICTED [P][F][H] ⚑ | RET-7Y-IMMUTABLE | **Reference only.** Values live in the encrypted payload store — INV-PRP-05 |
| `state` | enum | ✅ | INTERNAL | RET-POLICY+7Y | §4.5 of the domain model |
| `nomineeSummary` | structured | ✅ | RESTRICTED [P] ⚑ | RET-POLICY+7Y | Name, relationship, share |
| **UW case** `underwritingState` | enum | ✅ | INTERNAL | RET-POLICY+7Y | |
| `requirements[]` | entity list | ⭘ | CONFIDENTIAL [H] | RET-POLICY+7Y | `{requirementId, type, subType, description, status, dueDate}` |
| `documents[]` | entity list | ⭘ | RESTRICTED [P][H] ⚑ | RET-7Y-IMMUTABLE | Object references, never inline bytes |
| `counterOffer` | structured | ⭘ | CONFIDENTIAL [F] | RET-POLICY+7Y | Revised premium/sum assured |

### 4.7 Payment — SoR: Payment context

| Attribute | Type | Req | Cls | Ret | Notes |
|---|---|---|---|---|---|
| `paymentId` | ULID | ✅ | INTERNAL | RET-7Y-IMMUTABLE | Money record |
| `proposalId`, `journeyId` | ref | ✅ | INTERNAL | RET-7Y-IMMUTABLE | |
| `amount`, `currency` | money(INR) | ✅ | CONFIDENTIAL [F] | RET-7Y-IMMUTABLE | INV-PAY-03 |
| `state` | enum | ✅ | INTERNAL | RET-7Y-IMMUTABLE | §4.6 of the domain model |
| `deviceChannel` | enum | ✅ | INTERNAL | RET-7Y-IMMUTABLE | `SMS_LINK / EMAIL_LINK / QR_SCAN` — the C4 evidence field |
| `linkIssuedTo` | E.164/email | ✅ | CONFIDENTIAL [P] ⚑ | RET-7Y-IMMUTABLE | **Customer** contact; an RM contact here is a control violation |
| `linkExpiresAt` | timestamptz | ✅ | INTERNAL | RET-7Y-IMMUTABLE | |
| `pgTxnId` | string(100) | ⭘ | CONFIDENTIAL [F] | RET-7Y-IMMUTABLE | AU Bank PG reference |
| `authorisedAt`, `capturedAt` | timestamptz | ⭘ | INTERNAL | RET-7Y-IMMUTABLE | |
| `settlementRef`, `reconciledAt` | string / ts | ⭘ | CONFIDENTIAL [F] | RET-7Y-IMMUTABLE | INV-PAY-05 |
| `attempts[]` | entity list | ✅ | INTERNAL | RET-7Y-IMMUTABLE | Append-only |
| `refund` | structured | ⭘ | CONFIDENTIAL [F] | RET-7Y-IMMUTABLE | `{reason, initiatedBy, approvedBy, amount, at}` — maker-checker fields are mandatory when present |

No cardholder data appears in this model at all: the platform issues a link and receives a
reference. That is a deliberate scope boundary — PCI-relevant data never enters the platform's
trust boundary.

### 4.8 Policy — SoR: Policy & Issuance context

| Attribute | Type | Req | Cls | Ret | Notes |
|---|---|---|---|---|---|
| `policyId` | ULID | ✅ | INTERNAL | RET-POLICY+7Y | |
| `policyNumber` | string(50) | ✅ | CONFIDENTIAL | RET-POLICY+7Y | Insurer-minted; unique + immutable (INV-POL-02) |
| `proposalId`, `paymentId`, `journeyId`, `customerId` | ref | ✅ | INTERNAL | RET-POLICY+7Y | |
| `insurerCode`, `productCode` | string | ✅ | INTERNAL | RET-POLICY+7Y | |
| `sumAssured`, `premium`, `frequency` | money / enum | ✅ | CONFIDENTIAL [F] | RET-POLICY+7Y | |
| `riskCommencementDate`, `maturityDate` | date | ✅ | INTERNAL | RET-POLICY+7Y | |
| `state` | enum | ✅ | INTERNAL | RET-POLICY+7Y | §4.7 of the domain model |
| `issuedAt`, `confirmedAt` | timestamptz | ✅ | INTERNAL | RET-POLICY+7Y | Two distinct facts — see the "sold" definition |
| `documents[]` | entity list | ✅ | CONFIDENTIAL [P] | RET-7Y-IMMUTABLE | Policy PDF / COI object references |
| `freeLookExpiresAt` | timestamptz | ✅ | INTERNAL | RET-POLICY+7Y | |

### 4.9 Journey — SoR: Journey Orchestration

| Attribute | Type | Req | Cls | Ret | Notes |
|---|---|---|---|---|---|
| `journeyId` | ULID | ✅ | INTERNAL | RET-7Y | Already threaded through `integration_job.journey_id` today |
| `stage` | enum | ✅ | INTERNAL | RET-7Y | §5 of the domain model |
| `channel` | enum | ✅ | INTERNAL | RET-7Y | RM-assisted / self-service / hybrid |
| `customerId`, `rmId`, `leadId`, `lob` | ref / enum | ✅ | INTERNAL | RET-7Y | |
| `refs` | map | ✅ | INTERNAL | RET-7Y | `{suitabilityId, consentId, quoteId, offerId, proposalId, paymentId, policyId}` |
| `externalRefs` | map | ✅ | INTERNAL | RET-7Y | `{provider, applicationNumber, policyNumber}` |
| `partySnapshotRef` | ref | ✅ | INTERNAL | RET-7Y | Reference to the frozen customer snapshot — **not a copy** |
| `nextActionDueAt` | timestamptz | ✅ | INTERNAL | RET-7Y | INV-JRN-03 |
| `compensation` | structured | ⭘ | INTERNAL | RET-7Y | `{failurePoint, attempts, ownerTaskId}` |

INV-JRN-02 is visible in this table: there is no `eligibility`, `premium` or `uwDecision`
attribute. The journey holds stage and references and nothing that another context is authoritative
for.

### 4.10 Reference and master data — S06-E04-S04

| Set | SoR | Effective dating | Cache |
|---|---|---|---|
| Partner insurer master | Product Catalogue | ✅ effective-from/to | Yes, read cache |
| Product and plan catalogue | Product Catalogue | ✅ | Yes |
| Eligibility matrix (age, sum-assured, occupation bands) | Product Catalogue | ✅ | Yes |
| Branch and hierarchy | Identity & Access (WS-2) | ✅ | Yes |
| RM / SP certification | Identity & Access (WS-2) | ✅ — certification expiry is a business event, not a static attribute | Short TTL only |
| LOB, channel, occupation, relationship enumerations | Administration & Config, seeded from provider master lookup | ✅ | Yes |
| Consent statement pack | Administration & Config | ✅ versioned | Yes |
| Suitability questionnaire pack | Administration & Config | ✅ versioned | Yes |

> Enumerations are sourced from the provider master-lookup rather than hard-coded — the rule already
> stated in [`canonical-model/contexts.md`](../../1sb-insurance-integration/canonical-model/contexts.md)
> §2 ("use Master Lookup rather than hardcoding enums"), preserved platform-wide.

---

## 5. Audit data model — S06-E04-S05

The audit record must let a reader **reconstruct** a sale, not merely observe that one happened.
The existing `audit_event` table in
[`V1__init_schema.sql`](../../../services/bank-persistence-service/src/main/resources/db/migration/V1__init_schema.sql)
is a sound starting shape and is retained; four additions are required for business-journey
reconstruction.

| Field | Present today | Required | Purpose |
|---|---|---|---|
| `event_id`, `event_time`, `action`, `actor_id`, `actor_type` | ✅ | ✅ | Attribution |
| `resource_type`, `resource_id`, `outcome` | ✅ | ✅ | Subject and result |
| `lob`, `journey_id`, `distributor_id`, `agent_id` | ✅ | ✅ | Regulatory attribution — already correct |
| `trace_id` | ✅ | ✅ | Ties to logs and traces |
| `metadata` | ✅ (TEXT) | ✅ (JSONB in PostgreSQL) | Event-specific payload |
| **`prior_state` / `new_state`** | ❌ | **✅ new** | Without both, an audit log records that something changed but not *what it changed from*. Reconstruction is impossible from event lists alone |
| **`consent_ref` / `suitability_ref`** | ❌ | **✅ new** | The two references a regulator will ask for on any quote or proposal event. Deriving them by join at audit time assumes the transactional record still exists |
| **`event_schema_version`** | ❌ | **✅ new** | Audit records outlive the code that wrote them by seven years |
| **`sequence_no` per `journey_id`** | ❌ | **✅ new** | Gap detection. Without it, a *missing* audit event is undetectable, and undetectable evidence loss is the worst kind |

**Immutability implementation** (control C7, invariant INV-AUD-01): the service account holds
`INSERT` only — already the stated intent in the migration's comments — and the archive tier uses
object-lock. Immutability is proven by an automated deletion-refusal test, not by the absence of
delete code.

**Reconstruction test (S06-G7).** Take a `journeyId`; select audit events ordered by `sequence_no`;
assert the sequence is gapless and that it yields, in order: lead qualification, consent grant with
statement version, suitability outcome, quote request with the suitability reference, offer
selection with premium, proposal submission with application number, UW decision, payment link
issuance with device channel, capture, reconciliation, issuance, confirmation. If any of those
cannot be produced from audit alone, the model has failed, not the test. **This test cannot be run
today because no journey runs end to end** — recorded as OPEN-D8.

---

## 6. PII handling rules that follow from this model

| # | Rule | Derived from |
|---|---|---|
| PII-01 | Every attribute marked ⚑ is stored only in the encrypted payload store or an encrypted column with a dedicated CMK; none appears in a queryable index | INV-PRP-05, class `RESTRICTED` |
| PII-02 | No ⚑ attribute may be emitted in any log record at any level; proven by an automated log-scan test over a full suite run | C5 / INV-LOG-01 |
| PII-03 | API responses never echo customer identifiers back to a caller: `applicationNumber` and `journeyId` only — the existing 1SB rule, generalised | 1SB architecture §8.1 |
| PII-04 | Personal data is never copied between contexts; contexts hold references. The two deliberate exceptions are the consent `contactUsed` and the customer snapshot, both of which are evidence and are frozen by design | §3 |
| PII-05 | Analytical read models carry no ⚑ attribute; Reporting consumes events, and events carry no PII (§7 of the domain model) | Event payload rule |
| PII-06 | Disposal at the retention horizon writes an audit record of the disposal | S09-E06-S06 |

---

## 7. What remains open

| ID | Item | Owner | Target |
|---|---|---|---|
| OPEN-D7 | S03 attribute sheets ratified with per-attribute business validation rules (GAP-016 formal closure) | Rajal + BA | Before S11 entry |
| OPEN-I1 | Physical schema, datastore selection per context and index design | Aarti + Mahesh | S07 exit for design; S09 for implementation |
| OPEN-I2 | Tokenisation service for `aadhaarRef` — the model assumes tokenisation; no tokenisation capability exists in the repository | Deepali + Aarti | Before S11 entry |
| OPEN-I3 | Four new audit fields (§5) require a migration on `audit_event`; migration is S08/S09 work and must not be applied by this phase | Aarti + Amit | Foundation Recovery Increment |
| OPEN-I4 | Retention horizon values for `RET-7Y` classes confirmed against the final IRDAI/DPDP position (D-011 open) | Shailja | Before S11 entry |

---

**Drafted by:** Mahesh — Principal Insurance Platform Architect, for Aarti's ratification
**signature_status:** `AI-DRAFTED — mandatory human signature outstanding (Aarti AP, Mahesh AP)`
**Date:** 2026-08-16
