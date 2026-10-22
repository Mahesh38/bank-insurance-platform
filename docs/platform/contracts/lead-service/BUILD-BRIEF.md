# lead-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Rajal (product), Deepali (security) and Amit (engineering) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Service page](../../api-reference/services/lead-service.md) · [Scenario 05](../../api-reference/scenarios/05-lead-creation-and-dedupe.md) ·
[Scenario 06](../../api-reference/scenarios/06-lead-onboarding-and-assignment.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

| Area | State |
|---|---|
| Code | **Partial-implemented** (38 main / 12 test classes): create (PDP, idempotency, dedupe), list, get, onboarding, assign, close, info |
| Contract operations | 7 / 7 coded |
| Persistence | Flyway V1–V4 + vendor `V5__lead_dedupe_open` (partial unique on PostgreSQL) |
| Rules | `LEAD.EXCEPTION` evaluated by **`StubExceptionRules`** (`lead.exception-rules=stub`, refused under prod/uat) — the engine adapter is not built; `bank-rule-engine` not a dependency |
| Events | `LeadCreated`, `LeadExceptionEvaluated`, `LeadAssigned`, `LeadClosed` written as attribute payloads; no relay; envelope pending |
| Not built | Exception approval (E6), expiry sweep (E7 `LEAD.POLICY`), `LeadQualified`, BI-generated / convert / archive, `JourneySold` consumer |

Maturity estimate: ≈ 55 %.

## 2. Scope for this build

1. `ExceptionRulesPort` adapter over `bank-rule-engine`: resolve published `LEAD.EXCEPTION`,
   `PRIORITY`, `BLOCK > APPROVAL_REQUIRED > PASS`; unset thresholds → `BLOCK / EH-CONFIG-MISSING`
   (D2; thresholds are **unset** by owner default). Data sources for `EH-INT-001…005` behind ports
   with stubs (LEAD-2). Keep the stub only under `local`.
2. Guards (LLD fix-pendings): terminal / held / blocked lead cannot be re-onboarded; a hold is
   never cleared by re-running onboarding; terminal lead cannot be assigned.
3. Expiry: `expires_at` from **`LEAD.POLICY.expiryDays` scoped by `lob`** (E7; LIFE 90 by
   engineering default, seed pending); sweep to `EXPIRED` (`SKIP LOCKED`, idempotent).
4. Outbox in envelope form + relay; `AuditRecorded` for create / onboarding / assign / close.
5. Exception approval **only after the contract adds the operation** (LEAD-1): approver must hold
   PDP `lead.exception.approve` (branch-scoped, bank employees only, E6) and a role listed in
   `LEAD.EXCEPTION` `approverRoles` for the fired rule; remarks mandatory (BRD §24.3 col. J).
6. Consume `JourneySold` (convert) when journey publishes it; `JourneyStarted` (journey placeholder).

## 3. Operations

[openapi.yaml](./openapi.yaml): `createLead`, `listLeads`, `getLead`, `startOnboarding`,
`assignLead`, `closeLead`, `serviceInfo` (coded). Exception approval / hold release: **not in the
contract** (LEAD-1).

## 4. Data owned

Schema `lead_lms` — design [`04-lead_lms.sql`](../../data-architecture/schemas/04-lead_lms.sql):
`lead`, `lead_assignment`, `lead_meeting`, `lead_follow_up`, `lead_exception_evaluation`,
`lead_exception_decision`, `idempotency_record`, `outbox_event`. Remaining drift (LLD §9 #8):
`customer_id CHAR(26)` (D3), exception evaluation tables, `processed_event` for consumers.

## 5. Rule sets

| Rule set | Use | Content |
|---|---|---|
| `LEAD.EXCEPTION` | Start Onboarding; rules 012–013 again at premium capture (owner of that call: LEAD-4) | Seed: all thresholds/actions **unset** → every lead `BLOCK` until the BRD owner sets values (round-2 default) |
| `LEAD.POLICY` (E7) | `expiryDays` per `lob` | Seed pending (LEAD-3); unresolvable → no `expires_at` is **not** acceptable — fail closed at create (`CONFIGURATION_UNRESOLVABLE`) per playbook §6 |

## 6. Events

Produced ([catalogue](../events/CATALOGUE.md)): `LeadCreated`, `LeadExceptionEvaluated`
(`exceptionReasonCodes` as array), `LeadAssigned`, `LeadQualified`, `LeadClosed`, `AuditRecorded`.
Consumed: `JourneySold` (convert), `RuleSetPublished`; `JourneyStarted` (LLD journey §3.1 step 8 —
not in catalogue yet).

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists |
|---|---|---|
| identity-authorization PDP (`lead.create`, `lead.assign`, `lead.exception.approve`) | 300 ms, fail closed | Built; E6 permission in flight |
| administration-config resolve | 300 ms, cached | `RuleSetSeeds` (`LEAD.EXCEPTION` seed exists) |
| `SpCertificationPort` | — | Fixture today; source is identity certification data (LEAD-5) |
| CBS / Equifax / DWH / insurer policy data (`EH-INT-001…005`) | not specified | Stub ports returning "fact unavailable" → rule fires `BLOCK` (fail closed) |

## 8. Invariants

`INV-LED-01` terminal accepts nothing · `INV-LED-03`/`INV-ACT-01` certified assignee ·
`INV-ACT-03` accountable SP written once · `INV-LED-07` absent not forbidden · one unfinished lead
per creator + customer + product class · `D-019` exceptions before assignment · hold released only
by an authorised approval (E6) · `QUALIFIED ⇒ bi_generated`.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-LEAD-01 | Onboarding with the seed rule set (thresholds unset) → `BLOCK`, reasons include `EH-CONFIG-MISSING`, `409 ILLEGAL_TRANSITION`, evaluation persisted with `ruleSetId@version` | D2, round-2 defaults |
| AT-LEAD-02 | With a SAMPLE test-resource rule set: age above `maxProposerAge` and action `APPROVAL_REQUIRED` → hold `APPROVAL_REQUIRED`, message renders `@AGE` | rule-engine LLD §6 |
| AT-LEAD-03 | Re-run onboarding on an `APPROVAL_REQUIRED` lead whose engine now says `PASS` → `409`, hold unchanged | LLD §3.2 step 2 |
| AT-LEAD-04 | Assign a `DISQUALIFIED` lead → `409 ILLEGAL_TRANSITION` | `INV-LED-01` |
| AT-LEAD-05 | `LEAD.EXCEPTION` unresolvable → `CONFIGURATION_UNRESOLVABLE`, nothing persisted | ADR-024 §5 |
| AT-LEAD-06 | Duplicate unfinished lead → `409 CONFLICT` with `errors[0] = {field: leadId, message: <existing ULID>}` | LLD §3.1 |
| AT-LEAD-07 | Same `Idempotency-Key` + same body → same `201`; different body → `409 IDEMPOTENCY_CONFLICT` | `INV-IDM-01` |
| AT-LEAD-08 | Create with `lob=LIFE` sets `expires_at = now + LEAD.POLICY(lob=LIFE).expiryDays`; sweep moves an overdue lead to `EXPIRED` once, emits `LeadClosed` | E7 |
| AT-LEAD-09 | (after LEAD-1) insurer partner rep approves an exception → `403`; bank admin with the permission but a role not in `approverRoles` → `403`; allowed approver without remarks → `422` | E6, BRD §24.3 |
| AT-LEAD-10 | Lead outside the actor's book → `404` (never `403`) | `INV-LED-07` |
| AT-LEAD-11 | `ux_lead_dedupe_open` rejects a second open lead on PostgreSQL | V5 vendor script |
| AT-LEAD-12 | Every state change writes its event + `AuditRecorded` in the same transaction | ADR-025, ADR-026 |
| AT-LEAD-13 | Startup with `exception-rules=stub` under `prod` refused | LLD §3.2 |

## 10. Build order

**Tier 3.** Needs PDP and administration-config (both exist). Startable now.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| LEAD-1 | Contract the exception approval / rejection operation (E6: approver roles from `LEAD.EXCEPTION`, PDP `lead.exception.approve`, mandatory remarks) | Rajal + Mahesh | Releasing any `APPROVAL_REQUIRED` hold |
| LEAD-2 | **Lead exception thresholds and per-rule actions unset**; data sources for `EH-INT-001…005` (CBS, Equifax, DWH, insurer) undefined | Exception BRD owner | Go-live — every lead is `BLOCK` |
| LEAD-3 | `LEAD.POLICY` seed (LIFE 90 days per E7) and approval | Rule-engine owner; Rajal | Expiry at go-live |
| LEAD-4 | Who re-evaluates rules 012–013 at premium capture (no caller contracted) | Mahesh | Those two rules |
| LEAD-5 | SP certification source (fixture today) | Deepali / Aarti | Go-live |
| LEAD-6 | Close reason / remarks (VAL-017…019) not in the contract | Rajal | Close completeness |
| LEAD-7 | Missing `X-Actor-Id` answers Spring 400 without the envelope | Engineering | Conformance |
