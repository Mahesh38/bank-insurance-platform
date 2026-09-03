# S06 — Domain & Information Architecture · Retroactive Stage Evidence

**Stage:** [S06 — Domain & Information Architecture](../stages/S06-domain-architecture.md)
**Gate:** GATE-S06 · **Workstream:** WS-3 (proposed under [CR-010](../../governance/change-requests/CR-010-context-module-and-safe-autopilot.md))
**Compiled by:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2)
**Date:** 2026-08-16
**Retroactive verdict:** **`CLOSED-WITH-CONDITIONS`** — see [§7](#7-retroactive-verdict)

> **Evidence rule GS-1 applies to this file.** Every "present" claim below cites a path a reader can
> open. Where a criterion requires executed evidence (E3/E4) and none exists, the criterion is
> recorded `OPEN`, not closed. Nothing here is closed by assertion.

---

## 1. Method

The stage file's own §6 position assessment was the starting inventory. I re-verified each line
against the repository rather than trusting it, then closed only what was genuinely absent.
**No existing artefact was rewritten or superseded.** The canonical model, the context list and the
ArchUnit provider-isolation rule are good work and are cited, extended and promoted — not replaced.

---

## 2. What the stage requires

From [`S06-domain-architecture.md §5`](../stages/S06-domain-architecture.md), eight exit criteria:

| # | Criterion | Level |
|---|---|---|
| S06-G1 | Bounded context map with relationships and justifications | E2 |
| S06-G2 | Aggregates with state machines and invariants | E2 |
| S06-G3 | Journey saga designed including compensations | E2 |
| S06-G4 | Logical data model per context | E2 |
| S06-G5 | Data ownership matrix complete | E1 |
| S06-G6 | Compliance hard-gate enforcement points named | E2 |
| S06-G7 | Audit model proven to reconstruct a journey | **E3** |
| S06-G8 | Canonical model is provider-neutral | E2 |

Approvers: Mahesh (AP) · Aarti (AP) · Rajal (AP, semantics) · Shailja (RV) · Deepali (RV) ·
Swapnali (RV) · Shivanshi (RV).

---

## 3. Inventory — what already existed

| Artefact | Path | Covers |
|---|---|---|
| 19 bounded contexts, with domain ownership and datastore engine | [`business-problem-statement.md §6`](../../context/business-problem-statement.md) | S06-E01-S01 (partial: named, not related) |
| Service catalogue with owned canonical objects and boundary rationale | [`architecture-review/02-target-microservices-architecture.md`](../../platform/architecture-review/02-target-microservices-architecture.md) | S06-E01-S04 (partial) |
| Canonical context model — Party, Distribution, Catalog, Suitability, Quotation, Proposal, Payment; the `Journey` shared kernel | [`canonical-model/contexts.md`](../../1sb-insurance-integration/canonical-model/contexts.md) | S06-E05-S01, and the seed of S06-E02 |
| Payload simplification rationale | [`canonical-model/simplifying-payloads.md`](../../1sb-insurance-integration/canonical-model/simplifying-payloads.md) | S06-E05-S01 |
| Canonical business object list | [`knowledge-base/07-information-model-and-rules.md`](../../au-bank-insurance-platform/knowledge-base/07-information-model-and-rules.md) | S06-E04-S01 (object level, no attributes) |
| Glossary | [`knowledge-base/09-glossary.md`](../../au-bank-insurance-platform/knowledge-base/09-glossary.md) | Ubiquitous language baseline |
| Real domain layer: ports, commands, models, zero framework annotations | `services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/domain/` | S06-E02 for context #15 |
| Working state enumerations in code | [`JobStatus.java`](../../../libs/bank-common-domain/src/main/java/com/bank/common/domain/JobStatus.java), [`PaymentStatus.java`](../../../libs/bank-common-domain/src/main/java/com/bank/common/domain/PaymentStatus.java), [`BankApplicationStatus.java`](../../../libs/bank-common-domain/src/main/java/com/bank/common/domain/BankApplicationStatus.java) | Bank-owned domain enums in `bank-common-domain` (REFACTOR-001 / EPIC-002); not platform aggregates |
| Provider-isolation rule, **enforced by ArchUnit** | [`1sb-integration-service-architecture.md §3`](../../1sb-insurance-integration/architecture/1sb-integration-service-architecture.md) | **S06-E05-S02 implemented and tested** |
| Audit event schema, physically defined | [`V1__init_schema.sql`](../../../services/bank-persistence-service/src/main/resources/db/migration/V1__init_schema.sql) `audit_event` | S06-E04-S05 (partial) |
| Data classification scheme (four levels) | [`stages/S02-regulatory-framing.md`](../stages/S02-regulatory-framing.md) S02-E05-S01 | Vocabulary for S06-E04-S03 |

**S06-E05-S02 deserves its own line.** The rule that provider types may not escape
`adapter.onesb.*` is not merely documented — it is an executing ArchUnit test. That is E4-grade
evidence for a criterion that only needed E2, and it is more than most programmes achieve at this
stage.

---

## 4. What was missing

Verified against the repository, matching the stage file's §6 open list:

| Gap | Evidence of absence |
|---|---|
| Context **relationships** | No upstream/downstream, conformist or ACL designation anywhere. The 19 contexts are a list |
| Platform **aggregates** | No aggregate model for Lead, Consent, Suitability, Proposal, Payment or Policy. Only `1sb-integration-service` has a domain layer |
| **State machines** | Adapter-level enums exist; no aggregate lifecycle for any platform concept |
| **Journey saga** | Absent. `canonical-model/contexts.md §8` sketches a `Journey` shape with a `stage` field and stops there. No transitions, no compensations |
| **Invariants** | Not catalogued platform-wide. Design rules exist in prose in the 1SB architecture; none is expressed as a testable assertion with an id |
| **Data ownership matrix** | Absent |
| **Logical data model with attributes** | Object names only — GAP-016 |
| **Audit reconstruction** | The schema exists; nothing proves a business journey can be reconstructed from it |
| Compliance **enforcement points** | Controls C1–C10 are catalogued in [`07-SECURITY-COMPLIANCE-CANON.md`](../07-SECURITY-COMPLIANCE-CANON.md); the aggregate and code location that enforces each is not named |

---

## 5. New evidence produced by this increment

| Artefact | Path | Closes |
|---|---|---|
| **Context relationship register** — 10 named relationships with the pattern and what crosses each seam | [`ws3-platform/01 §2`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | S06-E01-S02 |
| **Language boundary table** — the four words that mean different things across contexts, with translation points | [`ws3-platform/01 §2.2`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | S06-E01-S03 |
| **Distinct-reason-to-change justification** per context, plus the contexts deliberately *not* created | [`ws3-platform/01 §2.3`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | S06-E01-S04, S06-VT-01 |
| **Aggregate register** — 10 aggregates with consistency boundaries and what is deliberately outside each | [`ws3-platform/01 §3`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | S06-E02-S01, S06-VT-02 |
| **Seven state machines** — Lead, Consent, Suitability, Quote, Proposal, Payment, Policy — with legal transitions, guards and terminal states | [`ws3-platform/01 §4`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | S06-E02-S02, S06-VT-03 |
| **Identity and lifecycle rules** ID-01…ID-04 | [`ws3-platform/01 §3.1`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | S06-E02-S04 |
| **The journey saga** — orchestrated state machine with `COMPENSATING` and `MANUAL_INTERVENTION` as first-class states | [`ws3-platform/01 §5`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | S06-E02-S05, S06-G3 |
| **Failure and compensation matrix** — 10 failure points, each with a compensation or a named manual procedure and owner | [`ws3-platform/01 §5.1`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | **S06-VT-05** |
| **Cross-aggregate consistency decisions** — where eventual is acceptable and where it is not | [`ws3-platform/01 §5.2`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | S06-E03-S04 |
| **Invariant catalogue** — 8 compliance hard-gates + 33 aggregate invariants, each with a testable assertion, an enforcement point and a violation behaviour | [`ws3-platform/01 §6`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | S06-E03-S01, S06-E03-S03, **S06-VT-04** |
| **Invariant placement summary** — every invariant assigned to an enforcement layer | [`ws3-platform/01 §6.3`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | S06-VT-04 pass condition |
| **Canonical domain events** with payload rule and versioning policy | [`ws3-platform/01 §7`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | S06-E05-S03 |
| **Ubiquitous-language glossary delta** — 11 terms sharpened | [`ws3-platform/01 §9`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | S06-E01-S03 |
| **Logical information model** — 9 attribute sheets with type, optionality, classification, retention class and system of record | [`ws3-platform/02 §4`](../../platform/ws3-platform/02-information-model.md) | S06-E04-S01, S06-E04-S03 |
| **Data ownership matrix** — sole writer per information set, with five explicit ambiguity resolutions | [`ws3-platform/02 §3`](../../platform/ws3-platform/02-information-model.md) | S06-E04-S02, **S06-VT-06** |
| **Reference and master data** with effective dating and cache policy | [`ws3-platform/02 §4.10`](../../platform/ws3-platform/02-information-model.md) | S06-E04-S04 |
| **Audit data model** — the existing schema plus four additions required for reconstruction, with the reconstruction test defined | [`ws3-platform/02 §5`](../../platform/ws3-platform/02-information-model.md) | S06-E04-S05 |
| **PII handling rules** PII-01…PII-06 derived from the classification | [`ws3-platform/02 §6`](../../platform/ws3-platform/02-information-model.md) | S06-E04-S03 |

### 5.1 Compliance hard-gate enforcement points — S06-E03-S02 / S06-G6

The specific table the criterion asks for, extracted here so a Compliance reviewer does not have to
navigate to find it.

| Control | Invariant | Enforcement point | Behaviour on violation |
|---|---|---|---|
| C1 Suitability hard-gate | INV-QUO-01 | `QuotationService.request()`, server-side, before any Hub call | `403 SUITABILITY_REQUIRED`; `Quote` persisted in `REJECTED`; audit event emitted |
| C2 Consent evidence | INV-PRP-01, INV-CNS-01/02 | `ProposalService.submit()`; consent store is INSERT-only | `403 CONSENT_REQUIRED`; proposal stays `DRAFT` |
| C3 Attribution | INV-DIS-01 | Integration Hub, before adapter dispatch | `422 ATTRIBUTION_NOT_CALLER_SUPPLIED` + security event |
| C4 Payment device isolation | INV-PAY-01 | `PaymentService.issueLink()` and the PG redirect handler | `403 PAYMENT_DEVICE_ISOLATION` + security event |
| C5 PII masking | INV-LOG-01 | Logging framework converter + CI log-scan | Build fails; runtime masks |
| C6 Data residency | INV-DAT-01 | IaC policy check pre-apply | `apply` blocked; runtime drift raises O0 |
| C7 7-year retention | INV-AUD-01 | INSERT-only grant + Object Lock | Write refused at the store layer |
| C8 Audit completeness | INV-AUD-02 | Aggregate transition hook + outbox | Journey cannot reach `SOLD` |
| C9 Maker-checker | INV-PAY-06; WS-2 A.4 | Payment refund path; WS-2 privileged changes | Held pending second authorisation |
| C10 Encryption | Security architecture §5 | KMS CMK per data class; TLS 1.3 | Store creation blocked by IaC policy |

Every one of C1–C10 now has a named enforcement point. **None of C1, C2, C4, C6 or C7 is
implemented**, which is exactly what [`07-SECURITY-COMPLIANCE-CANON.md §3`](../07-SECURITY-COMPLIANCE-CANON.md)
records and what S11 exists to deliver. Naming the enforcement point is what S06 owes; building it
is not.

---

## 6. Criterion-by-criterion evidence table

| # | Criterion | Required | Evidence | State | Verified by |
|---|---|---|---|---|---|
| S06-G1 | Context map with relationships and justifications | E2 | [`ws3-platform/01 §2, §2.3`](../../platform/ws3-platform/01-domain-model-and-invariants.md); context list in [`business-problem-statement.md §6`](../../context/business-problem-statement.md) | **MET** pending signature | Mahesh (AI-drafted) |
| S06-G2 | Aggregates with state machines and invariants | E2 | [`ws3-platform/01 §3, §4, §6`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | **MET** pending signature | Mahesh (AI-drafted) |
| S06-G3 | Journey saga with compensations | E2 | [`ws3-platform/01 §5, §5.1`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | **MET** pending signature | Mahesh (AI-drafted) |
| S06-G4 | Logical data model per context | E2 | [`ws3-platform/02 §4`](../../platform/ws3-platform/02-information-model.md) | **MET pending Aarti's signature (AP)** | — |
| S06-G5 | Data ownership matrix complete | E1 | [`ws3-platform/02 §3`](../../platform/ws3-platform/02-information-model.md) | **MET** | Mahesh |
| S06-G6 | Compliance hard-gate enforcement points named | E2 | §5.1 of this file; [`ws3-platform/01 §6.1`](../../platform/ws3-platform/01-domain-model-and-invariants.md) | **MET pending Shailja's review (RV)** | — |
| S06-G7 | **Audit model proven to reconstruct a journey** | **E3** | Reconstruction test **defined** at [`ws3-platform/02 §5`](../../platform/ws3-platform/02-information-model.md); **not executed** | **OPEN** | — |
| S06-G8 | Canonical model is provider-neutral | E2 | [`ws3-platform/01 §8`](../../platform/ws3-platform/01-domain-model-and-invariants.md); ArchUnit rule in `1sb-integration-service` | **MET** | Mahesh |

### 6.1 Why S06-G7 stays OPEN

S06-G7 requires evidence level **E3 — executed, point-in-time**. Rule GS-2 is explicit: a behaviour
criterion requires E3 or E4, and a document asserting behaviour does not close it. The
reconstruction test needs a completed business journey to reconstruct, and **no business journey
runs end to end in this repository today** — [`01-POSITION-ASSESSMENT.md §5`](../01-POSITION-ASSESSMENT.md)
records S11 as not started at platform level.

Designing the model so that reconstruction is *possible* — including the four schema additions and
the per-journey `sequence_no` that makes a missing event detectable — is what S06 can deliver.
Proving it is an S11 obligation. Recording this as `MET` would be exactly the evidence-substitution
anti-pattern the gate model names.

---

## 7. Retroactive verdict

```yaml
stage_evidence:
  stage: S06
  gate_id: GATE-S06
  workstream: WS-3
  compiled_by: "Mahesh — Principal Insurance Platform Architect"
  date: 2026-08-16
  verdict: CLOSED-WITH-CONDITIONS
  criteria_met: [S06-G1, S06-G2, S06-G3, S06-G4, S06-G5, S06-G6, S06-G8]
  criteria_open: [S06-G7]
  conditions:
    - id: S06-C1
      condition: >
        Aarti ratifies the logical information model and the four audit-schema additions
        as the S06-G4 approver.
      owner: "Aarti / Database"
      target: "before GATE-S08 sign-off"
    - id: S06-C2
      condition: >
        Rajal ratifies domain semantics (S06 approver, semantics) — in particular the
        journey stage vocabulary and the definition of Sold.
      owner: "Rajal / Product"
      target: "before GATE-S08 sign-off"
    - id: S06-C3
      condition: >
        Shailja reviews the compliance hard-gate enforcement points in §5.1 and confirms
        that naming the enforcement point satisfies the S06 obligation, with implementation
        remaining an S11 obligation.
      owner: "Shailja / Compliance"
      target: "before GATE-S08 sign-off"
    - id: S06-C4
      condition: >
        S06-G7 audit reconstruction executed against a completed journey. Carried forward
        as an S11 entry obligation, not waived.
      owner: "Mahesh + Swapnali"
      target: "S11"
    - id: S06-C5
      condition: >
        GAP-016 formally closed at S03 with per-attribute business validation rules.
        The architecture-side model in ws3-platform/02 unblocks S06; it does not close S03.
      owner: "Rajal + R11 / BA"
      target: "before S11 entry"
    - id: S06-C6
      condition: >
        OPEN-D1 through OPEN-D8 in ws3-platform/01 §10 carry named owners and targets and
        are transcribed into the appropriate registers by the orchestrator.
      owner: "Kalpana / Delivery"
      target: "with CR-010 ratification"
  signature_status: "AI-DRAFTED — mandatory human signature outstanding"
  required_signatures:
    - "Mahesh / Architecture (AP) — HUMAN"
    - "Aarti / Database (AP) — HUMAN"
    - "Rajal / Product (AP, semantics) — HUMAN"
    - "Shailja / Compliance (RV)"
    - "Deepali / Security (RV)"
    - "Swapnali / QA (RV)"
    - "Shivanshi / SRE (RV)"
  note: >
    CLOSED-WITH-CONDITIONS means seven of eight criteria have artefacts at the required
    evidence level and one is honestly OPEN. It does not mean the gate has passed.
    Under 04-GATE-AND-SIGNOFF-MODEL section 4, only Mahesh and Rajal jointly, as humans,
    mark a gate PASSED, and no AI-drafted verdict substitutes for that.
```

---

## 8. What remains open, with owners

| ID | Open item | Owner | Target |
|---|---|---|---|
| S06-G7 | Audit reconstruction executed (E3) | Mahesh + Swapnali | S11 |
| OPEN-D1 | Lead reassignment SLA and attribution rules | Rajal + BA | Before S11 entry |
| OPEN-D2 | Suitability override authority | Shailja + Rajal | Before S11 entry (GAP-007) |
| OPEN-D3 | Consent statement set, versioning, sequencing | Shailja + Rajal | Before S11 entry (GAP-006) |
| OPEN-D4 | Validity-window and TTL values | Rajal + Shailja | Before S11 entry |
| OPEN-D5 | Maker-checker threshold for automatic refund | Shailja + Finance | Before S11 entry |
| OPEN-D6 | Group B redirect journey states | Rajal | S13 |
| OPEN-D7 | GAP-016 formal closure at S03 | Rajal + BA | Before S11 entry |
| OPEN-D8 | Audit reconstruction walkthrough record | Mahesh + Swapnali | S11 |
| OPEN-I2 | Tokenisation capability for Aadhaar references | Deepali + Aarti | Before S11 entry |
| OPEN-I3 | Audit-schema migration for the four additions | Aarti + Amit | Foundation Recovery Increment |

---

**Signed:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2)
**signature_status:** `AI-DRAFTED — mandatory human signature outstanding`
