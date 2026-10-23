# consent-service

Bounded context 6. Port **8092**. Package `com.bank.platform.consent`.

The service answers whether a customer holds valid consent for a checkpoint, and captures what is missing with an OTP to the customer's registered contact. Checkpoint names, purposes, validity and OTP limits come from rule set `CONSENT.POLICY`. They are not compiled into the service.

Wire contract: [`docs/platform/contracts/consent-service/openapi.yaml`](../../docs/platform/contracts/consent-service/openapi.yaml). Where this module and an older page disagree, that contract wins, then the [LLD](../../docs/platform/contracts/consent-service/LLD.md).

This is not an RBI, IRDAI or DPDP certificate. GAP-006 (the consent rule pack signature) is still open.

## What is implemented

| Operation | Path |
|---|---|
| `checkConsents` | `GET /internal/v1/consents:check` |
| `startCapture` | `POST /internal/v1/consents` |
| `listConsentChains` | `GET /internal/v1/consents` |
| `getChallenge` | `GET /internal/v1/consent-challenges/{challengeId}` |
| `verifyChallenge` | `POST /internal/v1/consent-challenges/{challengeId}/verify` |
| `resendOtp` | `POST /internal/v1/consent-challenges/{challengeId}/otp:resend` |
| `getConsent` | `GET /internal/v1/consents/{consentId}` |
| `startWithdrawal` | `POST /internal/v1/consents/{consentId}/withdrawals` |

Each use case is its own class under `application`: `CheckConsent`, `CaptureConsent`, `VerifyConsent`, `WithdrawConsent`, `SweepConsent`, and `ReadConsent`. Commands and views are `ConsentTypes`. The HTTP controller depends on those classes.

Evidence is insert-only. A withdrawal or a replacement grant appends a row. `EXPIRED` is visible at check time from `validUntil` and, when the sweeper runs, is also appended. The original `GRANTED` row is not rewritten.

`verificationModes` is read from the policy. The seed allows `CUSTOMER_DEVICE` only, so `RM_RELAYED` is `403 FORBIDDEN`. `separateActions` refuses a capture that affirms more than one purpose in one challenge. A statement that is not `APPROVED` refuses capture and marks the check `captureBlocked`.

OTP limits (10 minutes, 3 attempts, 5 issues in 24 hours) are policy parameters. The code is hashed. It is not on the HTTP response and not in the outbox. `ConsentGranted` does not carry `otpTxnId`.

## Stubs

| Port | Until the real service exists |
|---|---|
| `CONSENT.POLICY` | Seed in `libs/bank-rule-engine` (`CONSENT.POLICY.json`). `RuleSetPublished` drops the cache. |
| Statements | In-memory fixtures, language `en-IN`, status `DRAFT` until a caller marks one `APPROVED`. The sentence is a placeholder, not approved wording. |
| Registered contact | A fixed CBS-shaped number. The request body has no phone. Setting the stub unavailable returns `503` and writes nothing. |
| OTP send / verify | Local hash compare. The provider named in CNS-2 is still open. |

There is no call to Journey. A halt is the caller's reaction to a withdrawn or missing consent.

## Store

Schema `consent`: `consent_record` (PK `consent_id, record_version`), `consent_challenge` (the updated table), `otp_issue`, `idempotency_record`, `outbox_event`, `processed_event`. `VARCHAR` stands in for `CHAR` / `JSONB` / `INET` so H2 in PostgreSQL mode and PostgreSQL run the same script. On PostgreSQL, `V3` grants `app_consent` insert and select on evidence and update only on the challenge. H2 has no role, so that migration records nothing there.

CIF is not stored. The subject is the customer ULID.

## Tests

`./gradlew :services:consent-service:test jacocoTestCoverageVerification`

The API test is the build-brief set AT-CNS-01 through AT-CNS-14. Module line coverage on that run is 85.12% (1310/1539). The gate for this service is 50% line.

## Still outside this module

- Shailja's signature on the consent rule pack (GAP-006) and on statement wording, including `CNS-QSH`.
- The customer-device screen that collects the OTP (LLD O-2).
- Real administration-config, customer-service and notification calls. Budgets are in the LLD; the stubs do not open HTTP.
- S3 / WORM retention. The sweeper and the check do not delete evidence.
- Publishing `ConsentExpired` into the event catalogue (the LLD names it; the catalogue schema is still open).
