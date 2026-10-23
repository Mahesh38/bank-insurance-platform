# consent-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Mahesh (architecture) and Shailja (Compliance) review. GAP-006 stays OPEN (C8).
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Consent rule pack](../../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md) · [Scenario 08](../../api-reference/scenarios/08-consent-otp.md) ·
[Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

**Skeleton** (6 main / 2 test classes, placeholder `V1`). 0 / 8 operations. Maturity ≈ 5 %.

## 2. Scope for this build

All 8 operations, append-only evidence chain, challenge/OTP flow, withdrawal, expiry sweep, outbox,
under the **strict** posture:

| Strict rule | Effect on this service |
|---|---|
| C1 | `CONSENT.POLICY` requires `CNS-DP` + `CNS-SOL` at `BEFORE_SUITABILITY`; `BEFORE_MULTI_QUOTE` = `CNS-QSH` + still-valid DP/SOL. The service just evaluates whatever checkpoint the published rule set defines — no checkpoint names hard-coded |
| C2 | `verificationModes = ["CUSTOMER_DEVICE"]`; `RM_RELAYED` → `403 FORBIDDEN` |
| C3 | `CNS-DP` and `CNS-SOL` captured as **separate affirmative actions** (`separateActions=true`) — one OTP challenge may not grant both in one acknowledgement (CNS-1) |
| C7 | A purpose whose statement is not `APPROVED` cannot be captured → refuse (`CONFIGURATION_UNRESOLVABLE`) |

The seed `CONSENT.POLICY.json` in the engine still carries the D5 shape for some checkpoints;
round 2 is updating it. Code reads checkpoints from the resolved version only.

## 3. Operations

[openapi.yaml](./openapi.yaml): `checkConsents`, `startCapture`, `listConsentChains`,
`getChallenge`, `verifyChallenge`, `resendOtp`, `getConsent`, `startWithdrawal` — all to build.

## 4. Data owned

Schema `consent` — design [`05-consent.sql`](../../data-architecture/schemas/05-consent.sql)
plus the LLD §6 deltas: `consent` (PK `(consent_id, record_version)`, INSERT-only, one row per
purpose), new `consent_challenge` (the only updatable table), `idempotency_record`, `outbox_event`,
`processed_event`. Grants: `app_consent` INSERT/SELECT only on `consent` (CNS-R16) — prove on PostgreSQL.

## 5. Rule sets and configuration

| Rule set / config | Use |
|---|---|
| `CONSENT.POLICY` (`COLLECT`, `pinning: JOURNEY`) | Required purposes, `validityDays`, scope per checkpoint; `reuseAcrossJourneys`, `bindTo`, `verificationModes`, `separateActions`, OTP limits (`otp.validityMinutes` 10, `maxAttempts` 3, `maxIssuesPer24h` 5 per CNS-R14/R15) |
| Consent statements (`CONSENT_STATEMENT`) | Text, version, `status`; storage not contracted (administration-config CFG-2) — stub the `StatementPort` with DRAFT-status fixtures so C7 refusal is testable |

## 6. Events

Produced: `ConsentGranted`, `ConsentWithdrawn` (catalogue), `ConsentExpired` (LLD §8 — not in the
catalogue yet, CNS-5), `AuditRecorded` for every capture / verify / withdrawal / refusal. Never
publish the OTP txn id. Consumed: `RuleSetPublished`.

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists — stub contract |
|---|---|---|
| administration-config (policy + statements) | 300 ms, cached, fail closed | Test rule set from the seed; statement fixtures |
| customer-service `getRegisteredContact` | 1 s, no retry → `503` | Stub per [customer openapi](../customer-service/openapi.yaml) (`200` contact / `404` / `503`) |
| notification-service `sendNotification` (`CONSENT_OTP`, sync) | read 5 s (notification LLD §4.1) | Stub per [notification openapi](../notification-service/openapi.yaml): `200 SENT`, `503`, `504` |
| OTP verify provider | 1 s | Behind `OtpVerifierPort`; which provider verifies is CNS-2 |

Inbound: journey S-06 2 s; quotation / proposal `consents:check` 500 ms.

## 8. Invariants

`INV-CNS-01` evidence write-once (grants) · `INV-CNS-02` `GRANTED` only from a verified OTP ·
CNS-R10 no consent without OTP · CNS-R11 OTP only to CBS mobile (never from the body) · CNS-R13 / C2
customer device only · CNS-R18 one head per chain · CNS-R24 bound to one subject · CNS-R26
supersede chain · CNS-R37 fail closed in one transaction.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-CNS-01 | `check(BEFORE_SUITABILITY)` for a new customer → `satisfied=false`, missing `CNS-DP`, `CNS-SOL` (with the strict policy version) | C1 |
| AT-CNS-02 | Valid DP/SOL day 90 → valid; day 91 → missing `EXPIRED` at `BEFORE_MULTI_QUOTE` | D5 validity, seed test |
| AT-CNS-03 | Capture `CNS-DP` and `CNS-SOL` together as one acknowledgement → refused; two separate verified actions → two consent rows | C3, OPEN-CNS-01 |
| AT-CNS-04 | Verify with `RM_RELAYED` → `403 FORBIDDEN`; with `CUSTOMER_DEVICE` → `GRANTED` | C2, CNS-R13 |
| AT-CNS-05 | Capture a purpose whose statement is `DRAFT` → refused, no challenge, no OTP sent | C7 |
| AT-CNS-06 | Wrong OTP → `422` with `attemptsRemaining`; 4th attempt → `429 OTP_ATTEMPTS_EXCEEDED`, challenge `LOCKED`; expired → `409 OTP_EXPIRED` | CNS-R14/R15 |
| AT-CNS-07 | Sixth OTP issue in 24 h for a purpose → `429 OTP_ISSUE_RATE_EXCEEDED` | CNS-R15 |
| AT-CNS-08 | On PostgreSQL `app_consent` cannot UPDATE / DELETE `consent` | `INV-CNS-01`, CNS-R16 |
| AT-CNS-09 | New grant for a purpose with a live chain → new row + `SUPERSEDED` row on the old chain, one transaction | CNS-R26 |
| AT-CNS-10 | `getConsent` with another customer's id → `403 CONSENT_SUBJECT_MISMATCH` + security event | CNS-R38 |
| AT-CNS-11 | Withdrawal verified → `WITHDRAWN` row, `ConsentWithdrawn` event, no OTP txn id on the bus | CNS-R27/R28 |
| AT-CNS-12 | customer-service down during capture → `503`, nothing written | CNS-R37 |
| AT-CNS-13 | `CONSENT.POLICY` unresolvable / unknown checkpoint → `422 CONFIGURATION_UNRESOLVABLE` | ADR-024 |
| AT-CNS-14 | Expiry sweep is idempotent per `(consent_id, record_version)`; check never depends on the sweep | LLD §3.4 |

## 10. Build order

**Tier 4** (with suitability). Needs administration-config, customer, notification — all stubbable.
Startable now.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| CNS-1 | How C3 "separate affirmative actions" is expressed on the wire (two challenges vs one challenge with per-purpose confirmation) — the contract shows one challenge for several purposes | Mahesh + Shailja | DP/SOL capture flow (build the rest; refuse combined capture meanwhile) |
| CNS-2 | Customer-device confirmation surface (SMS link page / bank app) not designed; OTP verify provider not named | Mahesh + Rajal | End-to-end `CUSTOMER_DEVICE` verify |
| CNS-3 | Statement storage and approval (C7; administration-config CFG-2); wording incl. `CNS-QSH` | Mahesh; Shailja + Legal | Any capture at go-live |
| CNS-4 | **GAP-006** human Compliance signature on the consent rule pack (C8) | Shailja | Go-live (S11) |
| CNS-5 | `ConsentExpired` schema in the catalogue | Events owner | Publishing it |
| CNS-6 | Register `OTP_EXPIRED`, `OTP_ATTEMPTS_EXCEEDED`, `OTP_ISSUE_RATE_EXCEEDED`, `CONSENT_SUBJECT_MISMATCH` in `ErrorCodes` if not yet present | bank-common-error owner | Exact codes |
