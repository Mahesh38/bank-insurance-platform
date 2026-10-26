# integration-hub-service — Low-level design

**Status:** AI-DRAFTED 2026-10-21, awaiting Mahesh (owner, architecture) review. The `hub` schema
(§7) is a **PROPOSED** store decision and needs the owner's yes plus a DDL addition before build.

[OpenAPI](./openapi.yaml) · [API reference page](../../api-reference/services/integration-hub-service.md) ·
[Decisions 2026-10-21](../../architecture-review/DECISIONS-2026-10-21.md) ·
[ADR-024 rule engine](../../architecture-review/ADR-024-configuration-driven-rule-engine.md) ·
[ADR-025 events](../../architecture-review/ADR-025-event-specification-and-publisher.md) ·
[ADR-027 contract authority](../../architecture-review/ADR-027-openapi-contract-authority.md)

## 1. Purpose and ownership

The Hub is the only door from platform domain services to insurance providers (`IF-1`). It accepts
a **provider-neutral canonical** request, chooses the provider adapter with rule set `HUB.ROUTING`,
injects attribution, applies per-provider resilience, calls the adapter once, and maps the result
back to canonical form. Adding a provider is a new adapter plus a new `HUB.ROUTING` version — never
a change to Quotation, Proposal or Policy (`S07-VT-08`).

| | |
|---|---|
| Module | `services/integration-hub-service` · port 8100 · `service-id: integration-hub` · L5 INTERNAL |
| Callers | quotation-service (QUOTE, ULIP funds, masters, content, criteria), proposal-service (PROPOSAL_SUBMIT, form schema, status), policy-issuance-service (APPLICATION_STATUS, S-16), product-catalogue-service (eligibility criteria, §6) |
| Calls out | `1sb-integration-service` `/v1/*`; `direct-insurer-adapter-service` (deferred, same port); administration-config `rule-sets:resolve` (S-21) |
| Owns | **PROPOSED:** Aurora schema `hub` — `provider_request`, `outbox_event` (§7) |
| Owner | Mahesh (architecture, `HUB.ROUTING` content approver per ADR-024 §7) |

## 2. API summary

| Method | Path | Operations | Style |
|---|---|---|---|
| `POST` | `/internal/v1/provider-requests` | `QUOTE`, `PROPOSAL_SUBMIT` | Async: `202` then poll; **no retry on submit** |
| `GET` | `/internal/v1/provider-requests/{providerRequestId}` | — | Poll; maps adapter job → canonical state |
| `POST` | `/internal/v1/provider-queries` | `APPLICATION_STATUS`, `ELIGIBILITY_CRITERIA_GET`, `ELIGIBILITY_CRITERIA_SUBMIT`, `PROPOSAL_FORM_SCHEMA`, `MASTER_LOOKUP`, `PRODUCT_CONTENT`, `ULIP_FUNDS_LIST`, `ULIP_FUND_PERFORMANCE` | Sync |

Not a Hub operation in R0: **payment link** (D8 — premium collection is AU Bank PG via
payment-service; the adapter's `POST /v1/payments` stays unrouted) and `UW_REQUIREMENTS` (the
adapter's `GET /v1/requirements/{applicationNumber}` is only CONTRACTED, not implemented).

## 3. Routing — rule set `HUB.ROUTING` (hit policy `FIRST`)

Evaluated with `libs/bank-rule-engine` (ADR-024 §4: rules in ascending `priority`, first match
wins). The resolved version is cached (TTL 60 s, invalidated by `RuleSetPublished`); a resolution
failure **fails closed** — no compiled-in default provider.

| Input | Type | Source |
|---|---|---|
| `operation` | STRING | Request |
| `lob` | STRING | Request (`LIFE`) |
| `productClass` | STRING | Request (`TERM` / `SAVINGS` / `ULIP`) |
| `insurerCode` | STRING, optional | `selection.insurerCode`, `insurerCode`, or each of `insurerCodes` |

Seed (from the [API reference page §2.1](../../api-reference/services/integration-hub-service.md#21-canonical-operations-and-how-they-route-today); content approver Mahesh):

```json
{
  "format": "rule-set/v1", "ruleSetId": "HUB.ROUTING", "domain": "INTEGRATION",
  "scope": { "lob": "LIFE" }, "version": "1.0.0", "classification": "OPERATIONAL",
  "inputs": [ { "name": "operation", "type": "STRING", "required": true },
              { "name": "lob", "type": "STRING", "required": true },
              { "name": "productClass", "type": "STRING", "required": true },
              { "name": "insurerCode", "type": "STRING", "required": false } ],
  "decision": { "hitPolicy": "FIRST", "noMatchOutcome": "NO_ROUTE", "missingInputOutcome": "NO_ROUTE",
                "collectReasonCodes": "WINNING_ONLY" },
  "rules": [
    { "ruleId": "HUB-RT-900", "priority": 900,
      "when": { "op": "all", "args": [
        { "op": "eq", "args": [ { "var": "lob" }, { "const": "LIFE" } ] },
        { "op": "in", "args": [ { "var": "productClass" }, { "const": ["TERM", "SAVINGS", "ULIP"] } ] } ] },
      "then": { "outcome": "ONE_SB", "reasonCode": "ROUTE_AGGREGATOR" } }
  ],
  "testCases": [
    { "name": "HUB-TC-01", "inputs": { "operation": "QUOTE", "lob": "LIFE", "productClass": "TERM" },
      "expect": { "outcome": "ONE_SB" } },
    { "name": "HUB-TC-02", "inputs": { "operation": "QUOTE", "lob": "HEALTH", "productClass": "TERM" },
      "expect": { "outcome": "NO_ROUTE" } }
  ]
}
```

A direct insurer is a **lower `priority` number** rule, e.g. `HUB-RT-100` `insurerCode = INS07 →
DIRECT_INS07`, published as a new version. Rules:

1. `NO_ROUTE` → `422 CONFIGURATION_UNRESOLVABLE` (F-09: journey stays; Product decides Group-B redirect).
2. `MULTI` quote: the Hub evaluates once per entry of `insurerCodes` (or once with no insurer when
   absent). **R0:** all evaluations must yield the same provider, else `422
   CONFIGURATION_UNRESOLVABLE`. Fan-out across providers is R1 with the direct adapter.
3. `PROPOSAL_SUBMIT`, `APPLICATION_STATUS` and the status poll go to the provider **recorded on the
   QUOTE / submit row**, not to a re-evaluation — a new routing version never moves an in-flight sale.
4. The version used is stored on `hub.provider_request.routing_version` and sent as
   `configVersionRef` on the audit event.

## 4. Operation flows

### 4.1 Submit (`POST /provider-requests`)

1. Validate headers (`Idempotency-Key`, `X-Actor-Id`, `X-Correlation-Id`) and body against the
   operation schema. Any attribution field anywhere in the body → `422
   ATTRIBUTION_NOT_CALLER_SUPPLIED` and a security audit event.
2. **Idempotency** (`hub.provider_request.idempotency_key UNIQUE`): same key + same `request_hash` →
   replay the stored `202`; different hash → `409 IDEMPOTENCY_CONFLICT`; row in `DISPATCHING` → `409
   REQUEST_IN_PROGRESS`.
3. Route (§3). For `PROPOSAL_SUBMIT`, load the QUOTE row named by `quoteProviderRequestId` (must
   belong to the same `journeyId`, else `404 RESOURCE_NOT_FOUND`) and take its provider and
   `adapter_job_id` as `quoteJobId`.
4. Attribution: `distributorId` from secrets/config per (lob, insurer) — unresolvable → `422
   CONFIGURATION_UNRESOLVABLE`; `agentId` and `rmEmployeeId` from the principal behind `X-Actor-Id`
   — missing agent on `PROPOSAL_SUBMIT` → `422 AGENT_ATTRIBUTION_MISSING`; `channelType = B2B`.
5. `INSERT hub.provider_request (state DISPATCHING)` + outbox `AuditRecorded` in one transaction.
6. Per-provider bulkhead and breaker; open → `503 UPSTREAM_UNAVAILABLE` (row → `FAILED`).
7. Map canonical → adapter (§5) and call the adapter **once**, forwarding `Idempotency-Key`,
   `X-Actor-Id`, `X-Correlation-Id`.
8. Adapter `202 {jobId}` → row `IN_PROGRESS`, `adapter_job_id = jobId` → `202`.
   Adapter `4xx` → row `FAILED`, propagate the code (error-model §4).
   **Adapter timeout / connection reset after send** → row `SUBMISSION_UNCERTAIN` → `202
   {state: SUBMISSION_UNCERTAIN}`. The caller polls; the Hub never resends (§8).

### 4.2 Poll (`GET /provider-requests/{id}`)

1. Load the row; `callerRef.context` must match the calling service identity, else `404`.
2. `IN_PROGRESS`: `GET` the adapter job (`/v1/quotes/{jobId}` or `/v1/proposals/{jobId}`), map
   status (table below) and result; persist terminal state on the row. Reads may retry once.
3. `SUBMISSION_UNCERTAIN`: look the job up by idempotency key at the adapter (needs adapter
   `GET /v1/jobs?idempotencyKey=` — open item O-2); found → adopt `jobId`, continue as step 2; not
   found after the reconciliation window (configured, seed 10 min) → `FAILED` with `UPSTREAM_TIMEOUT`
   and an operations task. Proposal-service then decides (S-12: `SUBMISSION_FAILED` + ops task).

| Adapter `JobStatus` | Canonical state | Notes |
|---|---|---|
| `PENDING`, `RUNNING` | `IN_PROGRESS` | |
| `COMPLETED` | `COMPLETED` | offers / `applicationNumber` |
| `PARTIAL` | `PARTIALLY_COMPLETED` | **success** — offers plus `partialErrors` |
| `FAILED` | `FAILED` | `failure.code`: `PROPOSAL_REJECTED` on insurer refusal, else `UPSTREAM_BAD_RESPONSE`; `failure.reason` = adapter `failureReason` |
| `TIMEOUT` | `TIMED_OUT` | `failure.code`: `QUOTE_TIMEOUT` (quote) / `UPSTREAM_TIMEOUT` (proposal) |

### 4.3 Queries (`POST /provider-queries`)

1. Validate; route (§3, or the recorded provider when `quoteProviderRequestId` is given).
2. If the adapter treats the call as mutating and no key was sent, derive
   `query:{operation}:{sha256(canonical body)}`.
3. Call the adapter, map (§5), strip provider ids (`reqId`), return `200`. No Hub row is written.

### 4.4 Sequence — dispatch and poll with routing

```mermaid
sequenceDiagram
    autonumber
    participant Q as quotation-service
    participant H as integration-hub
    participant CFG as administration-config
    participant DB as hub.provider_request
    participant A as 1SB adapter
    participant P as 1SB (via Apigee)
    Q->>H: POST /provider-requests {QUOTE, LIFE, TERM} Idempotency-Key quote:{quoteId}:submit (read 30 s)
    H->>DB: lookup idempotency_key
    alt key known, same hash
        DB-->>H: stored 202
        H-->>Q: 202 replay (adapter not called)
    else new key
        H->>CFG: rule-sets:resolve HUB.ROUTING (cached, 300 ms, fail closed)
        CFG-->>H: version 1.0.0
        H->>H: evaluate FIRST → ONE_SB; inject distributorId, agentId
        H->>DB: INSERT DISPATCHING + outbox AuditRecorded
        H->>A: POST /v1/quotes {lob TERM, sumAssured, members[sequenceNumber]} (read 25 s, no retry)
        A->>P: POST quote (read ≤ 20 s)
        P-->>A: reqId
        A-->>H: 202 {jobId, PENDING}
        H->>DB: IN_PROGRESS, adapter_job_id
        H-->>Q: 202 {providerRequestId, IN_PROGRESS, ONE_SB}
    end
    loop S-11 backoff 1 s × 2 → 30 s, 20 attempts (caller budget)
        Q->>H: GET /provider-requests/{id}
        H->>A: GET /v1/quotes/{jobId} (2 s, 1 retry)
        A-->>H: PARTIAL + offers
        H-->>Q: PARTIALLY_COMPLETED {offers, partialErrors}
    end
```

## 5. Canonical ↔ adapter mapping

Source records: `CreateQuoteRequest`, `SubmitProposalRequest`, `EligibilitySubmitRequest`,
`QuoteJobResponse`, `ProposalJobResponse`, `ApplicationStatusResponse`,
`ProductUiDataResponse`, `UlipFundResponse` in
[`services/1sb-integration-service/.../api`](../../../../services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/api/)
and `MasterLookupRequest` in
[`services/1sb-integration-service/.../api/v1`](../../../../services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/api/v1/)
and `QuoteOffer`, `FundAllocation`, `JobStatus`, `BankApplicationStatus`, `Lob` in
[`libs/bank-common-domain`](../../../../libs/bank-common-domain/src/main/java/com/bank/common/domain/).

### 5.1 Classification

| Canonical | Adapter `Lob` |
|---|---|
| `lob LIFE` + `productClass TERM` | `TERM` |
| `lob LIFE` + `productClass SAVINGS` | `SAVING` |
| `lob LIFE` + `productClass ULIP` | `ULIP` |
| anything else | rejected by the Hub: `422 UNSUPPORTED_LOB` (never forwarded) |

### 5.2 QUOTE → `POST /v1/quotes` (`CreateQuoteRequest`); also the body of `ULIP_FUNDS_*`

| Canonical field | Adapter field | Rule |
|---|---|---|
| `lob` + `productClass` | `lob` | §5.1 |
| `mode` | `mode` | `MULTI` / `SINGLE` verbatim |
| `body.category` | `category` | verbatim |
| `body.requestedCover` | `sumAssured` | decimal, INR |
| `body.premiumAmount` | `premiumAmount` | required when `category = PREMIUM` |
| `body.members[].role` | `members[].role` | `LIFE_ASSURED` / `PROPOSER` |
| `body.members[].sequence` | `members[].sequenceNumber` | |
| `body.members[].dob` | `members[].dob` | ISO date string |
| `body.members[].gender` | `members[].gender` | verbatim (adapter maps to wire) |
| `body.members[].tobacco` / `annualIncome` / `pincode` / `relationship` | same names | verbatim |
| `body.preferences.paymentFrequency` | `preferences.paymentFrequency` | `HALF_YEARLY` → `HALF-YEARLY` (adapter wire map has no underscore form); others verbatim |
| `body.preferences.policyTerm` | `preferences.policyTerm` | |
| `selection.*` (`insurerCode`, `productCodes`, `planOption`, `coverOption`, `deathBenefitOption`, `policyTerm`, `premiumPaymentTerm`, `premiumFrequency`, `premiumPaymentOption`) | `selection.*` same names | required when `mode = SINGLE` |
| `journeyId` | `journeyId` | |
| — | `sessionId` | **not sent** (no BFF session id crosses into the provider layer) |
| — (Hub-injected) | `distribution.rmEmployeeId`, `distribution.agentId`, `distribution.channelType` | principal mapping; `B2B` |
| `insurerCodes` | — | used for routing only; the adapter quotes all eligible insurers in MULTI |
| `Idempotency-Key` header | `Idempotency-Key` header | forwarded unchanged |

### 5.3 PROPOSAL_SUBMIT → `POST /v1/proposals` (`SubmitProposalRequest`)

| Canonical field | Adapter field | Rule |
|---|---|---|
| `lob` + `productClass` | `lob` | §5.1 |
| `schemaId` | `schemaId` | |
| `providerOfferRef` | `offerId` | opaque, from the quote result |
| `productCode` | `productCode` | |
| `insurerCode` | `manufacturerId` | bank insurer code → provider manufacturer id (identity at R0; table in adapter config if they diverge — O-3) |
| `schemaVersion` | `version` | |
| `answers` | `values` | question id → answer |
| `consentRef` | `consentRef` | `CNS-SHR` consent id |
| `quoteProviderRequestId` | `quoteJobId` | resolved via `hub.provider_request.adapter_job_id` |
| `journeyId` | `journeyId` | |
| — (Hub-injected) | `agentId`, `distribution.{rmEmployeeId, agentId, channelType}` | mandatory agent |
| — | `sessionId` | not sent |

### 5.4 Queries

| Canonical operation and fields | Adapter call and fields |
|---|---|
| `APPLICATION_STATUS {applicationNumber, insurerCode, productCode}` | `GET /v1/status/{applicationNumber}?lob&insurerCode&productCode` |
| `ELIGIBILITY_CRITERIA_GET {insurerCode, productCode}` | `GET /v1/quotes/criteria?lob&productCode&manufacturerId=insurerCode` |
| `ELIGIBILITY_CRITERIA_SUBMIT {insurerCode, productCode, values}` | `POST /v1/quotes/criteria {lob, productCode, manufacturerId, values, agentId (injected), distribution (injected)}` |
| `PROPOSAL_FORM_SCHEMA {insurerCode, productCode, schemaVersion, quoteProviderRequestId}` | `GET /v1/proposals/schema?lob&productCode&manufacturerId&version&quoteJobId` |
| `MASTER_LOOKUP {category, entities, insurerCode}` | `POST /v1/master-data/lookup {lob, lookUpCategory, entityIds, manufacturerId}` (`lob` here is the adapter `Lob` name string) |
| `PRODUCT_CONTENT {insurerCode, productCode}` | `GET /v1/products/ui-data?productId=productCode&manufacturerId=insurerCode` |
| `ULIP_FUNDS_LIST` / `ULIP_FUND_PERFORMANCE {body, selection}` | `POST /v1/ulip/funds/list` / `/performance` with a `CreateQuoteRequest` built as §5.2 |

### 5.5 Results

| Adapter field | Canonical field | Rule |
|---|---|---|
| `QuoteJobResponse.status` / `ProposalJobResponse.status` | `state` | §4.2 table |
| `QuoteOffer.offerId` | `offers[].providerOfferRef` | opaque |
| `QuoteOffer.insurerCode`, `insurerName`, `productCode`, `productName`, `premiumAmount`, `premiumFrequency`, `sumAssured`, `outOfBound` | same names | offers with `errorSummary == null` |
| `QuoteOffer` with `errorSummary != null` | `partialErrors[] {insurerCode, productCode, code: UPSTREAM_BUSINESS_ERROR, detail: errorSummary}` | not an offer |
| `QuoteOffer.offerStatus` | — | consumed by the split above |
| `QuoteOffer.funds[] {code, name, allocationPercent}` | `offers[].funds[]` same names | |
| `ProposalJobResponse.applicationNumber` | `result.applicationNumber` | |
| `failureReason` | `failure.reason` | diagnostic |
| `ApplicationStatusResponse.bankStatus`, `policyNumber`, `applicationNumber` | same names | |
| `ApplicationStatusResponse.manufacturerSubStatus` | `providerSubStatus` | diagnostic, not displayed |
| `ApplicationStatusResponse.lastUpdated` | `providerUpdatedAt` | |
| `EligibilitySubmitResult.accepted`, `status` | same | `reqId` dropped |
| `ProposalSchema {productCode, manufacturerId, version, fields}` | `FormSchemaResult {productCode, insurerCode, schemaVersion, fields}` | |
| `MasterLookupResponse.lookups`, `cache` | `lookups`, `cache` | bank codes |
| `ProductUiDataResponse.data`, `UlipFundResponse.data` | `ContentResult.data` | `reqId` dropped (`INV-ACL-01`) |

The `bankStatus` → `policy.policy_status_history` status mapping (D8) is owned by
policy-issuance-service, not the Hub.

## 6. Eligibility criteria and the catalogue

`ELIGIBILITY_CRITERIA_GET` / `_SUBMIT` are called by **product-catalogue-service** (and Quotation
for Savings/ULIP gate forms). Precedence between the bank's catalogue eligibility and the insurer
criteria is defined in the [catalogue LLD §5](../product-catalogue-service/LLD.md#5-eligibility--two-sources-one-precedence):
catalogue rule set first, then insurer criteria through the Hub; both must pass.

## 7. Where idempotency and correlation live — decision (PROPOSED)

**Decision:** the Hub keeps a small `hub` schema. It does **not** delegate idempotency to the adapter.

| Option | Why |
|---|---|
| **A. `hub.provider_request` (chosen)** | (1) **Routing stability:** a retried submit must reach the provider the first attempt reached. If `HUB.ROUTING` is republished between the attempts (hot config, ADR-024 §5), a stateless Hub would re-route and could submit the same proposal to two providers — only a Hub-side record bound to the key prevents that. (2) `providerRequestId` must survive pod restarts and multiple replicas; the page's "in-memory cache" did not. (3) `PROPOSAL_SUBMIT` must resolve the QUOTE's adapter job (`quoteJobId`) and provider. (4) Every future adapter would otherwise have to implement identical durable idempotency; the 1SB adapter's filter is per-pod memory today |
| B. Delegate to the adapter | Works for one provider only; loses (1)–(3) |

```sql
-- PROPOSED DDL addition (new file, e.g. schemas/15-hub.sql) — not yet in the DDL set.
CREATE TABLE hub.provider_request (
    provider_request_id  CHAR(26)     PRIMARY KEY,
    idempotency_key      VARCHAR(128) NOT NULL UNIQUE,
    request_hash         VARCHAR(64)  NOT NULL,
    operation            VARCHAR(32)  NOT NULL,
    caller_context       VARCHAR(32)  NOT NULL,
    caller_aggregate_id  CHAR(26)     NOT NULL,
    journey_id           CHAR(26),
    provider             VARCHAR(32)  NOT NULL,
    routing_version      VARCHAR(64)  NOT NULL,
    adapter_job_id       VARCHAR(36),
    state                VARCHAR(24)  NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- plus hub.outbox_event (ADR-025 shape); role app_hub; no customer PII in either table.
```

Retention: rows 90 days (purge job), longer than any poll window; the idempotency key is
server-derived from an aggregate id so it is never legitimately reused. Store-ownership §2.3 requires
an ADR for a new Hub store: **this section is the proposal; Mahesh's yes is required** (O-1).

## 8. Timeouts — nested budgets, no retry on submit

Each inner budget is strictly shorter than its caller's so the caller never gives up while an inner
hop can still succeed and be unseen.

| Hop | Connect / read | Retry | On expiry |
|---|---|---|---|
| Quotation / Proposal → Hub submit (S-09 / S-12) | 3 s / **30 s** | **none** | Caller polls the `providerRequestId` it already has, or replays with the same key (Hub replays, does not resend) |
| Hub → config resolve (S-21) | 300 ms (cached) | none | `422 CONFIGURATION_UNRESOLVABLE` |
| Hub → adapter submit | 2 s / **25 s** | **none** | Row `SUBMISSION_UNCERTAIN`; `202` to caller |
| Adapter → 1SB submit | 3 s / **≤ 20 s** required | none (adapter rule) | Adapter job stays `RUNNING`; poller recovers |
| Caller → Hub poll | 1 s / 3 s | caller loop (S-11) | next attempt |
| Hub → adapter poll | 1 s / 2 s | 1 (read) | `503 UPSTREAM_UNAVAILABLE` |
| Policy → Hub status (S-16) | 3 s / 15 s | scheduled re-check | `CONFIRMATION_OVERDUE` path |
| Hub → adapter query | 2 s / 12 s | 1 for pure reads only | `504 UPSTREAM_TIMEOUT` |

**Deviation:** the adapter's `ONESB_READ_TIMEOUT_MS` default is 30 000 ms
([`application.yml`](../../../../services/1sb-integration-service/src/main/resources/application.yml)),
which breaks the nesting. It must be ≤ 20 000 ms (O-4, 1SB adapter owner).

## 9. Validation rules

| Field | Rule | Error code |
|---|---|---|
| `Idempotency-Key` | present on `POST /provider-requests`, ≤ 128 | `MISSING_IDEMPOTENCY_KEY` |
| `X-Actor-Id`, `X-Correlation-Id` | present | `VALIDATION_ERROR` |
| `operation` | known for the path | `VALIDATION_ERROR` |
| `lob` / `productClass` | `LIFE` / `TERM`·`SAVINGS`·`ULIP` | `UNSUPPORTED_LOB` |
| body | no `distributorId`, `agentId`, `spLicenceId`, `distribution` anywhere | `ATTRIBUTION_NOT_CALLER_SUPPLIED` |
| `body.members` | ≥ 1; `dob`, `gender` present; `pincode` `^[1-9][0-9]{5}$` | `VALIDATION_ERROR` (`MISSING_REQUIRED_FIELD` per field) |
| `body.premiumAmount` | required when `category = PREMIUM` | `VALIDATION_ERROR` |
| `selection` | required when `mode = SINGLE` | `VALIDATION_ERROR` |
| `quoteProviderRequestId` | exists, QUOTE, same `journeyId`, state `COMPLETED`/`PARTIALLY_COMPLETED` | `RESOURCE_NOT_FOUND` / `ILLEGAL_TRANSITION` |
| route | `HUB.ROUTING` yields a provider; MULTI yields one provider | `CONFIGURATION_UNRESOLVABLE` |
| attribution | distributor resolvable / agent resolvable on submit | `CONFIGURATION_UNRESOLVABLE` / `AGENT_ATTRIBUTION_MISSING` |

## 10. DTO → domain → table

| DTO | Domain | Table / column |
|---|---|---|
| `ProviderRequestAccepted.providerRequestId` | `ProviderRequest.id` | `hub.provider_request.provider_request_id` |
| `Idempotency-Key` + SHA-256(canonical body) | `IdempotencyGuard` | `idempotency_key`, `request_hash` |
| `operation`, `callerRef`, `journeyId` | `ProviderRequest` | `operation`, `caller_context`, `caller_aggregate_id`, `journey_id` |
| `provider`, `routingVersion` | `RouteDecision` | `provider`, `routing_version` |
| adapter `jobId` | `AdapterCorrelation` | `adapter_job_id` |
| `state` | `CanonicalState` | `state` |
| offers / application number | — | **not stored by the Hub** (caller's aggregate and adapter `bank_persistence` hold them) |

## 11. Invariants

| Invariant | Enforced where |
|---|---|
| `INV-ACL-01` provider vocabulary stays in the adapter | Mapping layer (§5); ArchUnit: no `adapter.onesb.*` types in the Hub |
| `INV-DIS-01` attribution injected, never caller-supplied | Request validator (§9) |
| No automatic retry on submit | Hub client config (§8); `SUBMISSION_UNCERTAIN` state |
| One key → one provider | `idempotency_key UNIQUE` + provider fixed on insert |
| Fail closed on configuration | Routing resolver |

## 12. Events

| Direction | Event | Notes |
|---|---|---|
| Produced | `AuditRecorded` (action `PROVIDER_REQUEST_DISPATCHED` / `_FAILED`, `configVersionRef = HUB.ROUTING@v`) | Via `hub.outbox_event`; depends on ADR-026 (Proposed) |
| Consumed | `RuleSetPublished` (`HUB.ROUTING`) | Cache invalidation |

Domain events (`QuoteCompleted`, `ProposalSubmitted`, …) remain the callers' (ADR-025). Names are
from ADR-025 and [outbox-and-events](../../api-reference/interactions/outbox-and-events.md);
`docs/platform/contracts/events/CATALOGUE.md` supersedes them once published.

## 13. Failure modes

| Failure | Behaviour |
|---|---|
| Adapter times out on submit | `SUBMISSION_UNCERTAIN`; reconcile by key; never resend |
| Adapter breaker open | `503 UPSTREAM_UNAVAILABLE` for that provider only (bulkhead) |
| Routing config unavailable | `422 CONFIGURATION_UNRESOLVABLE` (fail closed) |
| Hub DB unavailable | `503 UPSTREAM_UNAVAILABLE` before any adapter call — nothing is dispatched unrecorded |
| `PARTIAL` | Success |

## 13a. Open items

| # | Item | Owner |
|---|---|---|
| O-1 | Approve the `hub` schema (§7) and add DDL `15-hub.sql`, role `app_hub`, grants | Mahesh; DBA |
| O-2 | Adapter lookup by idempotency key (`GET /v1/jobs?idempotencyKey=`) for `SUBMISSION_UNCERTAIN` reconciliation | 1SB adapter owner |
| O-3 | Confirm bank `insurerCode` = 1SB `manufacturerId`, or provide the mapping table | 1SB adapter owner |
| O-4 | Lower `ONESB_READ_TIMEOUT_MS` to ≤ 20 s (or per-operation budgets) | 1SB adapter owner |
| O-5 | Registry says `CONFIGURATION_UNRESOLVABLE` = 422; ADR-024 §5 says 503. This contract follows the registry | Mahesh |
| O-6 | `BankApplicationStatus.CLOSED` is ambiguous (declined vs closed after issue) for D8 history mapping | Policy-issuance owner |
