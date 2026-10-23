# consent-service — Low-level design (contract companion)

**Status:** AI-DRAFTED 2026-10-22 from owner decisions D5 (`D-024`), D3, ADR-024/025/027 and the
compliance-strict decisions C1–C3, C7 ([DECISIONS-2026-10-22](../../architecture-review/DECISIONS-2026-10-22.md)),
awaiting Mahesh (architecture) and Shailja (Compliance) review. Consent rule pack content is
"ratification pending" (GAP-006); nothing here substitutes for Shailja's E2 signature.
Wire contract: [`openapi.yaml`](./openapi.yaml).

Sources: [DECISIONS-2026-10-21 §D5, §D3](../../architecture-review/DECISIONS-2026-10-21.md) ·
[ADR-024](../../architecture-review/ADR-024-configuration-driven-rule-engine.md) ·
[ADR-025](../../architecture-review/ADR-025-event-specification-and-publisher.md) ·
[consent rule pack](../../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md) ·
[domain model](../../ws3-platform/01-domain-model-and-invariants.md) (§4.2, INV-CNS-01/02) ·
[DDL `05-consent.sql`](../../data-architecture/schemas/05-consent.sql) ·
[API reference page](../../api-reference/services/consent-service.md) ·
[R0 HLD §5.2](../../../architecture/R0-HLD.md).

## 1. Purpose and ownership

consent-service (#6, L5, port 8092) owns the customer's consents and their **write-once evidence**.
It answers one question for every other context — *does this customer hold valid consent for the
purposes this checkpoint needs?* — and captures what is missing with an OTP on the customer's
registered mobile. It does **not** decide where in the journey a checkpoint sits (journey), nor
which purposes a checkpoint needs or for how long (rule set `CONSENT.POLICY`, approved by Compliance).

Ordering (**C1, compliance-strict**, seed `CONSENT.POLICY`): **`CNS-DP` + `CNS-SOL` at
`BEFORE_SUITABILITY`** (suitability collects income, dependants and liabilities, so DPDP consent
comes first, as the rule pack §2 says) → suitability → **`BEFORE_MULTI_QUOTE`: `CNS-QSH` plus a
re-check that `CNS-DP`/`CNS-SOL` are still valid** (90-day reuse; only what is missing is captured —
the owner's D5 flow) → quoting; `CNS-SHR` at `BEFORE_PROPOSAL_SUBMIT`; `CNS-COM` at
`BEFORE_FIRST_COMMUNICATION`. If Compliance confirms the owner's order (consent after the
questionnaire) is permissible, the relaxation is a `CONSENT.POLICY` version that removes
`CNS-DP`/`CNS-SOL` from `BEFORE_SUITABILITY` — no code change.

Further strict seeds: **C2** verification only on the customer's own device
(`verificationModes = [CUSTOMER_DEVICE]`); **C3** each purpose is a separate affirmative action
(`separateActions = true`); **C7** a purpose whose statement is not `APPROVED` cannot be captured
(`requiredStatementStatus = APPROVED`).

## 2. API summary

| Operation | Path | Caller | Notes |
|---|---|---|---|
| `checkConsents` | `GET /internal/v1/consents:check?customerId&checkpoint&lob[&insurerCode][&journeyId]` | Journey (S-06), Proposal (C2 at submit) | Read-only; `satisfied`, `valid[]`, `missing[]` |
| `startCapture` | `POST /internal/v1/consents` | Journey | One challenge + one OTP for the missing purposes |
| `getChallenge` | `GET /internal/v1/consent-challenges/{challengeId}` | Journey / BFF | RM sees pending / verified only (CNS-R13) |
| `verifyChallenge` | `POST /internal/v1/consent-challenges/{challengeId}/verify` | Customer-device surface; RM session only if `RM_RELAYED` is permitted | Writes evidence |
| `resendOtp` | `POST /internal/v1/consent-challenges/{challengeId}/otp:resend` | Journey | CNS-R15 limits |
| `getConsent` | `GET /internal/v1/consents/{consentId}` | Journey, Proposal, Audit | Head row of the chain |
| `listConsentChains` | `GET /internal/v1/consents?customerId=` \| `?journeyId=` | Ops, Audit | CNS-R19 retrieval |
| `startWithdrawal` | `POST /internal/v1/consents/{consentId}/withdrawals` | BFF via Journey / ops | OTP-verified (CNS-R27) |

The HLD-contracted `POST /internal/v1/consents/{id}/verify` becomes
`POST /internal/v1/consent-challenges/{challengeId}/verify`, because one OTP verifies several
separately recorded consents (rule pack §2 "one acknowledgement screen, three rows").

## 3. Flows

### 3.1 Check (`consents:check`)

1. Validate ids, `lob`, `checkpoint` format.
2. Resolve `CONSENT.POLICY` (pinned version for `journeyId` if the rule set says
   `"pinning": "JOURNEY"`, else in force) through the configuration port (S-21, 300 ms, cached to
   TTL, fail closed). Unknown checkpoint → `422 CONFIGURATION_UNRESOLVABLE`.
3. Engine evaluation, `hitPolicy: COLLECT`. Inputs: `asOf`, `checkpoint`, the grant date of the
   head `GRANTED` row per purpose (`dpGrantedOn` … `comGrantedOn`), `shrInsurerCode`,
   `targetInsurerCode`, and the current statement status per purpose (`dpStatementStatus` …).
   Outcome `NO_CAPTURE_REQUIRED` / `CAPTURE_REQUIRED` (reason codes `CAPTURE_CNS_*` name the missing
   purposes) / **`CAPTURE_BLOCKED`** (C7 — a missing purpose's statement is not `APPROVED`; reason
   `CNS_*_STATEMENT_NOT_APPROVED`). A missing statement status counts as not approved (fail closed).
4. For each required purpose, read the **head row** of the newest chain for
   `(customer_id, purpose[, insurer_code])` (`ix_consent_customer_purpose`).
5. Classify: head `GRANTED` and `valid_until > now` (or `NULL`) and scope matches and its
   statement's MAJOR version is current (CNS-R05, unless the journey is past this checkpoint) →
   `valid`; else `missing` with reason `ABSENT | EXPIRED | WITHDRAWN | SUPERSEDED |
   STATEMENT_MAJOR_CHANGE | SCOPE_MISMATCH`.
6. `satisfied = missing.isEmpty()`; `captureBlocked = (outcome == CAPTURE_BLOCKED)`. Reuse across journeys is allowed when
   `reuseAcrossJourneys = true` (CNS-R25); `bindTo = customerId` (CNS-R24 restated for D3).

### 3.2 Capture

```mermaid
sequenceDiagram
    participant JO as journey
    participant CN as consent-service
    participant CFG as administration-config
    participant CU as customer-service
    participant OTP as notification / OTP provider
    participant DEV as customer device
    JO->>CN: POST /consents {customerId, checkpoint, purposes[missing], channel, language}
    CN->>CFG: statement (id, version, text) per purpose + CONSENT.POLICY (fail closed)
    CN->>CU: GET /customers/{id}/registered-contact (CBS mobile, CNS-R11)
    CN->>CN: INSERT consent_challenge (OTP_PENDING, statement refs, contact_used_enc) + idempotency
    CN->>OTP: send OTP (10 min) → otpTxnId
    CN-->>JO: 201 {challengeId, OTP_PENDING, maskedContact, otpExpiresAt}
    DEV->>OTP: customer enters OTP on own device (CUSTOMER_DEVICE)
    OTP->>CN: POST /consent-challenges/{id}/verify {otpTxnId, otp, CUSTOMER_DEVICE}
    CN->>OTP: verify(otpTxnId, otp)
    CN->>CN: BEGIN; INSERT consent row per purpose (GRANTED, rv=1, evidence); UPDATE challenge VERIFIED; INSERT outbox ConsentGranted ×n; COMMIT
    JO->>CN: GET /consent-challenges/{id} (poll) or consumes ConsentGranted
```

1. Validate request (§5). Purposes must be a subset of the checkpoint's required purposes
   (a caller cannot capture an unrelated consent through a checkpoint).
2. Resolve statements — text is stored **verbatim** with its SHA-256 (CNS-R01, CNS-R04). **C7:** any
   statement whose status is not `requiredStatementStatus` (`APPROVED`) → `403
   CONSENT_STATEMENT_NOT_APPROVED`, no challenge, compliance event. Every statement is a draft until
   Compliance approves it, so capture is refused end to end until then — by design.
3. Resolve the CBS-registered mobile (never from the body). Customer-service down → `503`.
4. `INSERT consent_challenge` (state `OTP_PENDING`, attempts 0, `otp_issue_count` +1, expiry 10 min).
   Rate limit: >5 issues per purpose per customer per 24 h → `429 OTP_ISSUE_RATE_EXCEEDED`.
5. Dispatch OTP via notification-service (S-18 is OTP-only in R0). The OTP value is never returned.
6. **Verify:** challenge must be `OTP_PENDING` (else `409 ILLEGAL_TRANSITION`, INV-CNS-02); not
   expired (`409 OTP_EXPIRED`); attempt ≤ 3 (4th → `429 OTP_ATTEMPTS_EXCEEDED`, challenge
   `LOCKED`); wrong OTP → `422 VALIDATION_ERROR` with `attemptsRemaining`.
   `verificationMode` must be in `CONSENT.POLICY.parameters.verificationModes` (strict seed C2:
   `["CUSTOMER_DEVICE"]`) else `403 FORBIDDEN`. **C3:** with `separateActions = true` the body's
   `affirmations[]` must contain one entry per purpose of the challenge, each naming the statement
   rendered for it; one acknowledgement can never grant `CNS-DP` and `CNS-SOL` together — otherwise
   `422 VALIDATION_ERROR` and nothing is written. The single OTP authenticates the customer; the
   affirmations are the consents.
7. One transaction: per purpose `INSERT consent` (`record_version = 1`, `state = GRANTED`,
   `valid_from = otp_verified_at`, `valid_until = valid_from + validityDays` or `NULL`,
   `supersedes_consent_id` = previous chain head for the same purpose/scope if any) and, for that
   previous chain, `INSERT` its next row with `state = SUPERSEDED` (CNS-R26); challenge →
   `VERIFIED`; outbox `ConsentGranted` per consent. Any failure → nothing commits and the caller's
   step fails (CNS-R37).

### 3.3 Withdrawal

1. `startWithdrawal`: head row must be `GRANTED` (else `409 ILLEGAL_TRANSITION`); create a
   `WITHDRAWAL` challenge (same OTP rules).
2. Verify → `INSERT consent` next `record_version` with `state = WITHDRAWN`, `withdrawn_at`,
   `withdrawn_by`, `withdrawal_channel`, `withdrawal_reason_code`, `withdrawal_otp_txn_id`
   (CNS-R28); outbox `ConsentWithdrawn {consentId, customerId, purpose, insurerCode}`.
3. Journey consumes it: `CNS-DP`/`CNS-SOL`/`CNS-QSH` → halt (CNS-R29); `CNS-SHR` after submission →
   ops task `CONSENT_WITHDRAWN_POST_SUBMISSION` (CNS-R30). Suitability consumes it for `CNS-SOL`
   (SUIT-R30) only if Compliance keeps that rule under D5.

### 3.4 Expiry

The check (§3.1) treats `valid_until ≤ now` as expired at read time — correctness never depends on
a job. A sweeper (ShedLock / `SKIP LOCKED`, every 15 min) appends an `EXPIRED` row for heads whose
`valid_until` passed and emits `ConsentExpired` (idempotent by `(consent_id, record_version)`).
Pending challenges older than their expiry + 24 h → `ABANDONED`.

## 4. Rule-engine usage

| Rule set | Hit policy | Inputs | Output used |
|---|---|---|---|
| `CONSENT.POLICY` | `COLLECT` (`outcomeRank`: `CAPTURE_BLOCKED` > `CAPTURE_REQUIRED` > `NO_CAPTURE_REQUIRED`) | §3.1 step 3 | outcome + `CAPTURE_CNS_*` / `CNS_*_STATEMENT_NOT_APPROVED` reason codes; parameters `checkpoints.<cp>.{purposes, validityDays, scope}`, `verificationModes` (`[CUSTOMER_DEVICE]`, C2), `separateActions` (`true`, C3), `requiredStatementStatus` (`APPROVED`, C7), `reuseAcrossJourneys`, `bindTo` |

OTP limits are seed parameters too (E7 — no compiled-in values): `otp.validityMinutes` 10
(CNS-R14), `otp.maxAttempts` 3 and `otp.maxIssuesPer24h` 5 (CNS-R15).

The OTP limits are seeded from CNS-R14/R15 as parameters; the statement texts are configuration
domain `CONSENT_STATEMENT` (CNS-R02/R03, Compliance-classified MAJOR/MINOR). The rule-set document
format and the engine are owned by `libs/bank-rule-engine` (ADR-024 §2).

## 5. Validation rules

| Field | Rule | Error code |
|---|---|---|
| `customerId`, `leadId`, `journeyId`, path ids | ULID | `422 VALIDATION_ERROR` |
| `checkpoint` | known in the resolved `CONSENT.POLICY` | `422 CONFIGURATION_UNRESOLVABLE` |
| `purposes` | non-empty, unique, each required by `checkpoint` | `422 VALIDATION_ERROR` |
| `insurerCode` | mandatory with `CNS-SHR` | `422 MISSING_REQUIRED_FIELD` |
| `channel` | `RM_ASSISTED` \| `SELF_SERVICE` \| `HYBRID` (DDL vocabulary; the rule pack's `ASSISTED/DIY` maps to these) | `422 VALIDATION_ERROR` |
| `language` | `en-IN` (R0) | `422 VALIDATION_ERROR` |
| `otp` | 4–8 digits; correct | `422 VALIDATION_ERROR` |
| `verificationMode` | permitted by policy (`CUSTOMER_DEVICE` only, C2) | `403 FORBIDDEN` |
| `affirmations` | one per purpose of the challenge, statement matches (C3) | `422 VALIDATION_ERROR` |
| statement status per purpose | `APPROVED` (C7) | `403 CONSENT_STATEMENT_NOT_APPROVED` |
| `customerId` query on `getConsent` | equals the consent's subject | `403 CONSENT_SUBJECT_MISMATCH` + security event (CNS-R38) |
| challenge state / OTP age / attempts | §3.2 step 6 | `409 ILLEGAL_TRANSITION`, `409 OTP_EXPIRED`, `429 OTP_ATTEMPTS_EXCEEDED`, `429 OTP_ISSUE_RATE_EXCEEDED` |

## 6. DTO → domain → table mapping

| DTO field | Domain | Table.column |
|---|---|---|
| `challengeId`, `kind`, `state`, `attemptsRemaining`, `otpExpiresAt` | `ConsentChallenge` | **new** `consent.consent_challenge` (`challenge_id`, `kind`, `state`, `attempts`, `otp_issue_count`, `otp_expires_at`, `otp_txn_id`, `purposes`, `statement_refs` JSONB, `contact_used_enc`, `customer_id`, `journey_id`, `lead_id`, `checkpoint`, `insurer_code`, `target_consent_id` for withdrawal) |
| `consentId`, `recordVersion` | `Consent` chain id + row version | `consent.consent_id`, **new** `record_version` (PK becomes `(consent_id, record_version)`) |
| `customerId` | `Consent.subject` (D3) | `customer_id` — **`cif_enc` is removed** (CIF lives only in schema `customer`) |
| `purpose` | `Purpose` | `purpose` **`TEXT[]` → `VARCHAR(16)`** (one row per purpose) + CHECK on the five codes |
| `insurerCode` | `Scope` | **new** `scope_insurer_code` |
| `statementId`, `statementVersion`, text, hash, `language` | `Statement` (value) | **new** `statement_id`, `statement_version` (widen to `VARCHAR(32)`), `statement_text`, **new** `statement_text_hash`, **new** `language` |
| `channel`, `acknowledgementMethod` | | `channel` (CHECK add `HYBRID`), **new** `acknowledgement_method` |
| `checkpoint` | | **new** `checkpoint` |
| `capturedAt`, OTP evidence | `Evidence` | `captured_at`, `otp_txn_id`, **new** `otp_verified_at`, `source_ip`, `contact_used_enc`, **new** `captured_by_actor_id`, **new** `captured_by_agent_id` |
| `validFrom`, `validUntil` | `Validity` | **new** `valid_from`, `valid_until` (**nullable** for `CNS-COM`) |
| `state` | | `state` CHECK becomes `GRANTED`, `WITHDRAWN`, `EXPIRED`, `SUPERSEDED` (`REQUESTED`/`OTP_PENDING`/`ABANDONED` move to `consent_challenge`) |
| `supersedesConsentId` | | **new** `supersedes_consent_id` |
| withdrawal fields | `Withdrawal` | `withdrawn_at`, `withdrawn_by`, **new** `withdrawal_channel`, `withdrawal_reason` → `withdrawal_reason_code`, **new** `withdrawal_otp_txn_id` |
| policy version | | `config_version_ref` (`CONSENT.POLICY@version`) |
| Events | | **new** `consent.outbox_event` (ADR-025 columns incl. `parked_at`) |

Other DDL changes: drop `NOT NULL` problems at `REQUESTED` by moving pending state out (above);
`journey_id` nullable stays (consent is reusable across journeys); new index
`ix_consent_customer_purpose (customer_id, purpose, scope_insurer_code, consent_id, record_version DESC)`;
partial unique "one head per chain" is enforced by `PRIMARY KEY (consent_id, record_version)` +
INSERT-only; **grants: `app_consent` gets INSERT/SELECT only on `consent` (CNS-R16)**, UPDATE only
on `consent_challenge`.

## 7. Invariants

| Invariant | Enforced at |
|---|---|
| INV-CNS-01 evidence write-once | DB grants (INSERT/SELECT only) — not code (CNS-R16) |
| INV-CNS-02 `GRANTED` only from a verified OTP | `ConsentChallenge.verify` |
| CNS-R10 no consent without verified OTP | No code path inserts `consent` outside `verify` |
| CNS-R11 OTP to CBS-registered mobile | contact resolved server-side only |
| CNS-R13 RM cannot enter a customer-device OTP | `verificationModes = [CUSTOMER_DEVICE]` (C2); RM surface shows status only |
| OPEN-CNS-01 separate affirmative actions | `separateActions = true` (C3); `verifyChallenge` checks `affirmations[]` |
| C7 no capture of an unapproved statement | `startCapture` step 2; `consents:check` returns `captureBlocked` |
| CNS-R18 one head per chain | PK `(consent_id, record_version)`; next version computed under row lock of the head |
| CNS-R24 bound to one subject | `customer_id` immutable; mismatch → `403 CONSENT_SUBJECT_MISMATCH` |
| CNS-R37 fail closed | single transaction; any failure → caller's step fails |

## 8. Events

Produced (ADR-025 envelope, topic `{env}.consent.consent.v1`, key `consentId`; `data` ids only —
no CIF, no OTP txn id on the bus):

| Event | `data` | Consumers |
|---|---|---|
| `ConsentGranted` | `consentId`, `customerId`, `purpose`, `checkpoint`, `statementVersion`, `insurerCode`, `validUntil` | Journey, Audit |
| `ConsentWithdrawn` | `consentId`, `customerId`, `purpose`, `reasonCode`, `withdrawnAt` | Journey, Suitability, Audit, Notification |
| `ConsentExpired` | `consentId`, `customerId`, `purpose` | Journey (informational), Audit |

Schemas: `docs/platform/contracts/events/schemas/ConsentGranted.v1.json` etc. That schema keeps
`otpTxnId` as an optional field; this service **does not populate it** — the OTP transaction id is
Confidential (rule pack §5 #13) and stays in the evidence store.
Consumed: `RuleSetPublished` (cache invalidation, ADR-024 §5).

## 9. Idempotency, timeouts, failure modes

| Aspect | Value |
|---|---|
| Keys | `journey:{journeyId}:consent:{checkpoint}:{purposeSetHash}` (capture), `challenge:{challengeId}:verify`, `consent:{consentId}:withdraw` — `consent.idempotency_record`, 24 h |
| Inbound budget | S-06: 2 s from Journey, fail closed |
| Configuration (S-21) | 300 ms, no retry, cached to TTL; unresolvable → refuse |
| customer-service contact | 1 s, no retry → `503 UPSTREAM_UNAVAILABLE` |
| OTP dispatch / verify | 1 s, no retry on send (a resend is an explicit user action) |
| Evidence write failure | `503`, nothing committed, step fails (CNS-R37) |

## 10. Retention

`RET-7Y-IMMUTABLE` from the journey's terminal state (CNS-R33); WORM archive under ADR-026
(**PROPOSED**). Consent is never purged when a sale does not happen (F-02).

## 11. Open items

| # | Item | Owner |
|---|---|---|
| O-1 | **Decided strict (C2)**: `verificationModes = ["CUSTOMER_DEVICE"]`; R0-HLD §5.2's RM read-back is refused. `RM_RELAYED` is a Compliance-approved configuration change only | Shailja (relaxation only) |
| O-2 | The customer-device confirmation surface (SMS link page / bank app) is not designed; until it exists `CUSTOMER_DEVICE` cannot be exercised end to end | Mahesh + Rajal |
| O-3 | **Decided strict (C1)**: `CNS-DP`/`CNS-SOL` before suitability. Compliance may confirm the owner's order to relax it (configuration) | Shailja (relaxation only) |
| O-4 | Statement wording for all purposes incl. new `CNS-QSH` (OPEN-CNS-02) — **blocking (C7)**: no capture until approved | Shailja + Legal |
| O-5 | 90 / 30-day windows regulator-acceptable (OPEN-CNS-03) | Shailja |
| O-6 | `CONFIGURATION_UNRESOLVABLE` is 422 in `ErrorCatalogue`, ADR-024 §5 says 503 | Mahesh |
| O-7 | ~~Journey halt stage/code for CNS-R29~~ — **accepted (E4)**: stage `HALTED_CONSENT_WITHDRAWN`, error `JOURNEY_HALTED_CONSENT_WITHDRAWN` (registered) | closed |
