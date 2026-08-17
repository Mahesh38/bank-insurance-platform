# WS-3 — Platform Domain Model, State Machines and Invariants

**Workstream:** WS-3 — AU Bank Insurance Distribution Platform (proposed under CR-010)
**Stage:** S06 — Domain & Information Architecture
**Owner:** Mahesh — Principal Insurance Platform Architect (Board 1)
**Co-owner for the logical data model:** Aarti — Principal Insurance Data & Database Architect
**Status:** AI-DRAFTED architecture baseline. Board 1 verdict is recorded in
[`../../governance/change-requests/CR-010/verdicts/board-1-architecture-mahesh.md`](../../governance/change-requests/CR-010/verdicts/board-1-architecture-mahesh.md).
Mandatory human Architecture and Data signatures are outstanding.

**Companion documents**

| Document | Content |
|---|---|
| [`02-information-model.md`](./02-information-model.md) | Canonical entities, attributes, types, classification, retention, system of record |
| [`03-solution-architecture-r0.md`](./03-solution-architecture-r0.md) | Component/deployment view and seam semantics for the R0 slice |
| [`00-WS3-ARCHITECTURE-REGISTRATION.md`](./00-WS3-ARCHITECTURE-REGISTRATION.md) | Workstream registration, re-parenting and standing constraints |

---

## 1. Why this document exists

[`stages/S06-domain-architecture.md §6`](../../application-lifecycle-bible/stages/S06-domain-architecture.md)
records the S06 position as 🟡 Partial with five named holes: context relationships, platform
aggregates, the journey saga, platform-wide invariants, and the data ownership matrix. It says
plainly that *"the saga gap is the important one"* because payment-succeeded-but-issuance-failed is
routine in bancassurance.

This document closes the conceptual half of that gap. It is deliberately written so that an
engineer can implement directly from it: every state machine enumerates its legal transitions,
every invariant is phrased as an assertion a test can make, and every invariant names the component
that enforces it and the behaviour on violation.

It does **not** decide physical schema, datastore technology, indexing or messaging technology —
those are S07/S09 concerns and are recorded in [`03-solution-architecture-r0.md`](./03-solution-architecture-r0.md).

---

## 2. Bounded context map — relationships, not just a list

The 19 contexts are enumerated in
[`business-problem-statement.md §6`](../../context/business-problem-statement.md) and
[`architecture-review/02-target-microservices-architecture.md`](../architecture-review/02-target-microservices-architecture.md).
Those are a *target state*. What was missing — S06-E01-S02 — is the **relationship** between them.
This section supplies it for the contexts the R0 journey needs.

Relationship vocabulary used below is the standard strategic-DDD set:
**U/D** upstream–downstream · **ACL** anti-corruption layer at the downstream side ·
**CF** conformist (downstream accepts upstream's model) ·
**OHS** open host service (upstream publishes a stable contract) ·
**PL** published language · **SK** shared kernel.

```mermaid
graph LR
    subgraph Edge["Edge (no domain state)"]
        RMBFF["RM Workspace BFF #2"]
        CBFF["Customer BFF #1"]
    end

    subgraph Sales["Sales & advisory"]
        LEAD["Lead #5"]
        CUST["Customer #4"]
        CONS["Consent #6"]
        SUIT["Suitability #7"]
        CAT["Product Catalogue #8"]
        QTE["Quotation #10"]
        PRP["Proposal & UW #11"]
    end

    JRN["Journey Orchestration #9<br/>saga owner"]

    subgraph Fulfil["Fulfilment"]
        PAY["Payment #12"]
        POL["Policy & Issuance #13"]
    end

    subgraph Supply["Supplier edge — WS-1"]
        HUB["Integration Hub #14"]
        ONESB["1SB Adapter #15<br/>existing service"]
    end

    IDN["Identity & Access #3<br/>WS-2 enabler"]
    AUD["Audit & Compliance #16"]

    RMBFF -->|"U/D, CF"| JRN
    CBFF -->|"U/D, CF"| JRN
    RMBFF -->|"U/D"| IDN
    CBFF -->|"U/D"| IDN

    JRN -->|"U/D, OHS"| LEAD
    JRN -->|"U/D, OHS"| CUST
    JRN -->|"U/D, OHS"| CONS
    JRN -->|"U/D, OHS"| SUIT
    JRN -->|"U/D, OHS"| QTE
    JRN -->|"U/D, OHS"| PRP
    JRN -->|"U/D, OHS"| PAY
    JRN -->|"U/D, OHS"| POL
    QTE -->|"U/D"| CAT
    SUIT -->|"U/D"| CAT

    QTE -->|"U/D, ACL"| HUB
    PRP -->|"U/D, ACL"| HUB
    PAY -->|"U/D, ACL"| HUB
    POL -->|"U/D, ACL"| HUB
    HUB -->|"U/D, ACL"| ONESB

    JRN -.->|"PL: domain events"| AUD
    PAY -.->|"PL: domain events"| AUD
    POL -.->|"PL: domain events"| AUD
```

### 2.1 Relationship register

| # | Upstream | Downstream | Relationship | Why, and what crosses the seam |
|---|---|---|---|---|
| R-01 | Journey Orchestration | RM/Customer BFF | OHS + CF | The BFF conforms to the journey's published stage model. A BFF that reimplements journey state re-creates ARCH-005's failure mode |
| R-02 | Journey Orchestration | Lead, Customer, Consent, Suitability, Quotation, Proposal, Payment, Policy | U/D, OHS | Journey holds only references and stage; it never holds a copy of another context's authoritative state |
| R-03 | Suitability | Product Catalogue | U/D, CF | Suitability reads eligibility rules; it does not own product definitions |
| R-04 | Quotation | Product Catalogue | U/D, CF | Quote validates product/insurer availability before fan-out |
| R-05 | Quotation, Proposal, Payment, Policy | Integration Hub | U/D, **ACL** | Domain services speak bank-canonical only. Hub owns routing; provider shapes stop at the adapter |
| R-06 | Integration Hub | 1SB Adapter | U/D, **ACL** | Existing ArchUnit rule confines 1SB types to `adapter.onesb.*`. The Hub generalises the same rule per adapter |
| R-07 | Identity & Access (WS-2) | every context | U/D, OHS | Contexts consume an authenticated principal and a PDP decision; none re-derives entitlements |
| R-08 | every context | Audit & Compliance | PL (published language: domain events) | Audit consumes a versioned event language. Producers never write to the audit store directly |
| R-09 | Consent | Suitability, Quotation, Proposal | U/D, OHS | A consent reference is a value passed downstream; consent evidence itself is never copied |
| R-10 | Payment | Policy & Issuance | U/D via Journey saga | **Deliberately not direct.** Coupling payment to issuance directly is what makes the paid-but-not-issued failure unrecoverable |

### 2.2 Language boundary — where the same word means different things

S06-E01-S03. These four are the ones that actually bite.

| Word | In context A | In context B | Translation point |
|---|---|---|---|
| **Status** | Proposal: `UNDER_WRITING`, insurer-driven | 1SB Adapter: `BankApplicationStatus` normalised from the provider string (`domain/model/BankApplicationStatus.java`) | 1SB Adapter normalises; Integration Hub passes the normalised value; Proposal maps it onto its own aggregate state |
| **Quote** | Quotation: an aggregate with offers and a selection | 1SB Adapter: a `QuoteJob` — an async correlation record (`domain/model/QuoteJob.java`) | Quotation owns the business aggregate; the adapter's job id is an external reference only |
| **Customer** | Customer context: CIF-linked profile snapshot | Proposal: proposer/life-assured member roles | Party snapshot taken at proposal start; the proposal never re-reads the live profile mid-case |
| **Payment** | Payment context: an attempt with reconciliation | Policy: the precondition for issuance | Journey saga mediates; see [§5](#5-the-journey-saga) |

### 2.3 Why each context exists — the distinct-reason-to-change test

S06-E01-S04 and S06-VT-01. A context that always changes with another is not a context.

| Context | Its one reason to change |
|---|---|
| Lead | Bank sales-management policy: assignment, ageing, campaign |
| Customer | CBS contract and ETB profile semantics |
| Consent | Regulatory consent wording, sequencing and evidence obligations (IRDAI/DPDP) |
| Suitability | Need-analysis methodology and the IRDAI suitability obligation |
| Product Catalogue | Insurer product/eligibility matrix and effective dating |
| Journey Orchestration | The shape of the sale process itself |
| Quotation | Multi-insurer price discovery and comparison semantics |
| Proposal & UW | Insurer application capture and underwriting interaction |
| Payment | Money movement, the bank PG contract, and reconciliation |
| Policy & Issuance | Issued-policy record and document custody |
| Integration Hub | Which provider serves which LOB/product |
| 1SB Adapter | The 1SB API contract |
| Audit & Compliance | Regulatory evidence obligations |

**Contexts I deliberately did not create.** No "Journey Read Model" context, no "Rules Engine"
context, no "Document" context for R0. Each would be a noun without an independent reason to
change, and AP-01 forbids promoting a noun to a boundary.

---

## 3. Aggregates and consistency boundaries

S06-E02-S01 and validation test S06-VT-02: *walk each business transaction; count the aggregates it
must update atomically; never more than one.*

| Aggregate | Context | Root identity | Transactional consistency boundary | Deliberately outside the boundary |
|---|---|---|---|---|
| `Lead` | Lead | `leadId` | Lead + its assignment history + follow-ups | Customer profile, journeys spawned from it |
| `CustomerProfile` | Customer | `customerId` (bank CIF ref) | Profile snapshot + contact set | CBS record itself (external SoR) |
| `Consent` | Consent | `consentId` | Consent grant + its immutable evidence record | Everything else; consent is referenced, never copied |
| `SuitabilityAssessment` | Suitability | `suitabilityId` | Assessment + answers + outcome + override record | Product catalogue entries |
| `Quote` | Quotation | `quoteId` | Quote + all its `Offer` entities + the selection | The adapter's `QuoteJob`; insurer-side quote identifiers |
| `Proposal` | Proposal & UW | `proposalId` | Proposal + form values reference + `UnderwritingCase` + requirements | Payment, Policy, insurer decision record beyond its normalised status |
| `Payment` | Payment | `paymentId` | Payment + its attempts + reconciliation record | Proposal, Policy |
| `Policy` | Policy & Issuance | `policyId` | Policy + issued document references + confirmation record | Payment, Proposal |
| `Journey` | Journey Orchestration | `journeyId` | Journey stage + external references + party snapshot | Every other aggregate's internal state |
| `AuditEvent` | Audit & Compliance | `eventId` | A single event — append-only, never updated | Everything |

**`UnderwritingCase` is an entity inside `Proposal`, not its own aggregate.** It has no lifecycle
independent of the proposal it underwrites, and making it an aggregate would force a distributed
transaction on every requirement update. It becomes a candidate aggregate only if the business
begins servicing underwriting cases that outlive their proposal — recorded as the revisit trigger.

**`Offer` is an entity inside `Quote`.** Selecting an offer must be atomic with invalidating the
alternatives (INV-QUO-05); splitting `Offer` out would make that a cross-aggregate transaction on
the busiest path in the system.

### 3.1 Identity and referencing

S06-E02-S04.

| Rule | Statement |
|---|---|
| ID-01 | Every aggregate root carries a bank-minted, opaque, non-sequential identifier. Provider identifiers are never aggregate identity |
| ID-02 | Cross-context references are by root identifier plus context, never by embedded object graph |
| ID-03 | Provider identifiers (`reqId`, `applicationNumber`, insurer policy number) live in an `externalRefs` map on the owning aggregate and on `Journey`, and are never returned to a bank caller as the primary handle — the existing 1SB rule (`reqId` is not surfaced) generalised |
| ID-04 | An aggregate is archived, never deleted, until its retention horizon expires; disposal produces an audit record (see [`02-information-model.md`](./02-information-model.md)) |

---

## 4. State machines

Notation: every diagram enumerates the **legal** transitions. Any transition not drawn is illegal
and must be rejected by the aggregate, not by the caller. Terminal states are marked `[*]` targets.

### 4.1 Lead

```mermaid
stateDiagram-v2
    [*] --> NEW
    NEW --> ASSIGNED: assign(rmId)
    NEW --> DISQUALIFIED: disqualify(reason)
    ASSIGNED --> CONTACTED: recordContact()
    ASSIGNED --> ASSIGNED: reassign(rmId)
    ASSIGNED --> EXPIRED: ageOut()
    CONTACTED --> QUALIFIED: qualify()
    CONTACTED --> DISQUALIFIED: disqualify(reason)
    CONTACTED --> EXPIRED: ageOut()
    QUALIFIED --> CONVERTED: journeyReachedSold(journeyId)
    QUALIFIED --> DISQUALIFIED: disqualify(reason)
    QUALIFIED --> EXPIRED: ageOut()
    CONVERTED --> [*]
    DISQUALIFIED --> [*]
    EXPIRED --> [*]
```

| Transition | Trigger | Guard |
|---|---|---|
| `NEW → ASSIGNED` | RM assignment or auto-allocation | Target RM holds a valid SP certificate (INV-LED-03) |
| `ASSIGNED → ASSIGNED` | Reassignment | Previous owner retained in assignment history; SLA restart is a Product decision, recorded as OPEN-D1 |
| `* → EXPIRED` | Ageing job | Configurable ageing horizon; no journey in a non-terminal stage references this lead |
| `QUALIFIED → CONVERTED` | `JourneySold` event | Exactly one journey may convert a lead (INV-LED-02) |

### 4.2 Consent

Consent is modelled as a **grant with immutable evidence**, not as a mutable record. Withdrawal
creates a new state on the grant; it never edits the captured evidence.

```mermaid
stateDiagram-v2
    [*] --> REQUESTED
    REQUESTED --> OTP_PENDING: challenge()
    OTP_PENDING --> GRANTED: verifyOtp(txnId)
    OTP_PENDING --> ABANDONED: timeout()
    REQUESTED --> ABANDONED: timeout()
    GRANTED --> WITHDRAWN: withdraw(actor, reason)
    GRANTED --> EXPIRED: validityElapsed()
    WITHDRAWN --> [*]
    EXPIRED --> [*]
    ABANDONED --> [*]
```

`GRANTED` is the only state that satisfies a downstream consent precondition. There is no
`GRANTED → GRANTED` re-affirmation: a re-consent produces a **new** `Consent` aggregate carrying a
new statement version, which is what makes versioned evidence auditable.

### 4.3 Suitability assessment

```mermaid
stateDiagram-v2
    [*] --> IN_PROGRESS
    IN_PROGRESS --> COMPLETED: evaluate()
    IN_PROGRESS --> ABANDONED: timeout()
    COMPLETED --> SUPERSEDED: newAssessmentForSameCustomerAndLob()
    COMPLETED --> EXPIRED: validityElapsed()
    COMPLETED --> OVERRIDDEN: override(actor, reason)
    OVERRIDDEN --> SUPERSEDED: newAssessmentForSameCustomerAndLob()
    OVERRIDDEN --> EXPIRED: validityElapsed()
    SUPERSEDED --> [*]
    EXPIRED --> [*]
    ABANDONED --> [*]
```

`COMPLETED` carries an `outcome` of `ELIGIBLE | NOT_ELIGIBLE` and a recommended product set.
`OVERRIDDEN` exists because the business anticipated an override path
([`knowledge-base/07-information-model-and-rules.md`](../../au-bank-insurance-platform/knowledge-base/07-information-model-and-rules.md)
lists "Suitability — eligibility, recommendation, override, versioning"). **Who may override, and
whether an override may unblock quote, is a Compliance and Product decision, not an architecture
one** — recorded as OPEN-D2 and blocked behind GAP-007.

### 4.4 Quote

```mermaid
stateDiagram-v2
    [*] --> REQUESTED
    REQUESTED --> IN_PROGRESS: dispatchToHub()
    REQUESTED --> REJECTED: preconditionFailed()
    IN_PROGRESS --> QUOTED: allInsurersResponded()
    IN_PROGRESS --> PARTIALLY_QUOTED: someInsurersResponded()
    IN_PROGRESS --> FAILED: noInsurerResponded()
    IN_PROGRESS --> TIMED_OUT: pollBudgetExhausted()
    QUOTED --> SELECTED: selectOffer(offerId)
    PARTIALLY_QUOTED --> SELECTED: selectOffer(offerId)
    QUOTED --> EXPIRED: validityElapsed()
    PARTIALLY_QUOTED --> EXPIRED: validityElapsed()
    SELECTED --> CONVERTED: proposalCreated(proposalId)
    SELECTED --> EXPIRED: validityElapsed()
    CONVERTED --> [*]
    EXPIRED --> [*]
    FAILED --> [*]
    TIMED_OUT --> [*]
    REJECTED --> [*]
```

`PARTIALLY_QUOTED` is a first-class success state, not a degraded one — it carries forward the
existing and correct 1SB rule *"partial success is success"*
([`1sb-integration-service-architecture.md §1`](../../1sb-insurance-integration/architecture/1sb-integration-service-architecture.md)).
`REJECTED` is the state a quote enters when the suitability hard-gate refuses it; it exists so the
refusal is auditable rather than a bare HTTP 403 with no domain record.

### 4.5 Proposal

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> SUBMITTED: submit()
    DRAFT --> ABANDONED: timeout()
    SUBMITTED --> UNDER_WRITING: insurerAcknowledged(applicationNumber)
    SUBMITTED --> SUBMISSION_FAILED: insurerRejectedSubmission()
    UNDER_WRITING --> REQUIREMENTS_PENDING: requirementsRaised()
    REQUIREMENTS_PENDING --> UNDER_WRITING: requirementsSatisfied()
    REQUIREMENTS_PENDING --> EXPIRED: requirementSlaElapsed()
    UNDER_WRITING --> UW_APPROVED: insurerApproved()
    UNDER_WRITING --> UW_COUNTER_OFFER: insurerCounterOffered()
    UNDER_WRITING --> UW_DECLINED: insurerDeclined()
    UW_COUNTER_OFFER --> UW_APPROVED: customerAccepted()
    UW_COUNTER_OFFER --> CUSTOMER_DECLINED: customerRejected()
    UW_APPROVED --> AWAITING_PAYMENT: paymentRequested()
    AWAITING_PAYMENT --> PAID: paymentReconciled(paymentId)
    AWAITING_PAYMENT --> EXPIRED: paymentWindowElapsed()
    PAID --> CONVERTED: policyIssued(policyId)
    PAID --> ISSUANCE_FAILED: issuanceRejected()
    ISSUANCE_FAILED --> CONVERTED: issuanceRetrySucceeded()
    ISSUANCE_FAILED --> REFUND_REQUIRED: issuanceAbandoned()
    DRAFT --> WITHDRAWN: withdraw()
    SUBMITTED --> WITHDRAWN: withdraw()
    UNDER_WRITING --> WITHDRAWN: withdraw()
    CONVERTED --> [*]
    UW_DECLINED --> [*]
    CUSTOMER_DECLINED --> [*]
    WITHDRAWN --> [*]
    EXPIRED --> [*]
    ABANDONED --> [*]
    SUBMISSION_FAILED --> [*]
    REFUND_REQUIRED --> [*]
```

`ISSUANCE_FAILED` and `REFUND_REQUIRED` are the states the S06 stage file says are missing and
expensive. `PAID → ISSUANCE_FAILED` is the *paid-but-not-issued* case; it is a **non-terminal**
state with two defined exits, which is precisely what makes the money recoverable.
`WITHDRAWN` is not permitted after `AWAITING_PAYMENT` — once a payment window is open, exit is via
expiry or refund, never via a silent withdrawal that strands a paid premium.

### 4.6 Payment

Payment carries the C4 device-isolation control, so the state machine makes the device binding
explicit rather than leaving it to the UI.

```mermaid
stateDiagram-v2
    [*] --> INITIATED
    INITIATED --> LINK_ISSUED: issueCustomerDeviceLink()
    INITIATED --> REJECTED: preconditionFailed()
    LINK_ISSUED --> AWAITING_AUTHORISATION: customerDeviceOpenedLink()
    LINK_ISSUED --> EXPIRED: linkTtlElapsed()
    AWAITING_AUTHORISATION --> AUTHORISED: pgAuthorised(pgTxnId)
    AWAITING_AUTHORISATION --> DECLINED: pgDeclined(reason)
    AWAITING_AUTHORISATION --> EXPIRED: sessionTtlElapsed()
    AUTHORISED --> CAPTURED: pgCaptured()
    AUTHORISED --> UNCERTAIN: pgResponseLost()
    UNCERTAIN --> CAPTURED: reconciliationConfirmedCapture()
    UNCERTAIN --> DECLINED: reconciliationConfirmedNoCapture()
    CAPTURED --> RECONCILED: matchedAgainstPgSettlement()
    CAPTURED --> RECONCILIATION_BREAK: unmatchedAfterSlaWindow()
    RECONCILIATION_BREAK --> RECONCILED: manualReconciliationCompleted()
    RECONCILED --> REFUND_INITIATED: refundRequested(reason)
    REFUND_INITIATED --> REFUNDED: pgRefundConfirmed()
    RECONCILED --> [*]
    REFUNDED --> [*]
    DECLINED --> [*]
    EXPIRED --> [*]
    REJECTED --> [*]
```

`UNCERTAIN` is mandatory, not defensive engineering. Every payment integration eventually loses a
response, and a system with no `UNCERTAIN` state resolves that ambiguity by guessing. Note also
that `RECONCILED` — not `CAPTURED` — is the state that permits issuance, which is what makes the
four-part "sold" definition in
[`R0-SCOPE.md §2`](../../au-bank-insurance-platform/requirements/R0-SCOPE.md) enforceable rather
than aspirational.

### 4.7 Policy

```mermaid
stateDiagram-v2
    [*] --> PENDING_ISSUANCE
    PENDING_ISSUANCE --> ISSUED: insurerIssued(policyNumber)
    PENDING_ISSUANCE --> ISSUANCE_REJECTED: insurerRejected(reason)
    ISSUED --> CONFIRMED: issuanceConfirmedByApi()
    ISSUED --> CONFIRMATION_OVERDUE: confirmationSlaElapsed()
    CONFIRMATION_OVERDUE --> CONFIRMED: confirmationReceivedLate()
    CONFIRMATION_OVERDUE --> ISSUANCE_DISPUTED: escalated()
    CONFIRMED --> ACTIVE: documentsPersistedAndAudited()
    ACTIVE --> FREE_LOOK_CANCELLED: freeLookExercised()
    ACTIVE --> LAPSED: renewalMissed()
    ACTIVE --> SURRENDERED: surrendered()
    ACTIVE --> MATURED: termCompleted()
    ISSUANCE_REJECTED --> [*]
    ISSUANCE_DISPUTED --> [*]
    FREE_LOOK_CANCELLED --> [*]
    LAPSED --> [*]
    SURRENDERED --> [*]
    MATURED --> [*]
```

**R0 scope note.** R0 delivers `PENDING_ISSUANCE → ISSUED → CONFIRMED → ACTIVE` plus
`ISSUANCE_REJECTED` and `FREE_LOOK_CANCELLED`. `LAPSED`, `SURRENDERED` and `MATURED` are modelled
now because omitting them would let the R0 implementation treat `ACTIVE` as terminal and hard-code
that assumption into the schema; they are not implemented in R0. This is the one place in this
document where I model beyond the R0 slice, and the reason is that the cost of the omission is a
migration, not a feature.

`ACTIVE` requires *documents persisted and audited* — the fourth limb of the "sold" definition. A
policy that the insurer has issued but whose evidence the bank cannot produce is not `ACTIVE` here.

---

## 5. The journey saga

S06-E02-S05 and S06-VT-05 — the artefact the stage file says does not exist.

The saga is **orchestrated**, not choreographed. Journey Orchestration owns it. The reason is
AP-05 and a specific regulatory one: an orchestrator produces a single, queryable, attributable
record of where a sale is and why it stopped, which is exactly what the legacy AU Beema Portal
cannot produce and what the platform was funded to provide. A choreographed event chain distributes
that record across ten services and reconstructs it only by log archaeology.

```mermaid
stateDiagram-v2
    [*] --> INITIATED
    INITIATED --> NEED_ANALYSIS: leadQualified()
    NEED_ANALYSIS --> SUITABILITY_COMPLETE: suitabilityCompleted(suitabilityId)
    SUITABILITY_COMPLETE --> CONSENT_CAPTURED: consentGranted(consentId)
    CONSENT_CAPTURED --> QUOTING: quoteRequested(quoteId)
    QUOTING --> QUOTE_SELECTED: offerSelected(offerId)
    QUOTE_SELECTED --> PROPOSAL_IN_PROGRESS: proposalCreated(proposalId)
    PROPOSAL_IN_PROGRESS --> UNDERWRITING: proposalSubmitted()
    UNDERWRITING --> PAYMENT_PENDING: uwApproved()
    PAYMENT_PENDING --> PAYMENT_SETTLED: paymentReconciled(paymentId)
    PAYMENT_SETTLED --> ISSUANCE_PENDING: issuanceRequested()
    ISSUANCE_PENDING --> ISSUED: policyIssued(policyId)
    ISSUED --> SOLD: confirmedReconciledAndAudited()
    SOLD --> [*]

    NEED_ANALYSIS --> ABANDONED: inactivityTimeout()
    SUITABILITY_COMPLETE --> ABANDONED: inactivityTimeout()
    CONSENT_CAPTURED --> ABANDONED: inactivityTimeout()
    QUOTING --> ABANDONED: quoteFailedOrExpired()
    QUOTE_SELECTED --> ABANDONED: quoteExpired()
    PROPOSAL_IN_PROGRESS --> ABANDONED: inactivityTimeout()
    UNDERWRITING --> DECLINED: uwDeclinedOrCustomerDeclined()
    PAYMENT_PENDING --> ABANDONED: paymentWindowExpired()
    PAYMENT_SETTLED --> COMPENSATING: issuanceFailed()
    ISSUANCE_PENDING --> COMPENSATING: issuanceFailed()
    ISSUED --> COMPENSATING: confirmationOrReconciliationBroke()
    COMPENSATING --> SOLD: recoverySucceeded()
    COMPENSATING --> COMPENSATED: refundCompleted()
    COMPENSATING --> MANUAL_INTERVENTION: automaticRecoveryExhausted()
    MANUAL_INTERVENTION --> SOLD: opsResolvedForward()
    MANUAL_INTERVENTION --> COMPENSATED: opsResolvedBackward()
    ABANDONED --> [*]
    DECLINED --> [*]
    COMPENSATED --> [*]
```

### 5.1 Failure and compensation matrix

S06-VT-05 pass condition: *every failure has a defined compensation or a defined manual procedure*.

| # | Failure point | Business consequence | Compensation | Automatic? | Owner of the manual path |
|---|---|---|---|---|---|
| F-01 | Suitability service unavailable at quote | RM cannot quote | None. **Fail closed** — quote is refused (C1 is non-waivable). Journey holds at `SUITABILITY_COMPLETE` pending | n/a | — |
| F-02 | Consent captured, journey then abandoned | Consent evidence exists for a sale that never happened | Consent grant remains valid until its own expiry; journey records `ABANDONED` with the consent reference retained for the retention horizon. **No deletion** | Yes | — |
| F-03 | Quote expires mid-proposal | Priced offer no longer valid | Proposal held in `DRAFT`; journey returns to `QUOTING` for a re-quote. Previously captured proposal values are retained and re-applied | Yes | — |
| F-04 | Proposal submitted, insurer never acknowledges | Unknown whether the insurer has the application | Poll by external reference until the poll budget is exhausted, then `SUBMISSION_FAILED` and an operations task. **Never re-submit blind** — the existing 1SB no-retry-on-submit rule | Partial | Operations (Shivanshi runbook) |
| F-05 | **Payment captured, issuance fails** | Customer has paid for no policy | Journey → `COMPENSATING`. Bounded issuance retry; if exhausted, `Proposal → REFUND_REQUIRED` and `Payment → REFUND_INITIATED`. Refund is a **money movement**, so it is maker-checked and never auto-executed above the configured threshold | Retry yes; refund no | Operations + Finance |
| F-06 | **Issued, confirmation never received** | Bank cannot prove the sale | `Policy → CONFIRMATION_OVERDUE`, then reconciliation against the insurer status API. If still absent → `ISSUANCE_DISPUTED` | Partial | Operations + insurer escalation |
| F-07 | **Payment captured, reconciliation break** | Money received that cannot be matched | `Payment → RECONCILIATION_BREAK`. Journey stays out of `SOLD`. Manual reconciliation is the defined procedure; **no timeout auto-resolves a money break** | No | Finance |
| F-08 | PG response lost after authorisation | Duplicate-charge risk | `Payment → UNCERTAIN`; the next reconciliation cycle resolves it. Re-initiation is blocked while `UNCERTAIN` (INV-PAY-04) | Yes | — |
| F-09 | Integration Hub cannot route (no adapter for product) | Quote or proposal cannot proceed | Fail fast with a routing error; journey stays at its current stage. Product decides fallback to Group B redirect | Yes | Product |
| F-10 | Audit event emission fails | Regulated action with no evidence | Producer writes to a durable local outbox and retries. **The business transaction is not rolled back**, but the journey cannot reach `SOLD` until audit persistence is confirmed (INV-JRN-05) | Yes | — |

> **The rule embedded in F-05, F-07 and F-10:** nothing on the money or evidence path is resolved by
> a timeout. Timeouts resolve *availability* questions; they must never resolve *financial
> correctness* questions. That is the same principle
> [`08-SRE-READINESS-CANON.md`](../../application-lifecycle-bible/08-SRE-READINESS-CANON.md) states
> from the operations side.

### 5.2 Cross-aggregate consistency decisions

S06-E03-S04: where eventual consistency is acceptable, and where it is not.

| Interaction | Consistency | Justification |
|---|---|---|
| Offer selection ↔ invalidating sibling offers | **Strong** (single aggregate) | Two selected offers on one quote is a mis-sale |
| Payment attempt ↔ payment aggregate state | **Strong** | Money |
| Payment `RECONCILED` ↔ Proposal `PAID` | **Eventual, bounded, monitored** | Cross-context. Bound: reconciliation SLA (NFR-DAT-03). A breach is a `RECONCILIATION_BREAK`, not a silent lag |
| Journey stage ↔ owning aggregate state | **Eventual** | Journey is a projection of references; it is never the authority. INV-JRN-02 forbids reading business decisions from the journey |
| Suitability outcome ↔ quote precondition | **Strong at the point of use** | The quote service re-validates the assessment at request time; it does not trust a cached flag on the journey |
| Consent grant ↔ proposal submission precondition | **Strong at the point of use** | Same reason. C2 is non-waivable |
| Domain event ↔ audit record | **Eventual via outbox, at-least-once** | Duplicate audit records are acceptable and de-duplicable; missing ones are not |

---

## 6. Invariants

Format: `INV-<CTX>-<nn>`. Every row states the assertion in testable terms, the component that
enforces it, and what happens when it is violated (S06-E03-S03: *per invariant, not generically*).
`Control` links the invariant to the non-waivable control catalogue in
[`07-SECURITY-COMPLIANCE-CANON.md §3`](../../application-lifecycle-bible/07-SECURITY-COMPLIANCE-CANON.md).

### 6.1 Compliance hard-gates (S06-E03-S02)

| ID | Assertion | Enforced at | On violation | Control |
|---|---|---|---|---|
| INV-QUO-01 | A `Quote` may leave `REQUESTED` only if a `SuitabilityAssessment` exists for the same `customerId` **and** `lob`, is in `COMPLETED` or `OVERRIDDEN`, has `outcome = ELIGIBLE`, and is not `EXPIRED` at the instant of the check | `QuotationService.request()` — server side, before any Hub call | Reject with `403 SUITABILITY_REQUIRED`; persist `Quote` in `REJECTED`; emit `QUOTE_REJECTED_NO_SUITABILITY` | **C1** |
| INV-PRP-01 | A `Proposal` may leave `DRAFT` only if a `Consent` in `GRANTED` exists for the same `customerId`, covering the required purpose set, unexpired at submit time | `ProposalService.submit()` | Reject with `403 CONSENT_REQUIRED`; proposal stays `DRAFT`; emit `PROPOSAL_BLOCKED_NO_CONSENT` | **C2** |
| INV-DIS-01 | `distributorId` and the SP licence identifier on any outbound provider request are resolved from server-side configuration and the authenticated principal. A caller-supplied value in the request body is rejected, never ignored | Integration Hub, before adapter dispatch | Reject with `422 ATTRIBUTION_NOT_CALLER_SUPPLIED`; emit a security event | **C3** |
| INV-PAY-01 | A payment link is bound to a customer-device channel (SMS/email to the customer's registered contact, or a QR presented for scan). No API path issues a payment link into an RM session, and no RM principal may complete authorisation | `PaymentService.issueLink()` and the PG redirect handler | Reject with `403 PAYMENT_DEVICE_ISOLATION`; emit a security event | **C4** |
| INV-LOG-01 | No log record at any level contains a value matching the regulated field patterns (PAN, Aadhaar, mobile, email, DOB, income, health answer) | Logging framework converter + a CI log-scan test | Build fails; at runtime the converter masks | **C5** |
| INV-DAT-01 | Every persisted store, backup, log destination and archive resolves to an AWS India region | IaC policy check pre-apply + a residency attestation job | `terraform apply` blocked; running drift raises an O0 | **C6** |
| INV-AUD-01 | Every audit record and raw payload is written to an immutable store with a `retain_until` no earlier than event time + 7 years, and no code path performs `UPDATE` or `DELETE` on it | Store-level permission (INSERT-only role) + Object Lock + an immutability test | Write rejected at the database/object-store layer, not by application logic | **C7** |
| INV-AUD-02 | Every state transition on every aggregate in §4 emits exactly one audit event carrying actor, actor type, resource, outcome, `journeyId`, `distributorId`, `agentId`, `traceId` | Aggregate transition hook + a per-journey completeness test | Transition is committed; event is written via outbox and retried. Journey cannot reach `SOLD` (INV-JRN-05) | **C8** |

### 6.2 Aggregate invariants

| ID | Assertion | Enforced at | On violation |
|---|---|---|---|
| INV-LED-01 | A `Lead` in a terminal state accepts no further transitions | `Lead` aggregate | `409 ILLEGAL_TRANSITION` |
| INV-LED-02 | At most one `Journey` may drive a `Lead` to `CONVERTED` | `Lead` aggregate, on the conversion event | Second event is idempotently ignored; a differing `journeyId` raises an integrity alert |
| INV-LED-03 | A `Lead` may only be assigned to a principal holding a currently valid SP certification for the LOB | Lead assignment, reading WS-2 certification metadata | `422 RM_NOT_CERTIFIED` |
| INV-CNS-01 | A `Consent` evidence record is write-once. Statement text, statement version, CIF, OTP transaction id, timestamp and source IP are all mandatory and immutable | Consent aggregate + INSERT-only store | Write rejected at the store |
| INV-CNS-02 | A `Consent` may reach `GRANTED` only from `OTP_PENDING` with a verified OTP transaction id | Consent aggregate | `409 ILLEGAL_TRANSITION` |
| INV-SUI-01 | A `SuitabilityAssessment` in `COMPLETED` is immutable. A correction produces a new assessment and marks the old one `SUPERSEDED` | Suitability aggregate | `409 ASSESSMENT_IMMUTABLE` |
| INV-SUI-02 | An `OVERRIDDEN` assessment records overriding actor, reason and timestamp, and the override is itself an audited event | Suitability aggregate | Transition refused without a complete override record |
| INV-QUO-02 | A `Quote` carries at least one `Offer` in `QUOTED` or `PARTIALLY_QUOTED`, and zero offers in `FAILED` | Quote aggregate on poll completion | Quote transitions to `FAILED`, not to a zero-offer success |
| INV-QUO-03 | `Offer.premium.amount > 0` and `Offer.benefits.sumAssured > 0` for any offer that may be selected | Quote aggregate on offer ingestion | Offer marked `INVALID` and excluded from selection; a partial error is surfaced |
| INV-QUO-04 | An `Offer` may be selected only while the `Quote` is inside its validity window | Quote aggregate | `409 QUOTE_EXPIRED` |
| INV-QUO-05 | Selecting an offer atomically marks every sibling offer `NOT_SELECTED`; a quote never has two offers in `SELECTED` | Quote aggregate, single transaction | Transaction rolls back |
| INV-PRP-02 | A `Proposal` references exactly one `Offer`, and that offer's `Quote` was in `SELECTED` when the proposal was created | Proposal aggregate | `422 INVALID_OFFER_REFERENCE` |
| INV-PRP-03 | A `Proposal` cannot reach `AWAITING_PAYMENT` unless it is `UW_APPROVED` | Proposal aggregate | `409 ILLEGAL_TRANSITION` |
| INV-PRP-04 | A `Proposal` cannot be `WITHDRAWN` at or after `AWAITING_PAYMENT` | Proposal aggregate | `409 WITHDRAWAL_NOT_PERMITTED` |
| INV-PRP-05 | Proposal form values containing PII are persisted only in the encrypted payload store, never in the proposal's own queryable columns | Proposal repository + a schema assertion test | Persistence rejected; test fails the build |
| INV-PAY-02 | A `Payment` exists for exactly one `Proposal`, and at most one `Payment` per proposal may be in a non-terminal state | Payment aggregate, uniqueness constraint | `409 PAYMENT_ALREADY_IN_PROGRESS` |
| INV-PAY-03 | `Payment.amount` equals the selected `Offer.premium.amount` plus tax as quoted, to the paise | Payment aggregate at initiation | `422 PREMIUM_MISMATCH`; emit a financial-control alert |
| INV-PAY-04 | No new payment attempt may be initiated while a prior attempt for the same proposal is `AUTHORISED` or `UNCERTAIN` | Payment aggregate | `409 PAYMENT_STATE_UNCERTAIN` |
| INV-PAY-05 | A `Payment` reaches `RECONCILED` only on a match against a PG settlement record on `pgTxnId` **and** amount | Reconciliation process | Stays `CAPTURED`; on SLA breach → `RECONCILIATION_BREAK` |
| INV-PAY-06 | A refund is never executed automatically above the configured maker-checker threshold | Payment aggregate + maker-checker | Refund held pending second authorisation |
| INV-POL-01 | A `Policy` may not be created before its `Payment` is `RECONCILED` | Policy aggregate at creation | `409 PAYMENT_NOT_RECONCILED` |
| INV-POL-02 | `Policy.policyNumber` is unique per insurer and immutable once set | Policy aggregate + a unique constraint | Write rejected at the store |
| INV-POL-03 | A `Policy` reaches `ACTIVE` only when policy document references are persisted **and** the issuance audit event is confirmed written | Policy aggregate | Stays `CONFIRMED`; raises an operations task on SLA breach |
| INV-JRN-01 | A `Journey` stage transition is legal only per §5; unknown transitions are rejected and alerted | Journey aggregate | `409 ILLEGAL_TRANSITION` + integrity alert |
| INV-JRN-02 | A `Journey` never stores a business decision (eligibility, price, UW outcome); it stores stage plus references | Journey aggregate schema + an ArchUnit-style assertion | Build fails |
| INV-JRN-03 | Every `Journey` in a non-terminal stage has a defined next action and an inactivity horizon | Journey aggregate | Journey with no horizon fails validation at creation |
| INV-JRN-04 | A `Journey` reaching `COMPENSATING` may never be silently closed; it exits only to `SOLD`, `COMPENSATED` or `MANUAL_INTERVENTION` | Journey aggregate | Transition rejected |
| INV-JRN-05 | A `Journey` reaches `SOLD` only when policy is `ACTIVE`, payment is `RECONCILED`, issuance is confirmed, and all four audit events are persisted | Journey aggregate | Stays in `ISSUED` or `COMPENSATING` |
| INV-IDM-01 | Every mutating platform API accepts an idempotency key; a repeat with the same key and body returns the stored result, and a repeat with a different body returns `409` | Shared idempotency filter | Per the existing `bank-common` idempotency contract |
| INV-ACL-01 | No type from a provider SDK or provider wire model appears outside that provider's adapter package | ArchUnit, per adapter | Build fails — the existing `adapter.onesb.*` rule, generalised |

### 6.3 Invariant placement summary (S06-VT-04: 100% placed)

| Enforcement layer | Invariants |
|---|---|
| Aggregate (in-process, transactional) | INV-LED-01/02/03, INV-CNS-02, INV-SUI-01/02, INV-QUO-01…05, INV-PRP-01…04, INV-PAY-01…04/06, INV-POL-01/03, INV-JRN-01/03/04/05 |
| Database / object-store constraint | INV-CNS-01, INV-AUD-01, INV-POL-02, INV-PRP-05, INV-PAY-02 |
| Cross-cutting filter or library | INV-IDM-01, INV-LOG-01 |
| Integration Hub boundary | INV-DIS-01, INV-ACL-01 |
| Process (reconciliation, outbox) | INV-PAY-05, INV-AUD-02 |
| Infrastructure policy-as-code | INV-DAT-01 |
| Build-time architecture test | INV-JRN-02, INV-ACL-01, INV-LOG-01 |

Every invariant in §6.1 and §6.2 is placed. None is left to "the caller will do it".

---

## 7. Canonical domain events

S06-E05-S03. Events are the published language to Audit, Notification and Reporting. Payload rule:
**identifiers, state and attribution — never PII, never money-bearing detail beyond the amount and
currency needed for reconciliation.**

| Event | Emitted by | Key payload | Consumers |
|---|---|---|---|
| `LeadQualified` | Lead | leadId, customerId, rmId | Journey, Audit, Reporting |
| `ConsentGranted` | Consent | consentId, customerId, statementVersion, otpTxnId | Journey, Audit |
| `ConsentWithdrawn` | Consent | consentId, actor, reason | Journey, Audit, Notification |
| `SuitabilityCompleted` | Suitability | suitabilityId, customerId, lob, outcome, validUntil | Journey, Audit |
| `SuitabilityOverridden` | Suitability | suitabilityId, actor, reason | Audit, Reporting |
| `QuoteRequested` / `QuoteCompleted` | Quotation | quoteId, lob, offerCount, partialErrorCount | Journey, Audit, Reporting |
| `QuoteRejectedNoSuitability` | Quotation | quoteId, customerId, reason | **Audit (mandatory), Reporting** |
| `OfferSelected` | Quotation | quoteId, offerId, insurerCode, premium | Journey, Audit |
| `ProposalSubmitted` | Proposal | proposalId, applicationNumber, insurerCode | Journey, Audit |
| `UnderwritingDecision` | Proposal | proposalId, decision, requirementCount | Journey, Audit, Notification |
| `PaymentLinkIssued` | Payment | paymentId, channel, expiresAt | Journey, Audit, Notification |
| `PaymentAuthorised` / `PaymentCaptured` | Payment | paymentId, pgTxnId, amount, currency | Journey, Audit |
| `PaymentReconciled` | Payment | paymentId, settlementRef | Journey, Audit, Reporting |
| `PaymentReconciliationBreak` | Payment | paymentId, expected, observed | **Audit, Operations alert** |
| `PolicyIssued` | Policy | policyId, policyNumber, insurerCode | Journey, Audit, Notification |
| `PolicyIssuanceFailed` | Policy | proposalId, paymentId, reason | **Journey (drives COMPENSATING), Audit, Operations alert** |
| `JourneySold` | Journey | journeyId, policyId, paymentId, leadId | Lead, Audit, Reporting |
| `JourneyCompensating` | Journey | journeyId, failurePoint | Operations alert, Audit |

**Versioning policy.** Events are additive-only within a major version: adding an optional field is
compatible; removing or retyping a field requires a new major version, and both versions are
published until every consumer has migrated. Consumers must ignore unknown fields.

---

## 8. Canonical model and provider neutrality

S06-E05-S01/S02 and S06-VT-08.

The canonical vocabulary already exists and is good — see
[`canonical-model/contexts.md`](../../1sb-insurance-integration/canonical-model/contexts.md). This
document promotes it from *adapter-scoped* to *platform-scoped* without changing a single term. The
rule that made it work stays and is generalised:

> **Provider vocabulary terminates at the adapter.** `manufacturerId`, `reqId`, `typeOfQuote`,
> `memberSequenceNumber`, `applicationStatus` and every other 1SB term appear only inside
> `adapter.<provider>.*`. Platform contexts speak `insurerCode`, `quoteId`, `mode`,
> `memberSequence`, `bankStatus`.

Scanning the canonical vocabulary in §4–§7 of this document for provider-shaped concepts returns
none. S06-VT-08 passes on the model as written; enforcing it in code is INV-ACL-01, which is an
S08 CI obligation.

---

## 9. Ubiquitous-language glossary delta

New or sharpened terms this document introduces. It supplements — does not replace —
[`knowledge-base/09-glossary.md`](../../au-bank-insurance-platform/knowledge-base/09-glossary.md).

| Term | Meaning in this platform | Not to be confused with |
|---|---|---|
| **Journey** | The orchestrated cross-context sale, owning stage and references only | A user session; a UI flow |
| **Sold** | Policy `ACTIVE` — issued, confirmed, reconciled and audited | Payment captured; policy issued |
| **Offer** | A priced, selectable proposition from one insurer inside a `Quote` | A 1SB `quote[]` array element |
| **Quote** | The aggregate holding the request, all offers and the selection | The adapter's `QuoteJob` correlation record |
| **Assessment validity** | The window in which a `SuitabilityAssessment` may gate a quote | Quote validity |
| **Attribution** | The server-derived `distributorId` + agent identity on every outbound request | A caller-supplied field |
| **Reconciliation break** | Captured money that cannot be matched to a settlement record within SLA | A failed payment |
| **Uncertain payment** | Authorised with a lost response; resolvable only by reconciliation | A declined payment |
| **Compensating** | A journey actively recovering from a post-payment failure | A failed journey |
| **Manual intervention** | Automatic recovery exhausted; a named human owns the outcome | An abandoned journey |
| **Evidence** | An artefact a third party can independently open | A status assertion |

---

## 10. What this document deliberately leaves open

Recorded honestly, with an owner and a target. None of these is claimed closed.

| ID | Open item | Why it is not mine to close | Owner | Target |
|---|---|---|---|---|
| OPEN-D1 | Lead reassignment: does SLA reset, and who receives conversion attribution? | Business rule, not architecture (Aarti raises the same question in her operating contract §5) | Rajal + BA | Before S11 entry |
| OPEN-D2 | Suitability override: who may override, and may an override unblock quote? | Regulatory permissibility — blocked behind GAP-007 | Shailja + Rajal | Before S11 entry |
| OPEN-D3 | Consent statement set, versioning and sequencing | Blocked behind GAP-006; D-011 is open in the business decision log | Shailja + Rajal | Before S11 entry |
| OPEN-D4 | Quote validity window, assessment validity window, payment link TTL, requirement SLA — the actual numbers | Product/Compliance parameters. The state machines are correct without them; the guards are configuration | Rajal + Shailja | Before S11 entry |
| OPEN-D5 | Maker-checker threshold for automatic refund | Financial control policy | Shailja + Finance | Before S11 entry |
| OPEN-D6 | Group B redirect journey states | Out of the R0 platform slice per R0-SCOPE §3; modelled at S13 | Rajal | S13 |
| OPEN-D7 | Field-level attribute sheets ratified at S03 (GAP-016) | S03 is Product/BA-owned. [`02-information-model.md`](./02-information-model.md) supplies the architecture-side model; formal GAP-016 closure needs BA + Rajal sign-off | Rajal + BA | Before S11 entry |
| OPEN-D8 | Audit reconstruction walkthrough (S06-G7, evidence level E3) | Requires a completed journey to reconstruct; no journey runs end to end today | Mahesh + Swapnali | At S11 |

---

**Signed:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2)
**signature_status:** `AI-DRAFTED — mandatory human signature outstanding`
**Date:** 2026-08-16
