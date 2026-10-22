# notification-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Mahesh (architecture) and Shailja (template wording) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

**Skeleton** (6 main / 2 test classes; no migration). 0 / 2 operations. Schema `notification`
accepted in round 2 (E3); design DDL being added by the DDL owner. Maturity ≈ 5 %.

## 2. Scope for this build

Four R0 templates (`CONSENT_OTP` sync; `PAYMENT_LINK`, `POLICY_ISSUED`, `PROPOSAL_DECLINED` from
events), template resolution from configuration (fail closed while unpublished), contact points
from customer-service, provider adapter port (SMS / email) with an in-memory adapter, delivery
state `QUEUED → SENT | FAILED`, retries per template for event sends, outbox. Holds **no message body
and no contact data**; sensitive parameters (`otp`, `url`) never persisted in clear.

## 3. Operations

[openapi.yaml](./openapi.yaml): `sendNotification`, `getNotification`; consumers for
`PaymentLinkIssued`, `PolicyIssued`, `ProposalDeclined`.

## 4. Data owned

Schema `notification` (E3) — LLD §7: `notification` (`idempotency_key UNIQUE`), `delivery`
(`parameters_enc`, nulled at terminal state), `processed_event`, `outbox_event`. Follow the design
DDL file once it appears in [schemas/](../../data-architecture/schemas/README.md). Role `app_notification`.

## 5. Rule sets and configuration

`NOTIFICATION.TEMPLATES` (classification `COMPLIANCE`): channels, bodies with `@placeholders`,
`requiredParameters`, `sensitiveParameters`, `maxAttempts`, `retryBackoff`, `language`. **No seed text
exists by design** (wording awaits Shailja) and its storage is not contracted in administration-config
(CFG-2). Until published every send fails closed with `422 CONFIGURATION_UNRESOLVABLE`.

## 6. Events

Consumed: `PaymentLinkIssued`, `PolicyIssued`, `ProposalDeclined`. Produced: `NotificationSent`,
`NotificationFailed` (**not in catalogue**, NOT-4), `AuditRecorded`.

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists — stub contract |
|---|---|---|
| customer-service contact points | 1 s, no retry | **No contact-point operation in the customer contract** (NOT-3); stub `getRegisteredContact` per [customer openapi](../customer-service/openapi.yaml) for the mobile only |
| administration-config | 300 ms, cached | Sample template set in test resources (`"sample": true`) |
| bank SMS / email gateway | connect 1 s / read 3 s, **no retry for OTP** | In-memory `NotificationGateway` adapter (refused in prod) |
| payment-service `getPayment` (link URL, amount) / policy-issuance `getPolicy` | — | Stubs per their openapi files; how the link URL is fetched is not in the payment contract (NOT-5) |

Inbound: consent → notification read 5 s.

## 8. Invariants

Recipient is only a `customerId` — any phone / email / device field → `PAYMENT_DEVICE_ISOLATION`
(C4) · sensitive parameters never persisted / logged · delivery failure never blocks a journey (ops
task) · idempotent on `Idempotency-Key` / `eventId`.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-NOT-01 | Template unpublished → `422 CONFIGURATION_UNRESOLVABLE`; event path → `FAILED` + ops task | LLD §9 |
| AT-NOT-02 | Request with a phone number in `recipient` → `PAYMENT_DEVICE_ISOLATION` | C4, LLD §6 |
| AT-NOT-03 | OTP send: gateway accepts → `200 SENT`; gateway timeout → `504`, no retry, `NotificationFailed` | LLD §4.1 |
| AT-NOT-04 | Same `Idempotency-Key` twice → one delivery | LLD §4.1 |
| AT-NOT-05 | `PaymentLinkIssued` delivered twice → one notification; URL stored encrypted then nulled at terminal state | LLD §4.2 |
| AT-NOT-06 | Retryable gateway failure retried up to `maxAttempts`; non-retryable 4xx not retried | LLD §5 |
| AT-NOT-07 | Log capture contains no OTP / URL | `INV-LOG-01` |
| AT-NOT-08 | Customer without a contact for the channel → `404 RESOURCE_NOT_FOUND` | LLD §6 |

## 10. Build order

**Tier 4** (consent needs it for OTP). Startable now with stubs.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| NOT-1 | Template wording, languages, channels (C7 posture: drafts only) | Shailja | Any real send at go-live |
| NOT-2 | Design DDL for schema `notification` + grants (E3) | Aarti | Migration parity |
| NOT-3 | Service-restricted contact-point read in customer-service (email for `PAYMENT_LINK`) | customer owner, Mahesh | Email channel |
| NOT-4 | `NotificationSent`, `NotificationFailed` in the catalogue | Events owner | Publishing them |
| NOT-5 | How notification obtains the payment link URL (not exposed by the payment contract) | Mahesh | `PAYMENT_LINK` |
| NOT-6 | Bank SMS / email gateway API and Apigee exposure | Bank | Go-live |
| NOT-7 | Template storage in administration-config (CFG-2) | Mahesh | Real templates |
