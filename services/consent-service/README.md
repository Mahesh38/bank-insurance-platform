# consent-service

Bounded context 6. Port **8092**. Package `com.bank.platform.consent`.

The service captures, verifies, withdraws, evaluates and retains consent for the assisted R0 journey. Each consent code is its own purpose. The engine adapts validity, scope, withdrawal and whether a missing consent stops the journey by looking up a `ConsentTypePolicy`. Callers do not switch on the code.

This module implements the in-service controls from the consent rule pack. It is **not** a certificate of compliance with the RBI, IRDAI or the DPDP Act. The pack is still AI-drafted, and the compliance sign-off named in that pack is outstanding. An owner decision to build the module does not create legal permission, regulator approval or a test result that was not run.

## Architecture

Hexagonal, inside one service:

| Layer | Owns |
|---|---|
| `domain` | Codes, evidence, the four type policies, the fixed evaluation chain, OTP rules, statement rules. No Spring. |
| `application` | Publish, open grant, verify, withdraw, evaluate, chain, retention. |
| `adapter` | JPA (insert-only evidence), AES-GCM field cipher, HMAC OTP hash, log-only dispatch and security event. |
| `api` | `/internal/v1/consent`. Idempotency on writes. Domain refusals become the platform error envelope. |

Evidence is insert-only (`INV-CNS-01`). Withdrawal and re-consent append a row on the same `rootConsentId`. `recordVersion` increments. `supersedesEvidenceId` points at the previous head. The original row stays `GRANTED`. A non-head row's **effective** status is `SUPERSEDED`. `EXPIRED` is derived from `validUntil` and is never written back.

`REQUESTED` and `OTP_PENDING` live on the OTP challenge, not on the evidence row. The challenge stores a hash of the code. The plaintext exists only as the argument to `OtpDispatch` and is not returned on HTTP and not written to the log.

CIF and the registered-contact reference are AES-GCM ciphertext (12-byte IV prepended) plus `encryption_key_id`. The all-zero local key is refused when the `prod` or `production` profile is active. The distributor id comes from configuration, never from the request body.

There is no HTTP client to Lead, Customer, Notification or Journey. Those identifiers are references. A journey halt is an effect on the evaluation response. The caller executes it.

## How the rule engine applies precedence

Evaluation is one fixed chain. The type policy is consulted inside that chain. It is not a switch in the caller.

1. **Subject.** The presented consent must belong to the customer id, and the presented CIF must match the decrypted stored CIF. A miss is `CONSENT_SUBJECT_MISMATCH`, a security event, and the journey is halted. The event carries actor, journey and consent id only.
2. **Head.** The head is the highest `recordVersion` for that customer, code and counterparty. A null counterparty matches only rows that also have none, so a sharing grant cannot satisfy data processing.
3. **Recorded state.** `WITHDRAWN` asks the policy what withdrawal means. Data processing, solicitation and sharing halt the journey (`JOURNEY_HALTED_CONSENT_WITHDRAWN`). Sharing after submission uses `HALT_FURTHER_SHARING` and records `CONSENT_WITHDRAWN_POST_SUBMISSION`. Communications suppresses optional messages and does **not** halt the sale. `EXPIRED` and `ABANDONED` are `CONSENT_EXPIRED` when the policy says absence blocks the journey.
4. **Scope.** Sharing requires the named insurer. A different insurer on a presented sharing consent is `CONSENT_SCOPE_MISMATCH`.
5. **Statement version.** A `MAJOR` upgrade forces re-consent until the caller sets `pastBlockingPoint`. A `MINOR` upgrade does not.
6. **Otherwise** the decision is `PROCEED`.

Absence uses the same policy. Data processing, solicitation and sharing block the step (`CONSENT_REQUIRED`). Communications returns HTTP 200 with `allowed: false` and `SUPPRESS_OPTIONAL_COMMUNICATIONS`.

| Code | Validity | Named insurer | Absence / withdrawal |
|---|---|---|---|
| `CNS-DP` | 90 days | No | Blocks / halts the journey |
| `CNS-SOL` | 90 days | No | Blocks / halts the journey |
| `CNS-SHR` | 30 days | Yes | Blocks / halts; post-submission stops further sharing |
| `CNS-COM` | Until withdrawal | No | Suppresses optional communications |

`CNS-RDR`, `DIY` and `HYBRID` are refused. The channel for R0 is `ASSISTED`. A journey-typed mobile (`^[6-9]\d{9}$`) is refused; the OTP target is a registered-contact reference. The RM role cannot verify a code (`CONSENT_DEVICE_ISOLATION`).

OTP window is 10 minutes, inclusive of the exact expiry instant and refused one second later. Three wrong attempts lock the challenge. A fourth call is `OTP_ATTEMPTS_EXCEEDED`. Five issues per customer and code in 24 hours are allowed; the sixth is `OTP_ISSUE_RATE_EXCEEDED`. A failed attempt commits on its own so the refusal does not roll the counter back. A dispatch failure rolls the challenge back.

A live grant for the same subject, code, counterparty and statement is reused and does not send another code.

## API

Prefix `/internal/v1/consent`. Writes require `X-Actor-Id` and `Idempotency-Key`. The same key and body replay the stored response. A different body is `IDEMPOTENCY_CONFLICT` (409). A missing key is `MISSING_IDEMPOTENCY_KEY` (400).

| Method | Path | Result |
|---|---|---|
| `POST` | `/statements` | Publish an immutable statement version |
| `POST` | `/grants` | Reuse a live grant, or open an OTP challenge |
| `POST` | `/challenges/{id}/verify` | Append `GRANTED` or `WITHDRAWN` evidence |
| `POST` | `/grants/{rootConsentId}/withdrawals` | Open a withdrawal challenge |
| `GET` | `/grants/{rootConsentId}` | Chain, with derived effective status |
| `GET` | `/journeys/{journeyId}` | Evidence for a journey |
| `GET` | `/customers/{customerId}` | Evidence for a customer |
| `POST` | `/evaluations` | Allowed, or a platform error when the journey is blocked |
| `POST` | `/grants/{rootConsentId}/retention` | Insert-only anchor: terminal instant plus 7 calendar years |
| `GET` | `/info` | Service identity |

Responses omit ciphertext, the distributor id, the source address, the contact reference and the statement text. `recordedState` is what was stored. `effectiveStatus` is derived.

Unknown JSON properties are rejected. LOB must be `LIFE`, `HEALTH` or `GENERAL`.

## What this module does toward RBI, IRDAI and DPDP

In-service controls from the rule pack that this module enforces:

- Separate purpose per code, not a bundle. No redirect consent in R0.
- Versioned statement text, SHA-256 of the exact text, `classifiedBy` required, English source before `hi-IN`.
- Customer-device OTP, attempt cap, issue cap, hash at rest.
- Append-only evidence, acknowledgement method `OTP_SMS_CUSTOMER_DEVICE`, captured statement snapshot.
- Subject match on customer id and CIF. Scope match for the named insurer.
- Withdrawal by appending a row. Original grant is not rewritten.
- Purpose-specific effect: halt, stop further sharing, or suppress optional communications.
- Retention anchor of seven calendar years from the terminal instant the caller supplies.
- No CIF, OTP, mobile, email, PAN, Aadhaar or source IP in the service log lines this module writes. Refusals go through the platform error catalogue. New consent codes are registered with `ErrorDefinitionProvider` and do not redefine `CONSENT_REQUIRED`.

## Residuals — not done here, and not certified

- Legal statement wording and the regulator window (`OPEN-CNS` items in the rule pack). Shailja's T4 sign-off is not recorded by this change.
- S3 Object Lock / compliance-mode retention of documents. This service stores the anchor instant only.
- Database `GRANT INSERT` for the evidence role. The store has no update or delete API. Privilege grants are an infrastructure item.
- Journey execution of `HALT_JOURNEY`. This service returns the effect.
- SMS delivery and the CBS registered-contact lookup. `OtpDispatch` is a port. The default adapter writes a constant acceptance line and does not log the code, the contact, or the challenge id.
- DPDP Consent Manager interoperability.
- The distributor id is stored in this service's database and is omitted from responses and logs. It is not separately encrypted.

## Tests

`./gradlew :services:consent-service:test jacocoTestCoverageVerification`

JaCoCo on this module after that command (config, properties and the application class are excluded by the repo gate):

| Scope | Line coverage |
|---|---|
| Module | 91.52% (1155/1262). Branch 70.33% (301/428). Gate for this service is 50% line. |
| `domain` | 92.68% (380/410) |
| `application` | 96.44% (298/309) |
| Domain + application together | 94.30% (678/719) |

H2 in PostgreSQL mode. No Testcontainers. The API test runs request, OTP verify, reuse, withdraw, re-consent, chain, retention, idempotency, device isolation, attempt lock, dispatch rollback, communications absence, and a subject-mismatch security event.

## Review notes

These notes are `reviewer_type: AGENT`. They are not T4 Architecture, Security, or Risk & Compliance signatures, and they are not a second human approval.

- Engineering: one policy per code, evaluation order fixed, evidence append-only, OTP failures committed independently of the refusal.
- Data: physical columns follow the design DDL where H2 can express them (ULID as `VARCHAR(26)`, ciphertext as `BYTEA`, LOB check). The shared `consent.` schema is not used because this database belongs only to this service. Withdrawal columns on the original row are not used.
- Product: R0 codes and assisted capture only. Communications does not stop the sale. Sharing is per insurer.
- Security and compliance: subject mismatch is a security event without the CIF. The dev field key cannot boot a production profile. Residuals above stay outside the module on purpose.

## Run

```bash
./gradlew :services:consent-service:bootRun --args='--spring.profiles.active=local'
```

Local defaults are in `application.yml`. Set `CONSENT_FIELD_KEY` (base64 of 32 bytes), `CONSENT_FIELD_KEY_ID`, `CONSENT_OTP_PEPPER` and `CONSENT_DISTRIBUTOR_ID` outside local development.
