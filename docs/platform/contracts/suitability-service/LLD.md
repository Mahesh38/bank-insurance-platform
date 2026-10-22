# suitability-service — Low-level design (contract companion)

**Status:** AI-DRAFTED 2026-10-22 from owner decisions D1 (ADR-024), D4, D5, `D-022` and ADR-027,
awaiting Mahesh (architecture), Rajal (behaviour) and Shailja (Compliance) review. Rule **content**
(thresholds, reason codes, Savings/ULIP model) needs Shailja's approval per rule-set version; the
suitability rule pack is ratification-pending (GAP-007). Wire contract: [`openapi.yaml`](./openapi.yaml).

Sources: [DECISIONS-2026-10-21 §D4, §D5](../../architecture-review/DECISIONS-2026-10-21.md) ·
[ADR-024](../../architecture-review/ADR-024-configuration-driven-rule-engine.md) ·
[ADR-025](../../architecture-review/ADR-025-event-specification-and-publisher.md) ·
[suitability rule pack](../../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md) ·
[domain model](../../ws3-platform/01-domain-model-and-invariants.md) (§4.3, INV-SUI-01, INV-QUO-01) ·
[DDL `06-suitability.sql`](../../data-architecture/schemas/06-suitability.sql) ·
[API reference page](../../api-reference/services/suitability-service.md).

## 1. Purpose and ownership

suitability-service (#7, L5, port 8093) captures the need-analysis answers for one product class,
evaluates **every** LIFE class with the rule engine, and keeps the **per-class outcome record**
(D4) that the C1 gate reads. It owns the gate *read*; quotation owns the gate *enforcement*
(SUIT-R25). It never decides price or product availability.

## 2. API summary

| Operation | Path | Caller |
|---|---|---|
| `createAssessment` | `POST /internal/v1/assessments` | Journey (S-07) |
| `getAssessment` | `GET /internal/v1/assessments/{suitabilityId}` | Journey, BFF |
| `getQuestionSet` | `GET /internal/v1/assessments/{suitabilityId}/questions` | BFF (via Journey) |
| `saveAnswers` | `PUT /internal/v1/assessments/{suitabilityId}/answers` | Journey |
| `evaluate` | `POST /internal/v1/assessments/{suitabilityId}/evaluate` | Journey (S-07) |
| `getEvaluation` | `GET /internal/v1/evaluations/{evaluationId}` | Journey, BFF, Audit |
| `acknowledgeDisclosure` | `POST /internal/v1/evaluations/{evaluationId}/acknowledgements` | Journey |
| `recordUsage` | `POST /internal/v1/evaluations/{evaluationId}/usages` | Journey |
| `lockEvaluation` | `POST /internal/v1/evaluations/{evaluationId}/lock` | Quotation (final BI) |
| `getUsableEvaluation` | `GET /internal/v1/evaluations:usable?customerId&lob&productClass&evaluationId` | Quotation (S-08) |

Changes from the api-reference PROPOSED shapes: the gate read moves from
`GET /assessments/{id}/validity` (200 + `usable:false`) to `GET /evaluations:usable` (200 or
`403 SUITABILITY_REQUIRED`); `lock` moves to the evaluation; `override` is not offered (SUIT-R38,
R0 has no override path; `OPEN-D2` stays open for R1).

## 3. Resolved conflicts

| Conflict | Resolution here | Basis |
|---|---|---|
| INV-SUI-01 (COMPLETED immutable) vs api-reference §2.2 (saving answers re-opens a COMPLETED assessment) | **INV-SUI-01 wins.** `PUT answers` on `COMPLETED`/`LOCKED`/`SUPERSEDED` → `409 ASSESSMENT_IMMUTABLE`. A correction is `POST /assessments` with `supersedesSuitabilityId`; the old one becomes `SUPERSEDED` when the new one is evaluated (so the customer is never left without an evaluation mid-correction) | Owner brief; domain model §6.2; closes `OPEN-SUI-IMMUTABLE` for the API |
| Single `ELIGIBLE/NOT_ELIGIBLE` (DDL, INV-QUO-01) vs per-class outcomes (rule pack §4) | Per-class record canonical; `INV-QUO-01` reads "outcome for class X ∈ `SUITABILITY.GATE.allowedOutcomes`" | D4 |
| Gate failure: S-08 "timeout ⇒ 403 SUITABILITY_REQUIRED" vs SUIT-R26 "unavailable ⇒ 503" | A *decision* to refuse = `403 SUITABILITY_REQUIRED` (with `refusalReason`). *Cannot decide* (timeout, 5xx, unreachable) = quotation returns `503 UPSTREAM_UNAVAILABLE`. Both fail closed, no quote, quote recorded `REJECTED` | Owner brief |
| Rule pack §6.2 seven distinct 403 codes vs one registered `SUITABILITY_REQUIRED` | One code; the seven become `refusalReason` values on the problem body | Keeps the catalogue small; evidence keeps the detail (SUIT-R28) |
| SUIT-R21 (evaluation needs `CNS-SOL` first) vs D5 (consent after suitability) | Suitability does **not** check consent. Journey calls `consents:check` at `BEFORE_SUITABILITY` before `createAssessment`; that checkpoint is **empty in the seed `CONSENT.POLICY`**. If Compliance refuses D5, publishing `CNS-DP`/`CNS-SOL` at `BEFORE_SUITABILITY` restores SUIT-R21 without code. `consent_id_sol` is recorded when one exists | D5 |
| `SUITABILITY_INCOMPLETE` named in rule pack, absent from the catalogue | Registered as **NEW** (`422`, VALIDATION) | Readiness review §3.3 |

## 4. Flows

### 4.1 Create

1. Validate (§6). PDP `suitability.perform` for the actor (accountable SP / process-further actor,
   `INV-ACT-01`, `D-019`) → `403` on deny; SP certification → `403 SP_CERTIFICATION_REQUIRED`.
2. Resolve `SUITABILITY.LIFE.{productClass}` in force (S-21, fail closed →
   `422 CONFIGURATION_UNRESOLVABLE`); pin its version on the assessment (`"pinning": "JOURNEY"`).
3. Existing `IN_PROGRESS` assessment for `(lead_id, product_class)` → return it (`200`, resume).
4. With `supersedesSuitabilityId`: predecessor must be `COMPLETED` for the same customer and class;
   decrypt its latest answers, copy as answer set v1.
5. `INSERT suitability` (`IN_PROGRESS`, edit lock = actor, `edit_lock_until` from configuration)
   + `idempotency_record` → `201`.

### 4.2 Save answers

1. State `IN_PROGRESS` else `409 ASSESSMENT_IMMUTABLE`; caller holds the edit lock else
   `409 CONFLICT`.
2. Validate each answer against the pinned rule set's `inputs` (type, enum, range — rule pack §2.2);
   question id outside the set → `422 VALIDATION_ERROR`.
3. Merge with the previous answer set, encrypt the whole document, `INSERT suitability_answer_set`
   (`version + 1`); header `current_answer_set_version`, `last_screen_id`.

### 4.3 Evaluate

```mermaid
sequenceDiagram
    participant JO as journey
    participant SU as suitability-service
    participant CU as customer-service
    participant CFG as administration-config
    participant ENG as bank-rule-engine (in process)
    JO->>SU: POST /assessments/{id}/evaluate {answerSetVersion}
    SU->>SU: state IN_PROGRESS? version current? own class set complete? (else 422 SUITABILITY_INCOMPLETE)
    SU->>CU: GET /customers/{cid}/snapshots/{partySnapshotRef} → derived SQ-D01, D02, D04
    SU->>CFG: resolve SUITABILITY.LIFE.TERM/SAVINGS/ULIP + SUITABILITY.GATE (pinned / in force)
    loop each class
        SU->>ENG: evaluate(ruleSet, inputs) → outcome, fired[], reasonCodes[], derived
    end
    SU->>SU: BEGIN; INSERT evaluation + evaluation_outcome×3; header COMPLETED; predecessor SUPERSEDED (+ its evaluation); outbox SuitabilityCompleted; COMMIT
    SU-->>JO: 200 Evaluation
```

1. Decrypt the answer set in process. Completeness is checked against the **assessment's own
   class** (SUIT-R01a) → `422 SUITABILITY_INCOMPLETE` with `errors[].field = SQ-xx`; nothing written.
2. Derived inputs: `SQ-D01 ageNextBirthday` (null → engine `missingInputOutcome` =
   `INSUFFICIENT_DATA`), `SQ-D02`, `SQ-D04` from the customer snapshot; `SQ-D03` (policy service) is
   not available in R0 → `0` with flag `ASSUMED_ZERO`. SUIT-R03 "use the higher value" is a rule-set
   `derive` (`max`), not code.
3. For each LIFE class (TERM, SAVINGS, ULIP) evaluate its rule set on the same inputs. Classes whose
   set was not answered resolve to `INSUFFICIENT_DATA` through `missingInputOutcome` (SUIT-R01c).
4. `SUITABILITY.GATE` gives `validityDays` (seed 30), `allowedOutcomes` (seed `SUITABLE`,
   `SUITABLE_WITH_CAUTION`), `cautionRequiresAcknowledgement` (seed true). `valid_until =
   evaluated_at + validityDays`.
5. `underwritingRiskFlag` (SUIT-R04) and `dataInconsistencyFlags` (SUIT-R03) are rule-set outputs
   recorded on the evaluation; `replacementDisclosureRequired = (SQ-12 = true)`.
6. PDF (SUIT-R22) is produced asynchronously from the `SuitabilityCompleted` event by the document
   path; `pdf_artefact_ref` is filled later (open item O-4).
7. Same inputs + same versions → same result bytes (`inputs_hash`, SUIT-R05). A replay with the same
   idempotency key returns the stored evaluation.

### 4.4 Gate read (S-08)

Given `evaluationId`, `customerId`, `lob`, `productClass` — checked in this order, first failure wins:

| # | Check | `refusalReason` |
|---|---|---|
| 1 | evaluation exists | `SUITABILITY_EVALUATION_NOT_FOUND` |
| 2 | `customer_id` = query `customerId` (else also a **security event**, SUIT-R36) | `SUITABILITY_SUBJECT_MISMATCH` |
| 3 | status `ACTIVE` or `LOCKED` | `SUITABILITY_EVALUATION_INVALID` |
| 4 | `now < valid_until` (unless `LOCKED`, SUIT-R34) | `SUITABILITY_EVALUATION_EXPIRED` |
| 5 | outcome for class ≠ `INSUFFICIENT_DATA` | `SUITABILITY_INSUFFICIENT_DATA` |
| 6 | outcome for class ∈ `SUITABILITY.GATE.allowedOutcomes` | `PRODUCT_CLASS_NOT_SUITABLE` |
| 7 | caution → acknowledgement row exists for the class's reason codes | `CAUTION_NOT_ACKNOWLEDGED` |
| 8 | `SQ-12 = true` → `REPLACEMENT` acknowledgement exists (SUIT-R46) | `REPLACEMENT_DISCLOSURE_REQUIRED` |

All pass → `200 GateDecision`; any fail → `403 SUITABILITY_REQUIRED` + outbox
`SuitabilityGateRefused` (SUIT-R28; the quotation's own `QuoteRejectedNoSuitability` remains the
mandatory audit event). Budget: answer within 500 ms; reads only the primary (no cache that could
allow on a miss). The gate code holds 100 % branch coverage (SUIT-R27).

### 4.5 Acknowledgement

1. Evaluation `ACTIVE`; class outcome `SUITABLE_WITH_CAUTION` (kind `CAUTION`) or
   `replacementDisclosureRequired` (kind `REPLACEMENT`); `reasonCodes` must equal the class's
   caution reason codes (else `422 VALIDATION_ERROR`).
2. Verify `otpTxnId` is a verified OTP transaction for this customer with the OTP provider
   (same provider and `verificationModes` policy as consent; RM entry of a customer-device OTP is
   barred by SUIT-R42 unless Compliance enables `RM_RELAYED`).
3. `INSERT disclosure_acknowledgement` (append-only), outbox `SuitabilityDisclosureAcknowledged`.

### 4.6 Expiry and invalidation

Gate correctness uses `valid_until` at read time. A sweeper appends status changes and emits
`SuitabilityExpired`: elapsed validity (SUIT-R29, not if `LOCKED`); `ConsentWithdrawn` for
`CNS-SOL` → `INVALIDATED` (SUIT-R30, only while Compliance keeps `CNS-SOL` tied to suitability);
`RuleSetPublished` with a MAJOR change of a class rule set → `INVALIDATED` reason
`MODEL_VERSION_SUPERSEDED` (SUIT-R32, not if `LOCKED`). Inactivity → assessment `ABANDONED`.

## 5. Rule-engine usage

| Rule set | Hit policy | Outcome rank | Notes |
|---|---|---|---|
| `SUITABILITY.LIFE.TERM` | `PRIORITY`, `collectReasonCodes: ALL_FIRED` | `NOT_SUITABLE > INSUFFICIENT_DATA > SUITABLE_WITH_CAUTION > SUITABLE` | SUIT-R08–R12; inputs SQ-01–04, 08–12 + derived; `noMatchOutcome`/`missingInputOutcome` = `INSUFFICIENT_DATA` |
| `SUITABILITY.LIFE.SAVINGS` | same | same | SUIT-R13–R15 (rule-pack arithmetic model seeded; BRD insurer grid would be a new version — D4) |
| `SUITABILITY.LIFE.ULIP` | same | same | SUIT-R16–R19; **`protectionGap` not derived** (no SQ-02/03/04, D4) |
| `SUITABILITY.GATE` | parameters only | — | `allowedOutcomes`, `cautionRequiresAcknowledgement`, `validityDays`, `editLockMinutes` (value not in any source — unset, O-5) |

Thresholds (0.15, 0.30, 0.20, income bands, age factors) are rule-set `parameters`/`tables` seeded
from the rule pack §3–§4 and **await Shailja's approval**; the seed rule-set files are owned by the
rule-engine work, not this LLD.

## 6. Validation rules

| Field | Rule | Error code |
|---|---|---|
| ids (`leadId`, `journeyId`, `customerId`, `partySnapshotRef`, path ids) | ULID | `422 VALIDATION_ERROR` |
| `lob` | `LIFE` (R0) | `422 UNSUPPORTED_LOB` |
| `productClass` | `TERM` \| `SAVINGS` \| `ULIP` | `422 UNSUPPORTED_LOB` |
| `supersedesSuitabilityId` | `COMPLETED`, same customer and class | `409 ILLEGAL_TRANSITION` |
| `answers` keys | in the pinned set | `422 VALIDATION_ERROR` |
| `SQ-01`, `SQ-08` | > 0 | `422 VALIDATION_ERROR` |
| `SQ-02` | integer 0–10 | `422 VALIDATION_ERROR` |
| `SQ-03`, `SQ-04` | ≥ 0 | `422 VALIDATION_ERROR` |
| `SQ-05` / `SQ-06` / `SQ-07` / `SQ-10` / `SQ-11` | rule-pack §2.2 enums (`SQ-06` is the enum `LT_5Y…GT_20Y`, not a band) | `422 VALIDATION_ERROR` |
| `SQ-09`, `SQ-12` | boolean | `422 VALIDATION_ERROR` |
| own set complete at evaluate | SUIT-R01a | `422 SUITABILITY_INCOMPLETE` (NEW) |
| `answerSetVersion` | = current | `409 CONFLICT` |
| state for save | `IN_PROGRESS` | `409 ASSESSMENT_IMMUTABLE` |
| edit lock | held by actor | `409 CONFLICT` |

## 7. DTO → domain → table mapping

| DTO field | Domain | Table.column |
|---|---|---|
| `suitabilityId`, `leadId`, `journeyId`, `customerId`, `lob`, `productClass` | `SuitabilityAssessment` | `suitability.suitability_id`, `lead_id`, `journey_id`, `customer_id`, `lob`, `product_class` |
| `questionSet.{ruleSetId,version}` | pinned rule set | `questionnaire_version` → **widen to `VARCHAR(64)`** and hold `SUITABILITY.LIFE.TERM@1.0.0` (or split into **new** `rule_set_id` + `rule_set_version`) |
| `state` | | `state` (CHECK keeps `IN_PROGRESS, COMPLETED, LOCKED, SUPERSEDED, EXPIRED, ABANDONED`; `NO_PRODUCT_FOUND` and `OVERRIDDEN` unused in R0) |
| `supersedesSuitabilityId` | | **new** `supersedes_suitability_id` |
| `partySnapshotRef` | | **new** `party_snapshot_ref` |
| answers | `AnswerSet` (encrypted) | `suitability_answer_set.answers_enc` (keys become `SQ-xx`; the COMMENT's BRD key list is superseded, `OPEN-SUIT-06`) |
| `evaluationId`, `status`, `evaluatedAt`, `validUntil`, `answerSetVersion`, `inputsHash`, `engineVersion`, flags, `gateRuleSet`, `pdfArtefactRef` | `Evaluation` | **new** `suitability.evaluation` (`evaluation_id`, `suitability_id`, `customer_id`, `lob`, `answer_set_version`, `status`, `evaluated_at`, `evaluated_by`, `evaluated_by_agent_id`, `valid_until`, `inputs_hash`, `engine_version`, `gate_rule_set_ref`, `underwriting_risk_flag`, `data_inconsistency_flags TEXT[]`, `replacement_disclosure_required`, `consent_id_sol` NULL, `locked_quote_id`, `locked_at`, `invalidation_reason`, `pdf_artefact_ref`) |
| `outcomes[]` | `ClassOutcome` | **new** `suitability.evaluation_outcome` (`evaluation_id`, `product_class`, `outcome` CHECK four values, `reason_codes TEXT[]` in rule order, `fired_rule_ids TEXT[]`, `derived JSONB` (optional keys), `rule_set_id`, `rule_set_version`; PK `(evaluation_id, product_class)`) |
| acknowledgement | `DisclosureAcknowledgement` | **new** `suitability.disclosure_acknowledgement` (`acknowledgement_id`, `evaluation_id`, `kind`, `product_class`, `reason_codes`, `disclosure_id`, `disclosure_version`, `otp_txn_id`, `verification_mode`, `acknowledged_at`, `actor_id`) |
| usage | `EvaluationUsage` | **new** `suitability.evaluation_usage` (`evaluation_id`, `journey_id`, `used_at`, `actor_id`; PK `(evaluation_id, journey_id)`) |
| header `currentEvaluationId` | | **new** `current_evaluation_id` (replaces `current_mapping_run_id`) |

**DDL changes this contract needs in `06-suitability.sql`** (applied in parallel by the DDL owner):
drop `outcome` + `ck_suit_outcome` + `ck_suit_no_product` + `ck_suit_completed_outcome` from the
header (outcomes live per class); the four new tables above; `suitability_mapping_run` superseded
by `evaluation`/`evaluation_outcome` (product recommendation is a catalogue concern — O-3); replace
`ux_suit_lead_current` with a partial unique on `(lead_id, product_class) WHERE state = 'IN_PROGRESS'`
(a correction coexists with the `COMPLETED` predecessor until evaluated); header comment
"Term leads have no row (BR-SUIT-002)" is obsolete (`D-022`: Term has its own set); evaluation
tables INSERT-only by grant (SUIT-R23) with status changes as appended rows or a separate
`evaluation_status_history`; `outbox_event` gains ADR-025 `parked_at` and `schema_version`.

## 8. Invariants

| Invariant | Enforced at |
|---|---|
| INV-SUI-01 COMPLETED immutable; correction = new assessment, old `SUPERSEDED` | Aggregate (`saveAnswers` guard) + INSERT-only evaluation tables |
| INV-QUO-01 (D4 wording) | `getUsableEvaluation` checks + quotation's enforcement |
| SUIT-R05 deterministic | Engine purity + `inputs_hash`; `RuleEngineDeterminismTest` |
| SUIT-R19a all reason codes in rule order | Engine `collectReasonCodes: ALL_FIRED`; persisted array order |
| SUIT-R36 never across customers | Gate check #2 + security event |
| SUIT-R38/R39/R40 no override, gate not disableable | No override endpoint; no flag in code (ArchUnit/grep check) |

## 9. Events

Produced (topic `{env}.suitability.assessment.v1`, key `suitabilityId`; `configVersionRef` =
rule-set refs):

| Event | `data` |
|---|---|
| `SuitabilityCompleted` | `evaluationId`, `customerId`, `ruleSetId`, `ruleSetVersion` (of the assessment's class), `outcomes[{productClass, outcome}]`, `leadId`, `journeyId`, `validUntil` — per `docs/platform/contracts/events/schemas/SuitabilityCompleted.v1.json` (replaces the single `outcome`) |
| `SuitabilityDisclosureAcknowledged` | `evaluationId`, `kind`, `productClass` |
| `SuitabilityGateRefused` | `evaluationId?`, `customerId`, `productClass`, `refusalReason` |
| `SuitabilityExpired` / `SuitabilityInvalidated` | `evaluationId`, `reason` |
| `SuitabilitySuperseded` | `suitabilityId`, `supersededBy` |

Consumed: `ConsentWithdrawn` (SUIT-R30), `RuleSetPublished` (cache + SUIT-R32).

## 10. Idempotency, timeouts, failure modes

| Aspect | Value |
|---|---|
| Keys | `journey:{journeyId}:suitability:create[:{supersedes}]`, `suitability:{id}:answers:{screenId}:{n}`, `suitability:{id}:evaluate:{answerSetVersion}`, `evaluation:{id}:ack:{kind}:{class}` — `suitability.idempotency_record` |
| Inbound | S-07 3 s from Journey (see journey LLD O-3 on the S-03 budget); S-08 500 ms from Quotation |
| customer-service snapshot read | 500 ms, 1 retry → `503 UPSTREAM_UNAVAILABLE` |
| Configuration | 300 ms, cached to TTL (60 s), fail closed |
| Evaluation write failure | nothing committed; Journey stage unchanged |

## 11. Open items

| # | Item | Owner |
|---|---|---|
| O-1 | Compliance approval of each class rule set and `SUITABILITY.GATE` content (OPEN-SUIT-01, -04) | **Shailja** |
| O-2 | Savings/ULIP: rule-pack arithmetic (seeded) vs BRD insurer grid (OPEN-SUIT-06) | Rajal + Shailja |
| O-3 | Where the BRD "recommended product set" lives now that `mapping_run` is superseded (catalogue at quote time is proposed) | Rajal |
| O-4 | Suitability PDF generation path and store (SUIT-R22) | Mahesh |
| O-5 | Edit-lock duration — no source value; left unset | Rajal |
| O-6 | `CNS-SOL` before suitability (SUIT-R21) vs D5 | **Shailja** |
| O-7 | OTP for disclosure acknowledgement: reuse consent-service challenges or call the OTP provider directly | Mahesh |
