# suitability-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Mahesh (architecture), Rajal (behaviour) and Shailja (Compliance) review. GAP-007 stays OPEN (C8).
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Suitability rule pack](../../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md) ·
[Scenario 07](../../api-reference/scenarios/07-journey-start-and-suitability.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

**Skeleton** (6 main / 3 test classes, placeholder `V1`). 0 / 10 operations. Rule sets
`SUITABILITY.LIFE.TERM/.SAVINGS/.ULIP` and `SUITABILITY.GATE` are seeded (DRAFT) and pass their
test cases in `libs/bank-rule-engine`. Maturity ≈ 5 % (≈ 25 % counting the seeds).

## 2. Scope for this build

All 10 operations: assessment lifecycle (create / resume / supersede), encrypted answer sets,
evaluation of **every LIFE class** per D4, gate read `evaluations:usable` with the eight ordered
checks, acknowledgements, usage, lock, expiry / invalidation sweep. Term first, then Savings/ULIP
(same code, different rule sets). Strict posture: **C5** (only `SUITABLE`; `SUITABLE_WITH_CAUTION`
only with acknowledgement on the customer's device) and **C6** (rule-pack gaps → `INSUFFICIENT_DATA`
→ no quote) are rule-set content; the service must not special-case them.

Not built: override (SUIT-R38, no R0 override), PDF generation (SUIT-O4).

## 3. Operations

[openapi.yaml](./openapi.yaml): `createAssessment`, `getAssessment`, `getQuestionSet`,
`saveAnswers`, `evaluate`, `getEvaluation`, `acknowledgeDisclosure`, `recordUsage`,
`lockEvaluation`, `getUsableEvaluation`.

## 4. Data owned

Schema `suitability` — design [`06-suitability.sql`](../../data-architecture/schemas/06-suitability.sql)
with LLD §7 deltas: `suitability` (header), `suitability_answer_set` (encrypted), `evaluation`,
`evaluation_outcome` (PK `(evaluation_id, product_class)`), `disclosure_acknowledgement`,
`evaluation_usage`, `idempotency_record`, `outbox_event`. INSERT-only evaluation tables by grant
(SUIT-R23); partial unique `(lead_id, product_class) WHERE state='IN_PROGRESS'` (PostgreSQL proof).

## 5. Rule sets

| Rule set | Policy | Use |
|---|---|---|
| `SUITABILITY.LIFE.TERM` / `.SAVINGS` / `.ULIP` | `PRIORITY`, `ALL_FIRED`, `pinning: JOURNEY` | Per-class outcome; `missingInputOutcome`/`noMatchOutcome` = `INSUFFICIENT_DATA` |
| `SUITABILITY.GATE` | parameters | `allowedOutcomes`, `cautionRequiresAcknowledgement`, `validityDays` (seed 30), `editLockMinutes` (unset — SUIT-5) |

Thresholds live only in these rule sets; content awaits Shailja / Rajal.

## 6. Events

Produced: `SuitabilityCompleted` (outcome per class), `SuitabilityOverridden` (catalogue; unused in
R0), plus LLD §9 events **not yet in the catalogue** — `SuitabilityDisclosureAcknowledged`,
`SuitabilityGateRefused`, `SuitabilityExpired`, `SuitabilityInvalidated`, `SuitabilitySuperseded`
(SUIT-6) — and `AuditRecorded` for every evaluation and every gate refusal (SUIT-R28). Note: the
catalogue topic is `suitability.suitability-evaluation.v1`, the LLD says `suitability.assessment.v1`
— the catalogue wins (SUIT-7). Consumed: `ConsentWithdrawn` (`CNS-SOL` → invalidate, SUIT-R30),
`RuleSetPublished` (cache + SUIT-R32).

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists — stub contract |
|---|---|---|
| customer-service `getSnapshot` (derived SQ-D01/D02/D04) | 500 ms, 1 retry → `503` | Stub per [customer openapi](../customer-service/openapi.yaml), including a snapshot with null `ageNextBirthday` |
| administration-config | 300 ms, cached, fail closed | `RuleSetSeeds` |
| identity-authorization PDP `suitability.perform` + SP certification | 300 ms | Stub; action not yet in the PDP catalogue (identity AUTHZ-2) |
| OTP verification for acknowledgements | — | Port; mechanism open (SUIT-8) |

Inbound: journey S-07 3 s; quotation S-08 **500 ms**, primary reads only, no cache that can allow on a miss.

## 8. Invariants

`INV-SUI-01` COMPLETED immutable (`409 ASSESSMENT_IMMUTABLE`) · `INV-QUO-01` as amended by D4 (gate
read) · SUIT-R05 deterministic (`inputs_hash`) · SUIT-R19a all reason codes in rule order ·
SUIT-R36 never across customers (+ security event) · SUIT-R38–R40 no override, gate not disableable ·
SUIT-R27 **100 % branch coverage** of the gate class.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-SUIT-01 | Reference customer → TERM `SUITABLE`, `recommendedCover = 25 600 000`, result bytes identical on re-run | `SUIT-TC-REF-01`, SUIT-R05 |
| AT-SUIT-02…08 | The seven gate cases of rule pack §6.2 each give `403 SUITABILITY_REQUIRED` with the matching `refusalReason`, a `SuitabilityGateRefused` + `AuditRecorded` each | Rule pack §6.2, SUIT-R28 |
| AT-SUIT-09 | `SUITABLE_WITH_CAUTION` without acknowledgement → `CAUTION_NOT_ACKNOWLEDGED`; after a customer-device acknowledgement → usable | C5 |
| AT-SUIT-10 | Savings with affordability > 0.20 (or `WEALTH_GROWTH`) → `INSUFFICIENT_DATA / NO_RULE_MATCHED` → gate refuses | C6, rule-engine LLD §14 #2 |
| AT-SUIT-11 | `saveAnswers` on `COMPLETED` → `409 ASSESSMENT_IMMUTABLE`; correction via `supersedesSuitabilityId` leaves the predecessor usable until the new one is evaluated | `INV-SUI-01`, LLD §3 |
| AT-SUIT-12 | Evaluate with the own class set incomplete → `422 SUITABILITY_INCOMPLETE`, `errors[].field = SQ-xx`, nothing written | SUIT-R01a |
| AT-SUIT-13 | ULIP outcome has no `protectionGap` derived | D4 |
| AT-SUIT-14 | Gate read with another customer's id → `SUITABILITY_SUBJECT_MISMATCH` + security event | SUIT-R36 |
| AT-SUIT-15 | `ConsentWithdrawn(CNS-SOL)` → evaluation `INVALIDATED` (not when `LOCKED`) | SUIT-R30 |
| AT-SUIT-16 | Rule set unresolvable at evaluate → `CONFIGURATION_UNRESOLVABLE`, nothing written | ADR-024 |
| AT-SUIT-17 | Gate class JaCoCo branch coverage = 100 % | SUIT-R27 |
| AT-SUIT-18 | On PostgreSQL the evaluation tables refuse UPDATE/DELETE by grant | SUIT-R23 |

## 10. Build order

**Tier 4** (with consent). Needs administration-config, customer, PDP — all stubbable. Quotation
depends on its gate read. Startable now.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| SUIT-1 | **GAP-007** human Compliance signature on the suitability rule pack (C8); approval of each class rule set and the gate | Shailja | Go-live (S11) |
| SUIT-2 | Savings/ULIP model: rule-pack arithmetic (seeded) vs BRD insurer grid (`OPEN-SUIT-06`) | Rajal + Shailja | Savings/ULIP content only |
| SUIT-3 | C1 vs LLD: under strict C1 journey checks `BEFORE_SUITABILITY` (DP+SOL) before `createAssessment`; suitability still does not check consent itself | Mahesh | Nothing (journey enforces) |
| SUIT-4 | PDF generation path and store (SUIT-R22) | Mahesh | Customer copy at go-live |
| SUIT-5 | `editLockMinutes` value — no source; unset | Rajal | Edit lock (fail closed: no lock expiry → lock held until completion) |
| SUIT-6 | Catalogue entries for the five extra suitability events | Events owner | Publishing them |
| SUIT-7 | Topic name LLD vs catalogue | Events owner | Nothing (catalogue followed) |
| SUIT-8 | OTP for disclosure acknowledgement: reuse consent challenges or call the provider directly | Mahesh | Acknowledgement verify |
| SUIT-9 | Register `SUITABILITY_INCOMPLETE` in `ErrorCodes` if not yet present | bank-common-error owner | Exact code |
