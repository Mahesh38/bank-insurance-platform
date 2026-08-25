# 04 — Error and Degraded-State Catalogue

**Every refusal the platform can produce: who emits it, what the client does, what the journey does.**

Status: `AI-DRAFTED` · Owner: Mahesh (Board 1) + Digital (S05 canon) · Origin: `SUG-20260821-jx1`

This file closes one of the two S05 canon artefacts marked absent in
[`05-DOCUMENTATION-CANON.md`](../application-lifecycle-bible/05-DOCUMENTATION-CANON.md) —
*"Error, empty and degraded-state catalogue"* — for the R0 slice. The journey wireframes it should
pair with (`GAP-009`) remain outstanding, so the **Client behaviour** column is a specification of
intent, not an approved UX.

---

## 1. Reading the columns

| Column | Meaning |
|---|---|
| **Layer** | Where the refusal is produced — `01 §1` |
| **Retryable** | `no` = the same request will always fail · `after-fix` = fix input · `yes` = transient |
| **Journey effect** | What happens to journey stage. `unchanged` is the default and the safe one |
| **Audit** | Whether a platform audit/compliance event is written |
| **Alert** | Whether a sustained rate is an operational signal |

**The default posture on every failure is: journey stage unchanged, evidence untouched, no partial
advance.** A row that departs from that says so explicitly.

---

## 2. Envelope and protocol errors

| HTTP | Code | Layer | Retryable | Journey effect | Audit | Alert |
|---|---|---|---|---|---|---|
| 403 | *(WAF block, no body)* | L1 | no | unchanged | **no** | rate |
| 413 | `PAYLOAD_TOO_LARGE` | L2 | after-fix | unchanged | no | no |
| 400 | `SCHEMA_INVALID` | L2 | after-fix | unchanged | no | no |
| 429 | `RATE_LIMITED` | L2 | yes, honour `Retry-After` | unchanged | no | rate |
| 400 | `MISSING_IDEMPOTENCY_KEY` | L4 | after-fix | unchanged | no | no |
| 409 | `IDEMPOTENCY_KEY_CONFLICT` | L5 | no | unchanged | yes | rate |
| 409 | `REQUEST_IN_PROGRESS` | L5 | yes, brief backoff | unchanged | no | no |
| 422 | `ATTRIBUTION_NOT_CALLER_SUPPLIED` | L4/L5 | after-fix | unchanged | **security event** | **yes** |
| 422 | `LOB_REQUIRED` | L5 | after-fix | unchanged | no | no |

**Client behaviour.** `429` is the only one the app retries automatically, once, on the server's
`Retry-After`. Everything else surfaces. `ATTRIBUTION_NOT_CALLER_SUPPLIED` should never be seen in
production by a correct client — treat any occurrence as a client defect or an intrusion attempt.

---

## 3. Session and authentication

| HTTP | Code | Layer | Retryable | Journey effect | Audit | Client behaviour |
|---|---|---|---|---|---|---|
| 401 | `SESSION_INVALID` | L4 | after re-auth | unchanged | `SessionRevoked` | Re-authenticate, return to the same journey step |
| 401 | `SESSION_EXPIRED` | L4 | after re-auth | unchanged | `SessionRevoked` | Same. Draft input held locally is preserved |
| 401 | `SESSION_REVOKED` | L4 | **no** | unchanged | `SessionRevoked` | Full logout. Do **not** offer a silent retry — the credential may be stolen |
| 401 | `AUTHENTICATION_FAILED` | L4 | after re-auth | n/a | `LoginFailed` | Generic message only. Never says which of the four causes fired |
| 401 | `STEP_UP_REQUIRED` | L4 | yes, after MFA | unchanged | yes | Prompt MFA, then replay the original request with its original idempotency key |
| 400 | `INVALID_STATE` | L4 | no — restart login | n/a | **security event** | Restart the login ceremony from the beginning |
| 400 | `CODE_ALREADY_CONSUMED` | L4 | no | n/a | security event | Restart login |
| 400 | `RETURN_LOCATION_NOT_ALLOWED` | L4 | no | n/a | **security event** | Restart login with the default return location |
| 403 | `CSRF_REJECTED` | L4 | no | unchanged | security event | Re-fetch a CSRF token; a second failure is a logout |
| 503 | `IDENTITY_PROVIDER_UNAVAILABLE` | L4 | yes | n/a | yes | "Sign-in temporarily unavailable." **No alternative sign-in path is offered** (`VR-023`) |

> The four distinct causes behind `AUTHENTICATION_FAILED` — unknown subject, inactive identity,
> missing employment, empty branch scope — are separated **in the event stream**, never in the
> response. A response that distinguishes them is a user-enumeration oracle.

---

## 4. Authorization

| HTTP | Code | Layer | Retryable | Journey effect | Audit | Client behaviour |
|---|---|---|---|---|---|---|
| 403 | `DEFAULT_DENY` | L5 PDP | no | unchanged | yes | "You do not have access to this action" |
| 403 | `EXPLICIT_DENY` | L5 PDP | no | unchanged | yes | Same message. The distinction is for audit, not for the RM |
| 403 | `ACCOUNT_SUSPENDED` | L5 PDP | no | unchanged | yes | Full logout, contact-supervisor message |
| 403 | `ORIGINATION_RM_ONLY` | L4/L5/L6 | no | unchanged | **compliance event** | IPR surface should not have offered the control at all — also a UI defect |
| 403 | `ASSIST_ONLY_ACTOR` | L5 PDP | no | unchanged | **compliance event** | Same |
| 403 | `CROSS_INSURER_DENIED` | L5 PDP | no | unchanged | **compliance event** | On a *read*, this must never appear — the row is absent instead |
| 403 | `OUT_OF_BRANCH_SCOPE` | L5 PDP | no | unchanged | yes | "Outside your branch scope" |
| 403 | `SP_CERTIFICATION_REQUIRED` | L5 | **no** | unchanged | **compliance event** | Names the expiry, the LOB, and the renewal path. Non-selling work stays available |
| 403 | `BREAK_GLASS_INVALID` | L5 PDP | no | unchanged | enhanced audit | Ops path only |
| 403 | `AUTHORIZATION_UNAVAILABLE` | L4/L5 | yes | unchanged | yes | "Temporarily unavailable, please retry." **Alert on rate** — this is fail-closed, not normal |
| 403 | `SERVICE_IDENTITY_REJECTED` | L5 | no | unchanged | **security event** | Never reaches a human. A leaked internal URL is not an entry point |

---

## 5. Compliance hard gates

These are the refusals that exist because a regulator requires them. Every one is a **compliance
event** and every one is **not retryable** until the RM does the missing thing.

| HTTP | Code | Gate | Layer | Journey effect | What the RM must do |
|---|---|---|---|---|---|
| 403 | `SUITABILITY_REQUIRED` | **C1** | L6 | Quote persisted `REJECTED`; journey **unchanged** | Complete or refresh the suitability assessment, then re-quote |
| 403 | `CONSENT_REQUIRED` | **C2** | L6 | Proposal stays `DRAFT` | Re-run the consent OTP on the customer's device |
| 422 | `ATTRIBUTION_NOT_CALLER_SUPPLIED` | **C3** | L5 Hub | unchanged | Nothing — client defect |
| 403 | `PAYMENT_DEVICE_ISOLATION` | **C4** | L5/L6 | unchanged | Nothing — the link goes to the customer. There is no RM-side path, by design |
| 409 | `PAYMENT_NOT_RECONCILED` | `SC-W3-4` | L6 | Policy not created; journey holds | Wait for reconciliation. Never override |
| 422 | `CONFIG_VERSION_REQUIRED` | `INV-CFG-03` | L6 | unchanged | Nothing — platform defect |

> `SUITABILITY_REQUIRED` writes a `Quote` row in `REJECTED` and emits
> `QUOTE_REJECTED_NO_SUITABILITY`. **The refusal is itself evidence** — it is not a silent 403.
> A regulator asking "show me every quote you refused, and why" is answered from this row.

---

## 6. Domain state errors

| HTTP | Code | Layer | Journey effect | Client behaviour |
|---|---|---|---|---|
| 409 | `ILLEGAL_TRANSITION` | L6 | unchanged + **integrity alert** | Re-fetch state; the client's view is stale |
| 409 | `QUOTE_EXPIRED` | L6 | unchanged | Offer a re-quote. Do not auto-re-quote — C1 must be re-checked |
| 409 | `ASSESSMENT_IMMUTABLE` | L6 | unchanged | Create a new assessment; the old one is `SUPERSEDED` |
| 409 | `WITHDRAWAL_NOT_PERMITTED` | L6 | unchanged | Explain that money is in flight |
| 409 | `PAYMENT_ALREADY_IN_PROGRESS` | L7 | unchanged | Show the existing attempt, never start a second |
| 409 | `PAYMENT_STATE_UNCERTAIN` | L6 | unchanged | **Wait.** Never a new attempt — this is the double-charge guard |
| 422 | `CUSTOMER_NOT_IN_BOOK` | L6 | n/a | Not an error to work around; the customer is not this RM's ETB |
| 422 | `RM_NOT_CERTIFIED` | L6 | n/a | Assign to a certified RM |
| 422 | `OPPORTUNITY_REQUIRED` | L6 | n/a | Client defect |
| 422 | `INVALID_OFFER_REFERENCE` | L6 | unchanged | Re-select from a live quote |
| 422 | `PREMIUM_MISMATCH` | L6 | unchanged + **financial alert** | Stop. Do not retry. This is a pricing-integrity incident |

---

## 7. Degraded states — the ones with no error code

The states that matter most have **no HTTP status**, because nobody is waiting on them.

| State | Trigger | Meaning | Resolution | Never |
|---|---|---|---|---|
| `PARTIALLY_QUOTED` | Some insurers answered, some did not | **Success**, not failure (`INV-QUO-02`) | RM proceeds with what returned | Treated as an error |
| `TIMED_OUT` | Quote poll budget exhausted (`S-11`) | No answer inside the budget | Journey → `ABANDONED` with a re-quote path | Retried automatically forever |
| `SUBMISSION_FAILED` | Proposal submit exhausted (`S-12`) | Submit may or may not have landed | Poll by external reference; ops task `F-04` | **Auto-resubmitted** — that is the double-submit failure mode |
| `UNCERTAIN` | PG callback never arrived (`S-14`) | Money state unknown | Reconciliation `F-08` resolves it | **Guessed.** Never assumed paid or unpaid |
| `RECONCILIATION_BREAK` | Settlement unmatched past SLA (`S-15`) | Books disagree | Manual procedure `F-07` | Auto-corrected |
| `CONFIRMATION_OVERDUE` | Issuance confirm not returned (`S-16`) | Policy state unconfirmed | Scheduled re-check | Assumed issued |
| `ISSUANCE_DISPUTED` | Re-check still disagrees | Insurer and platform disagree | Ops + insurer resolution | Resolved by the platform alone |
| `MANUAL_INTERVENTION` | Compensation exhausted (`S-19`) | Automation is out of moves | Named owner picks it up | Silently closed (`INV-JRN-04`) |
| *audit backlog* | Outbox lag beyond SLA | Events not yet acknowledged | Poller retries unbounded | Dropped. Journey cannot reach `SOLD` (`INV-JRN-05`) |

> Five of these nine exist because the platform refuses to guess about money or evidence. That
> refusal is the design, not a gap: `UNCERTAIN` resolved by reconciliation is correct;
> `UNCERTAIN` resolved by assumption is a financial-control failure.

---

## 8. Dependency-down behaviour

From [`03-solution-architecture §5.3`](../platform/ws3-platform/03-solution-architecture-r0.md#53-resilience-policy-per-dependency-class--s07-e02-s04).

| Dependency | Timeout | Retry | Degraded mode | Caller sees |
|---|---|---|---|---|
| **PDP** | 300 ms | none | **none — fail closed** | `403 AUTHORIZATION_UNAVAILABLE` |
| **Configuration** | 300 ms | none | **none — fail closed**; cache serves to TTL only | `422 CONFIGURATION_UNRESOLVABLE` |
| CBS | 3 s | 1 on 5xx/connect | snapshot inside freshness window | `503` typed, journey holds at `INITIATED` |
| Product catalogue | 500 ms | 1 | read cache | `503` typed |
| Hub → adapter | per adapter | **none on submit** | partial-quote success, per-provider bulkhead | `PARTIALLY_QUOTED` or `TIMED_OUT` |
| AU Bank PG | 3 s / 15 s | none on session create | **none on the money path** | `REJECTED`; proposal stays `AWAITING_PAYMENT` |
| Identity provider | — | — | **none** | `503 IDENTITY_PROVIDER_UNAVAILABLE` |
| Outbox → audit | 2 s | unbounded, backoff | queue and alert | invisible; `SOLD` blocked until acknowledged |

**Three dependencies have no degraded mode at all** — PDP, Configuration and the identity provider.
Each is a deliberate decision recorded in the architecture, and each has the same rationale: a
fallback would be an implicit allow or a compiled-in rule, which is exactly what the control it
implements forbids. A per-provider bulkhead is an architecture property, not a tuning knob: one
failing insurer must never consume the connection budget that makes every other insurer look down.
