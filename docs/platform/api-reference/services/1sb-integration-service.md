# 1sb-integration-service — 1SB provider adapter (#15, WS-1)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | The bank's **translation booth** for 1SilverBullet (1SB), the insurance aggregator. Takes bank-canonical requests (`lob`, members, bank enums), builds 1SB wire payloads per LOB, calls 1SB, polls its async jobs, normalises statuses and offers back to bank language, and records every exchange. **1SB vocabulary never leaves `adapter.onesb.*`** (ArchUnit) |
| **Module** | `services/1sb-integration-service` · port 8080 · `service-id: onesb` · layer L5 · INTERNAL · Swagger at `/swagger-ui.html` |
| **Base path** | `/v1` (bank-facing, cluster-private) |
| **Callers** | **Target: Integration Hub only** (`IF-1`). No WS-3 domain service may call it directly. Today it is exercised directly in tests and UAT drives |
| **Calls out** | 1SB (`ONESB_BASE_URL`; UAT/prod via **Apigee**, `ADR-020`) with `Authorization: Basic apiKey:apiSecret` · `bank-persistence-service` `/internal/v1/*` for every durable write |
| **Owns data** | **No datasource. No JPA. No Flyway.** All job, offer, poll, raw-payload, payment-session and audit rows live in `bank_persistence` through HTTP |
| **Code status** | **IMPLEMENTED** — Life Term, Savings, ULIP: quote, eligibility criteria, proposal schema/submit/poll, payment URL, application status, master lookup, product UI data, ULIP funds |
| **Inbound auth** | No application-level check in code; relies on cluster-private exposure (target: mesh mTLS; the WS-1 architecture also describes JWT / mTLS client certs) |

---

## 1. API index

| Method | Path | Status | Purpose | Idempotency-Key |
|---|---|---|---|---|
| `POST` | `/v1/quotes` | IMPLEMENTED | Create async quote job | **Required** |
| `GET` | `/v1/quotes/{jobId}` | IMPLEMENTED | Poll quote job; offers when done | — |
| `GET` | `/v1/quotes/criteria?lob&productCode&manufacturerId` | IMPLEMENTED | Eligibility (gate-criteria) form for a product | — |
| `POST` | `/v1/quotes/criteria` | IMPLEMENTED | Submit gate-criteria answers | **Required** (mutating `/v1`) |
| `GET` | `/v1/proposals/schema?lob&productCode&manufacturerId&version&quoteJobId` | IMPLEMENTED | Dynamic proposal form schema | — |
| `POST` | `/v1/proposals` | IMPLEMENTED | Submit proposal (async job) | **Required** |
| `GET` | `/v1/proposals/{jobId}` | IMPLEMENTED | Poll proposal job; `applicationNumber` when known | — |
| `POST` | `/v1/payments` | IMPLEMENTED | Create 1SB payment-URL session | **Required** |
| `GET` | `/v1/status/{applicationNumber}?lob&insurerCode&productCode` | IMPLEMENTED | Insurer application status, normalised | — |
| `POST` | `/v1/master-data/lookup` | IMPLEMENTED | Cached master values (exempt from idempotency) | Not required |
| `GET` | `/v1/products/ui-data?productId&manufacturerId` | IMPLEMENTED | Product UI content | — |
| `POST` | `/v1/ulip/funds/list` | IMPLEMENTED | ULIP fund list for a quote context | **Required** |
| `POST` | `/v1/ulip/funds/performance` | IMPLEMENTED | ULIP fund performance | **Required** |
| `POST` | `/v1/payments/{paymentSessionId}/intimation` | DEFERRED (FUNC-008, Phase 5.3) | Payment intimation | — |
| `GET` | `/v1/requirements/{applicationNumber}` | CONTRACTED (WS-1 architecture §5.4) | Pending UW requirements | — |
| `GET` | `/v1/jobs/{jobId}` | CONTRACTED | Generic job status | — |

Common headers: `Idempotency-Key` (see table), `X-Actor-Id` (optional; defaults to `system` on
jobs and audit), `X-Correlation-Id`. `lob` is a required enum in every body:
`TERM | HEALTH | MOTOR | SAVING | ULIP | ANNUITY | PENSION` (`bank-common-domain` `Lob`) — R0
uses `TERM`, `SAVING`, `ULIP`; Health and Motor are Phase 5.

---

## 2. Cross-cutting behaviour

| Concern | Behaviour |
|---|---|
| Idempotency | `IdempotencyFilter`: mutating `/v1/**` (except `/v1/master-data`) needs `Idempotency-Key`. SHA-256 body fingerprint. Same key + same body → stored response replayed; different body → `409 IDEMPOTENCY_CONFLICT`; missing → `400 MISSING_IDEMPOTENCY_KEY`. Responses `≥ 500` are not stored. Store is per-pod memory today |
| Attribution | `distributorId` is **never** a request field (`COMP-004`); it is injected from secrets on every 1SB call. Agent id comes from `distribution.agentId` / mapping |
| Submit retries | **None.** Quote / proposal submits are never retried automatically — 1SB may have processed them. Recover by polling the `reqId` |
| Polling | `AsyncJobPoller` on a dedicated `pollingExecutor`: base 1 s × 2, cap 30 s, max 20 attempts; each attempt recorded; exhaustion → job `TIMEOUT` (`POLL_TIMEOUT`); unexpected poll error → `FAILED` (`POLL_ERROR` / `UPSTREAM_BAD_RESPONSE`) |
| Circuit breaker | Enabled: opens after 5 failures, 30 s open, 1 half-open success |
| Raw payloads | REQ and RES bodies of quote, proposal and payment calls are sent to `bank-persistence-service` (encrypted there, 7 years). **Proposal-submit and payment-link REQUESTS are evidence:** stored before the call, retried once, and if still failing the call is **not made** (`503 UPSTREAM_UNAVAILABLE`; a proposal job is marked `FAILED`). Quote traffic and all RESPONSES stay best effort (logged at ERROR as `RAW_PAYLOAD_CAPTURE_FAILED`) — after 1SB answered, failing would only invite a duplicate submit (`SUG-20261021-gap`) |
| Audit | `AuditEventPublisher` emits e.g. `QUOTE_CREATED` (`PENDING`) with job id, lob, journey id, agent id. Audit failure never breaks the create path |
| Errors | Platform `problem+json`; provider business errors normalised (`UPSTREAM_BUSINESS_ERROR`, `PROPOSAL_REJECTED`), never raw 1SB text |

---

## 3. Endpoints

### 3.1 `POST /v1/quotes`

**Request** (`CreateQuoteRequest`):

| Field | Type | Validation / meaning |
|---|---|---|
| `lob` | enum | `@NotNull` |
| `mode` | string | `MULTI` (all eligible insurers) or `SINGLE` (one pinned product) |
| `category` | string | e.g. `SUM_ASSURED` — sum assured vs premium driven |
| `sumAssured` | decimal | `@NotNull` |
| `premiumAmount` | decimal | When premium-driven |
| `members[]` | list | `@NotEmpty`, each: `role`, `sequenceNumber`, `dob` (`@NotNull`), `gender` (`@NotNull`), `tobacco`, `annualIncome`, `pincode`, `relationship` |
| `preferences` | map | e.g. `paymentFrequency`, `policyTerm` |
| `distribution` | object | `rmEmployeeId`, `agentId`, `channelType` (default `B2B`) |
| `journeyId`, `sessionId` | string | Bank correlation, stored on the job |
| `selection` | object | Required when `mode = SINGLE`: `insurerCode`, `productCodes[]`, `planOption`, `coverOption`, `deathBenefitOption`, `policyTerm`, `premiumPaymentTerm`, `premiumFrequency`, `premiumPaymentOption` |

```json
{
  "lob": "TERM",
  "mode": "MULTI",
  "category": "SUM_ASSURED",
  "sumAssured": 10000000,
  "members": [ { "role": "LIFE_ASSURED", "sequenceNumber": 1, "dob": "1990-04-12", "gender": "MALE",
                 "tobacco": false, "annualIncome": 1500000, "pincode": "400001" } ],
  "preferences": { "paymentFrequency": "YEARLY" },
  "distribution": { "rmEmployeeId": "E123", "agentId": "AG-77", "channelType": "B2B" },
  "journeyId": "01JQX4K8A1B2C3D4E5F6G7H8J9"
}
```

**Response `202 Accepted`**: `{ "jobId": "3f6c…", "status": "PENDING" }`.

**Behind the scenes** (`QuoteService.createQuote`):

1. `LifeQuoteValidator` — field rules per LOB; any `UNSUPPORTED_LOB` error → `422 UNSUPPORTED_LOB`,
   otherwise `422 VALIDATION_ERROR` with `errors[]`.
2. Pick the LOB handler (`LobQuoteHandlerRegistry`: Term, Savings, ULIP).
3. `POST bank-persistence /internal/v1/jobs` → `integration_job` `PENDING` with `journeyId`,
   `idempotencyKey`, actor.
4. Handler builds the 1SB payload (bank enums → 1SB master codes, distributor block injected).
5. Raw REQ stored; `POST {1SB}/insurance/lifeterm/v1/quote` (or `/insurance/lifesave/v1/quote`);
   raw RES stored; extract `reqId`.
6. `PATCH /internal/v1/jobs/{id}/status` → `RUNNING` + `externalReqId`.
7. Schedule polling on `pollingExecutor` and return immediately.
8. Publish audit `QUOTE_CREATED`.

Then asynchronously: poll `GET {1SB}/insurance/…/v1/quote/poll/{reqId}` with backoff; each attempt
→ `POST …/poll-attempts`; when complete → parse offers → `PATCH status` (`COMPLETED`, or
`PARTIAL` when some offers carry an error summary and some do not) → `POST …/offers` per offer.
Full detail: [Scenario 10](../scenarios/10-multi-quote-and-selection.md) and
[Scenario 14](../scenarios/14-provider-status-sync.md).

### 3.2 `GET /v1/quotes/{jobId}`

Response `200`:

```json
{
  "jobId": "3f6c…",
  "status": "PARTIAL",
  "offers": [
    { "offerId": "o-1", "insurerCode": "INS01", "insurerName": null, "productCode": "TERM-PLUS",
      "productName": "Term Plus", "premiumAmount": 12300.00, "premiumFrequency": "YEARLY",
      "sumAssured": 10000000.00, "outOfBound": false, "offerStatus": "OK", "errorSummary": null, "funds": [] },
    { "offerId": "o-2", "insurerCode": "INS02", "errorSummary": "Age outside product band", "…": "…" }
  ]
}
```

`status` ∈ `PENDING`, `RUNNING`, `PARTIAL`, `COMPLETED`, `FAILED`, `TIMEOUT`. Offers are returned
**only** for `COMPLETED` / `PARTIAL` — never fabricated while running. `failureReason` appears on
`FAILED` / `TIMEOUT` (`POLL_TIMEOUT`, `POLL_ERROR`, `UPSTREAM_BAD_RESPONSE`). Unknown id →
`404 RESOURCE_NOT_FOUND`. The 1SB `reqId` is stored but never returned.

### 3.3 `GET` / `POST /v1/quotes/criteria`

Gate-criteria (eligibility) form for Savings/ULIP-style products. `GET` returns a `ProposalSchema`
(pages → sections → fields) for `lob`, `productCode`, `manufacturerId`. `POST` body
`EligibilitySubmitRequest`: `lob` (`@NotNull`), `productCode` (`@NotBlank`), `manufacturerId`
(`@NotBlank`), `values` (map), `agentId` or `distribution.agentId`. Returns the eligibility result.
See [Scenario 09](../scenarios/09-pre-quote-reference-data.md).

### 3.4 `GET /v1/proposals/schema`

Query `lob` (required), `productCode`, `manufacturerId`, `version`, `quoteJobId`. Returns the 1SB
dynamic proposal form normalised into a bank envelope (`schemaId`, pages, sections, fields with
`fieldId`, `type`, `mandatory`, `pattern`, `visibility`). The caller's renderer drives the form;
this service does not interpret field semantics.

### 3.5 `POST /v1/proposals`

| Field | Meaning |
|---|---|
| `lob` | `@NotNull` |
| `schemaId`, `version`, `productCode`, `manufacturerId` | Which form was filled |
| `offerId`, `quoteJobId` | The selected offer and its quote job |
| `values` | Map of dot-notation `fieldId` → value (may contain PII — goes only to the encrypted raw-payload store) |
| `consentRef` | Consent reference (mandatory tightening `COMP-005` parked to Phase 5.5) |
| `agentId` / `distribution` | Attribution (agent required — `AGENT_ATTRIBUTION_MISSING`) |
| `journeyId`, `sessionId` | Correlation |

Response **`201 Created`** `{ "proposalJobId": "…", "status": "PENDING" }` (the WS-1 architecture
doc says `202`; the code returns `201`). Behind the scenes: `ProposalFormValidator` /
`DynamicFormValidator` against the schema → job `PENDING` → build 1SB payload →
raw REQ → `POST {1SB}/insurance/lifeterm/v1/proposal` (or lifesave) → raw RES → job `RUNNING` +
`reqId` → poll `…/proposal/poll/{reqId}` → `COMPLETED` with `applicationNumber`.
**No automatic retry on submit.** Full detail: [Scenario 11](../scenarios/11-proposal-and-underwriting.md).

### 3.6 `GET /v1/proposals/{jobId}`

`{ "jobId", "status", "applicationNumber", "failureReason", "offers": [] }` — `applicationNumber`
is withheld while `PENDING` / `RUNNING`.

### 3.7 `POST /v1/payments`

| Field | Validation |
|---|---|
| `lob` | `@NotNull` |
| `applicationNumber`, `insurerCode`, `productCode`, `redirectUrl`, `premiumFrequency` | `@NotBlank` |
| `payer.firstName`, `lastName`, `mobileNumber`, `email` | `@NotBlank` |
| `amountToBePaid` | `@NotNull` |
| `jobId`, `journeyId`, `currency`, `distribution` | optional |

Response `201 { "paymentSessionId", "paymentUrl", "expiresAt", "status": "AWAITING_PAYMENT" }`.
Behind the scenes: raw REQ → `POST {1SB}/v1/payment/url` → raw RES → `POST bank-persistence
/internal/v1/payment-sessions`. `PaymentStatus` ∈ `AWAITING_PAYMENT`, `INTIMATION_SENT`,
`PAYMENT_SUCCESS`, `PAYMENT_FAILURE`.

> **R0 rule:** the platform's premium is paid through the **AU Bank Payment Gateway** on the
> customer's device, owned by `payment-service` (C4). The 1SB payment URL is an adapter capability
> and is **not** the R0 money path. The payment-URL session table is not the Payment system of
> record. If a 1SB link is ever used, it must be delivered to the customer's device, never returned
> into an RM session.

### 3.8 `GET /v1/status/{applicationNumber}`

Query `lob`, `insurerCode` (required), `productCode`. Calls `POST {1SB}/LifeTerm/prostat/` and
normalises. Response:

```json
{ "applicationNumber": "APP123456", "bankStatus": "UW_REQUIREMENTS", "manufacturerSubStatus": "Medical pending",
  "policyNumber": null, "lastUpdated": "2026-10-21T10:00:00Z" }
```

`bankStatus` ∈ `QUOTING`, `PROPOSAL`, `UW_REQUIREMENTS`, `UW_IN_PROGRESS`, `CUSTOMER_DECISION`,
`PAYMENT`, `ISSUED`, `CLOSED`, `UNKNOWN`. The raw 1SB status string is never returned.

### 3.9 `POST /v1/master-data/lookup`

Request: `lob` (`@NotBlank`), `lookUpCategory` (default `quote`), `entityIds[]` (`@NotEmpty`,
each non-blank, e.g. `GENDER`, `OCCUPATION`), `manufacturerId`. Calls
`/insurance/lifeterm/v1/master/lookup` or `/insurance/lifesave/v1/master/lookup`; cached.
Response `{ "lob", "lookups": { "GENDER": [ {"code","label"} ] }, "cache": {"hit","stale"} }` plus
header `X-Master-Cache`. **Rule:** this is a provider feed behind the Hub — the UI must never use
1SB `entityIds` or codes as its own dropdown contract (standing constraint).

### 3.10 `GET /v1/products/ui-data` and `POST /v1/ulip/funds/{list|performance}`

Product UI data: `productId`, `manufacturerId` → `/insurance/lifeterm/v1/master/getproductuidata`;
response `{productId, manufacturerId, reqId, data}`. ULIP funds take a `CreateQuoteRequest`-shaped
body and call `/insurance/lifesave/v1/fund/list` / `/fund/performance`; response `{reqId, data}`.

---

## 4. 1SB wire endpoints used

| Bank operation | 1SB path |
|---|---|
| Term quote / poll | `POST /insurance/lifeterm/v1/quote` · `GET /insurance/lifeterm/v1/quote/poll/{reqId}` |
| Savings / ULIP quote / poll | `POST /insurance/lifesave/v1/quote` · `GET /insurance/lifesave/v1/quote/poll/{reqId}` |
| Proposal submit / poll | `POST /insurance/lifeterm/v1/proposal` · `…/proposal/poll/{reqId}` (and `lifesave`) |
| Payment URL | `POST /v1/payment/url` |
| Application status | `POST /LifeTerm/prostat/` |
| Masters | `POST /insurance/lifeterm/v1/master/lookup` · `/insurance/lifesave/v1/master/lookup` |
| Product UI data | `/insurance/lifeterm/v1/master/getproductuidata` |
| ULIP funds | `/insurance/lifesave/v1/fund/list` · `/fund/performance` |

Field-level schemas: [extracted 1SB schemas](../../../1sb-insurance-integration/reference/extracted-schemas/README.md) ·
[field guides](../../../1sb-insurance-integration/field-guides/README.md).

## 5. Sources

- `services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/api/**` · `application/QuoteService.java` · `adapter/idempotency/IdempotencyFilter.java` · `adapter/onesb/polling/AsyncJobPoller.java` · `adapter/persistence/*.java` · `src/main/resources/application.yml`
- [1SB service architecture](../../../1sb-insurance-integration/architecture/1sb-integration-service-architecture.md) · [API alignment 2026-09-03](../../../1sb-insurance-integration/service-ssot/API-ALIGNMENT-1SB-GATEWAY-2026-09-03.md) · [Resilience policy](../../../1sb-insurance-integration/service-ssot/RESILIENCE-POLICY.md)
