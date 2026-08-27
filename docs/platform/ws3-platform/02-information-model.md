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

**Revision 2026-08-20 — HLD review round R0-actors/LOB/configuration** (`SUG-20260820-hr0`):
`lob` corrected from `TERM` to `LIFE` with `productClass` separated (`LB-3`); the opportunity sheet
gains origination, accountable-SP, need-analysis and partner-visibility attributes; §4.11 adds the
versioned configuration record; the audit model gains acting capacity, partner insurer, assisted
actor and configuration version.

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
| `RET-WORKING-LEAD` | Lead terminal state + configured horizon (default 90 days) | Mutable then archived | Working inbox fields only. **Shailja C-RET-1:** attribution fields must not use this class |
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
| Workforce identity, roles, **RM SP certification state** | **Identity & Access (WS-2)** | every context | PDP decision + principal claims |
| Partner (IPR) identity, `insurerId` scope, assist-only grants | **Identity & Access (WS-2)** | every context | PDP decision + principal claims |
| Quote, offers, selection | Quotation | Journey, Proposal, Reporting | API + events |
| Proposal, UW case, requirements | Proposal & UW | Journey, Policy, Reporting | API + events |
| Payment attempts and reconciliation | Payment | Journey, Policy, Finance reporting | API + events |
| Policy record and document references | Policy & Issuance | Journey, Customer, Reporting | API + events |
| Journey stage and external references | Journey Orchestration | BFFs, Reporting | API |
| Routing policy per LOB/product | Integration Hub | — | Config |
| Provider job/correlation, raw payloads | **1SB Adapter (WS-1)** | Audit (read), Operations | Internal |
| Audit events | Audit & Compliance | Compliance, Reporting | Query API, read-only |
| Configuration, feature flags, rule packs, **all LOB-partitioned versioned config domains (`CF-2`)** | Administration & Config #19 | every context | Config resolution contract, effective-dated |

**Ambiguities resolved explicitly**

| Question | Answer |
|---|---|
| Who owns `distributorId`? | Administration & Config holds the value; **Integration Hub injects it**. No domain service and no caller may supply it (INV-DIS-01) |
| Who owns the customer's mobile number? | CBS is SoR. The Consent evidence record keeps an **immutable copy at capture time**, because consent evidence must reflect the contact used, not today's value |
| Who owns `applicationNumber`? | The insurer mints it; **Proposal** is the platform SoR for it; Journey and the adapter hold it as an external reference |
| Who owns premium amount? | Quotation owns the quoted amount; **Payment owns the amount actually charged**. INV-PAY-03 asserts they match |
| Who owns the accountable SP on a record? | The **Lead (opportunity)** context. Written once at origination from the creating RM principal and immutable thereafter (INV-ACT-03). Identity & Access remains SoR for the certification itself; the opportunity records *which* SP was accountable |
| Who owns `insurerId` on a partner principal? | **Identity & Access (WS-2)**. No business context may accept it from a request; the scoping predicate reads it from the authenticated principal (`AC-5`) |
| Who owns `lob`? | The originating **Lead (opportunity)**. Every downstream aggregate inherits it and none may change it (ID-05) |
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

### 4.2 Lead · SoR: Lead context

The single **on-platform** origination record (`AC-8`). Created only by a `BANK_RM` principal (INV-LED-04). Spoken and Product name is **Lead**. Opportunity is the durable-demand alias only (`ADR-014`). Off-platform sales do **not** create a Lead.

| Attribute | Type | Req | Cls | Ret | Notes |
|---|---|---|---|---|---|
| `leadId` | ULID | ✅ | INTERNAL | RET-7Y | Origin pointer. Carried by every on-platform downstream aggregate (INV-LED-06). **C-RET-1 — never `RET-WORKING-LEAD`** |
| `customerId` | ref | ✅ | INTERNAL | RET-7Y | ETB only in R0; must be inside the creating RM's book (INV-LED-05) |
| `state` | enum | ✅ | INTERNAL | RET-7Y | §4.1 — includes `ARCHIVED` |
| `lob` | enum | ✅ | INTERNAL | RET-7Y | **`LIFE` for R0.** One of `LIFE` \| `HEALTH` \| `GENERAL`, non-null, immutable (`LB-1`, `LB-2`). **C-RET-1** |
| `productClass` | enum | ⭘ | INTERNAL | RET-WORKING-LEAD | **`TERM` for R0.** A distinct dimension from `lob` (`LB-3`) |
| `createdByActorType` | enum | ✅ | INTERNAL | RET-7Y | Always `BANK_RM`; no other value is writable (INV-LED-04) |
| `accountableSpId` | ref | ✅ | INTERNAL | RET-7Y | The originating RM principal. Written once, immutable (INV-ACT-03). **C-RET-1** |
| `accountableSpCertRef` | structured | ✅ | INTERNAL | RET-7Y | `{certificateNumber, lobScope, validFrom, validTo}` snapshotted at origination. **C-RET-1** |
| `needAnalysisState` | enum | ✅ | INTERNAL | RET-WORKING-LEAD | `NOT_STARTED` \| `IN_PROGRESS` \| `COMPLETED`. Half of the IPR visibility predicate (`AC-4`) |
| `insurerId` | ref | ⭘ | INTERNAL | RET-WORKING-LEAD | Set when an insurer is selected; the IPR scoping key (`AC-5`) |
| `partnerVisibleFrom` | timestamptz | ⭘ | INTERNAL | RET-WORKING-LEAD | Set when `AC-4` first holds |
| `configVersions` | map | ✅ | INTERNAL | RET-7Y | Configuration versions in force at origination (INV-CFG-03) |
| `source` | enum | ✅ | INTERNAL | RET-7Y | `RM` only for Lead create. Campaign/self-service not writable (`AC-8`) |
| `assignedRmId` | ref | ⭘ | INTERNAL | RET-WORKING-LEAD | WS-2 principal |
| `assignmentHistory[]` | entity list | ✅ | INTERNAL | RET-WORKING-LEAD | `{rmId, assignedAt, assignedBy, reason}` — append-only |
| `followUps[]` | entity list | ⭘ | CONFIDENTIAL [P] | RET-WORKING-LEAD | Free-text notes may contain personal detail |
| `expiresAt` | timestamptz | ✅ | INTERNAL | RET-WORKING-LEAD | Ageing horizon; value is configuration (OPEN-D4) |
| `convertedJourneyId` | ref | ⭘ | INTERNAL | RET-7Y | Set once (INV-LED-02). **C-RET-1** |
| `archivedAt` | timestamptz | ⭘ | INTERNAL | RET-7Y | Set when the working inbox row is archived after a terminal state |

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
| `issuanceMode` | enum | ✅ | INTERNAL | RET-POLICY+7Y | `STP` \| `NON_STP` \| `INSTA`. Configuration, not a code branch. **C-ISS-1:** no mode skips C1/C2/C4/C7 |

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
| `issuanceMode` | enum | ✅ | INTERNAL | RET-POLICY+7Y | Inherited from Proposal |
| `source` | enum | ✅ | INTERNAL | RET-POLICY+7Y | `ON_PLATFORM` \| `OFF_PLATFORM`. Off-platform rows have no `leadId` and must not be counted as platform conversion |
| `leadId` | ref | ⭘ | INTERNAL | RET-POLICY+7Y | Required when `source=ON_PLATFORM`. **Absent** when `source=OFF_PLATFORM` (C-ING-1) |
| `stateHistory[]` | entity list | ✅ | INTERNAL | RET-POLICY+7Y | Append-only `{from, to, at, actorId, reason}` — the historic issuance transitions Compliance requires |

### 4.9 Journey — SoR: Journey Orchestration

| Attribute | Type | Req | Cls | Ret | Notes |
|---|---|---|---|---|---|
| `journeyId` | ULID | ✅ | INTERNAL | RET-7Y | Already threaded through `integration_job.journey_id` today |
| `stage` | enum | ✅ | INTERNAL | RET-7Y | §5 of the domain model |
| `channel` | enum | ✅ | INTERNAL | RET-7Y | RM-assisted / self-service / hybrid |
| `customerId`, `rmId`, `leadId` | ref | ✅ | INTERNAL | RET-7Y | `leadId` is the originating Lead; an on-platform journey cannot exist without it (INV-LED-06) |
| `lob` | enum | ✅ | INTERNAL | RET-7Y | Inherited from the opportunity, non-null, immutable (`LB-1`, ID-05) |
| `accountableSpId` | ref | ✅ | INTERNAL | RET-7Y | The RM. Immutable; unaffected by any partner assistance (INV-ACT-03) |
| `currentAssistingActorId`, `currentAssistingActorType` | ref / enum | ⭘ | INTERNAL | RET-7Y | Who is assisting *now* (`JS-06`). Never the accountable SP |
| `partnerVisibility` | structured | ⭘ | INTERNAL | RET-7Y | `{insurerId, visibleFrom, gatedOn}` — materialises the `AC-4` predicate for the query layer |
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
| **RM SP certification** — certificate number, LOB scope, validity window, status | Identity & Access (WS-2) | ✅ — certification expiry is a business event, not a static attribute (`AC-1`) | Short TTL only |
| **Partner (IPR) principal** — `insurerId` scope, assist-only role grants | Identity & Access (WS-2) | ✅ | Short TTL only |
| LOB, channel, occupation, relationship enumerations | Administration & Config, seeded from provider master lookup | ✅ | Yes |
| Journey step definitions and transitions | Administration & Config | ✅ versioned, `(lob, journeyType, version)` | Yes |
| Field validation rules | Administration & Config | ✅ versioned, `(lob, formId, fieldId, version)` | Yes |
| Document checklists / requirements | Administration & Config | ✅ versioned, `(lob, insurerId, productCode, version)` | Yes |
| Product eligibility rules | Administration & Config → Product Catalogue | ✅ versioned, `(lob, insurerId, productCode, version)` | Yes |
| Role → permission grants, including the IPR gate | Administration & Config → Identity & Access | ✅ versioned, `(lob, actorType, roleId, version)` | Short TTL only |
| Commission structures | Administration & Config | ✅ versioned, `(lob, insurerId, productCode, version)` — **namespace reserved in R0; no consumer until R1** | n/a |
| Consent statement pack | Administration & Config | ✅ versioned | Yes |
| Suitability questionnaire pack | Administration & Config | ✅ versioned | Yes |

> Enumerations are sourced from the provider master-lookup rather than hard-coded — the rule already
> stated in [`canonical-model/contexts.md`](../../1sb-insurance-integration/canonical-model/contexts.md)
> §2 ("use Master Lookup rather than hardcoding enums"), preserved platform-wide.

### 4.11 ConfigurationRecord — SoR: Administration & Config · **append-only, versioned**

The configuration layer ships in W0b. The administration UI is an R0 W4 consumer (`ADR-014`,
`CF-5`). This sheet is why that is safe: the store, its versioning and its resolution contract
exist before the screen; the UI does not re-platform the rules, and it must not sit on the Lead
writer (C-ISO-1).

| Attribute | Type | Req | Cls | Ret | Notes |
|---|---|---|---|---|---|
| `configId` | ULID | ✅ | INTERNAL | RET-7Y | |
| `domain` | enum | ✅ | INTERNAL | RET-7Y | One of the closed list in `CF-2` |
| `lob` | enum | ✅ | INTERNAL | RET-7Y | Non-null on **every** configuration record (`LB-1`, `LB-4`). Cross-LOB values are seeded per LOB, never as a null |
| `insurerId` | ref | ⭘ | INTERNAL | RET-7Y | Present where the domain is insurer-scoped |
| `productCode`, `journeyType`, `formId`, `fieldId`, `roleId`, `actorType` | string / enum | ⭘ | INTERNAL | RET-7Y | The rest of the domain's partition key (`CF-2`) |
| `version` | integer | ✅ | INTERNAL | RET-7Y | Monotonic per partition key. A change mints a new version; it never edits an active row (`CF-3`, INV-CFG-02) |
| `payload` | JSONB | ✅ | INTERNAL | RET-7Y | The rule, checklist, validation set or grant. Schema-validated per `domain` |
| `effectiveFrom`, `effectiveTo` | timestamptz | ✅ / ⭘ | INTERNAL | RET-7Y | Activation window. Resolution is effective-dated, so a March record remains explicable under March's rules |
| `status` | enum | ✅ | INTERNAL | RET-7Y | `DRAFT` \| `ACTIVE` \| `SUPERSEDED` \| `WITHDRAWN` |
| `checksum` | string(64) | ✅ | INTERNAL | RET-7Y | Of `payload`. Lets a seed run prove idempotence rather than assume it (`CF-4`) |
| `seedRef` | string | ⭘ | INTERNAL | RET-7Y | The source-controlled seed artefact this version came from (`CF-4`) |
| `createdBy`, `createdAt` | ref / timestamptz | ✅ | INTERNAL | RET-7Y | `createdBy` is a seed job identity in R0; a human administrator once a UI exists |

**Seeding contract.** Seeds live in the repository, are applied by the same mechanism in every
environment, and are idempotent on `(domain, partition key, version, checksum)`. A re-run is a
no-op; a changed payload at the same version is an error, not an overwrite.

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
| **`acting_capacity`** | ❌ | **✅ new** | `SP_ACCOUNTABLE` or `ASSIST_ONLY`. `actor_type` alone says *who*; only capacity says *in what right*. Without it an IPR's assistance and an RM's regulated act are indistinguishable in the record IRDAI reads (INV-ACT-04) |
| **`actor_insurer_id`** | ❌ | **✅ new** | The partner principal's insurer. Present on every `INSURER_PARTNER_REP` event; null for bank actors |
| **`assisted_actor_id`** | ❌ | **✅ new** | The accountable RM an assist action was performed alongside. Makes the solicitation trail single-threaded to one SP |
| **`config_version_ref`** | ❌ | **✅ new** | The configuration version in force for the action (`CF-3`, INV-CFG-03). A seven-year-old evidence record is unreadable without the rule it was produced under |

**Immutability implementation** (control C7, invariant INV-AUD-01): the service account holds
`INSERT` only — already the stated intent in the migration's comments — and the archive tier uses
object-lock. Immutability is proven by an automated deletion-refusal test, not by the absence of
delete code.

**Reconstruction test (S06-G7).** Take a `journeyId`; select audit events ordered by `sequence_no`;
assert the sequence is gapless and that it yields, in order: opportunity creation with its
accountable SP, need analysis completion, consent grant with
statement version, suitability outcome, quote request with the suitability reference, offer
selection with premium, proposal submission with application number, UW decision, payment link
issuance with device channel, capture, reconciliation, issuance, confirmation — **and, at every
step, the acting capacity of the actor who performed it (INV-ACT-04)**. If any of those
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
| PII-07 | A read executed for an `INSURER_PARTNER_REP` principal is constrained at the query layer by `insurer_id` **and** the `AC-4` visibility predicate. A record outside that scope is absent from the result set, never redacted in it — a redacted row still confirms the record exists | `AC-5`, INV-LED-07, `FF-17` |

---

## 7. What remains open

| ID | Item | Owner | Target |
|---|---|---|---|
| OPEN-D7 | S03 attribute sheets ratified with per-attribute business validation rules (GAP-016 formal closure) | Rajal + BA | Before S11 entry |
| OPEN-I1 | Physical schema, datastore selection per context and index design | Aarti + Mahesh | **Design drafted** in [`data-architecture/`](../data-architecture/README.md) (`DATA-001`). Human Aarti/Mahesh signature still required for S07 exit. Implementation remains S09 |
| OPEN-I2 | Tokenisation service for `aadhaarRef` — the model assumes tokenisation; no tokenisation capability exists in the repository | Deepali + Aarti | Before S11 entry |
| OPEN-I3 | Four new audit fields (§5) require a migration on `audit_event`; migration is S08/S09 work and must not be applied by this phase | Aarti + Amit | Foundation Recovery Increment — DDL drafted as [`14-audit_event_delta.sql`](../data-architecture/schemas/14-audit_event_delta.sql), not applied |
| OPEN-I4 | Retention horizon values for `RET-7Y` classes confirmed against the final IRDAI/DPDP position (D-011 open) | Shailja | Before S11 entry |
| OPEN-I5 | Four further audit fields (`acting_capacity`, `actor_insurer_id`, `assisted_actor_id`, `config_version_ref`) join OPEN-I3's `audit_event` migration. Same migration, same S08/S09 window — not applied by this phase | Aarti + Amit | Foundation Recovery Increment — same delta script as OPEN-I3 |
| OPEN-I6 | Physical partitioning strategy for the LOB dimension: whether `lob` is a partition key, an index prefix or both, per store. `LB-1` fixes the *logical* dimension; the physical choice is Aarti's | Aarti | **R0 design decision:** index prefix, not a partition key — [`01-physical-design.md` §5](../data-architecture/01-physical-design.md#5-open-i6--lob-partitioning). Revisit when a second LOB is admitted or a table shows measured mixed-LOB cost |

---

**Drafted by:** Mahesh — Principal Insurance Platform Architect, for Aarti's ratification
**signature_status:** `AI-DRAFTED — mandatory human signature outstanding (Aarti AP, Mahesh AP)`
**Date:** 2026-08-16 · **revised** 2026-08-20 (HLD review round — actors, LOB, configuration)
