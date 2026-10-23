# 1sb-integration-service — low-level design

**Status:** AI-DRAFTED from code (ADR-027), awaiting engineering and Deepali (security) review.

[Contract (authoritative)](./openapi.yaml) · [API portal page](../../api-reference/services/1sb-integration-service.md) ·
[Adapter to 1SB](../../api-reference/interactions/onesb-adapter-to-1sb.md) ·
[Adapter to persistence](../../api-reference/interactions/onesb-adapter-to-persistence.md) ·
[Idempotency, timeouts, retries](../../api-reference/conventions/idempotency-timeouts-retries.md) ·
[ADR-027](../../architecture-review/ADR-027-openapi-contract-authority.md)

## 1. Purpose and ownership

The bank's translation booth for 1SilverBullet (1SB). It takes bank-canonical requests, builds 1SB
wire payloads **per LOB** (`lob.life.term`, `lob.life.saving`, `lob.life.ulip`), calls 1SB, polls
its async jobs, normalises statuses and offers back to bank language and records every exchange.
1SB vocabulary never leaves `adapter.onesb.*` (ArchUnit). It owns **no datasource**: jobs, offers,
poll attempts, raw payloads and payment sessions are written to `bank-persistence-service`.

| Item | Value |
|---|---|
| Module | `services/1sb-integration-service`, port 8080, `service-id: onesb`, L5, INTERNAL |
| Callers | Target: Integration Hub only (`IF-1`); today tests / UAT drives |
| Calls out | 1SB (UAT/prod via Apigee, `ADR-020`) with `Authorization: Basic apiKey:apiSecret`; bank-persistence `/internal/v1/*` with `X-Internal-Service-Key` (`BANK_PERSISTENCE_INTERNAL_KEY`; fix-pending with the persistence caller-auth change) |
| Inbound auth | None in code (cluster-private; target mesh mTLS) |

Source: `services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/**`.

## 2. API summary

| Method | Path | Idempotency-Key | Success | LOBs |
|---|---|---|---|---|
| POST | `/v1/quotes` | required | **202** `{jobId, PENDING}` | TERM, SAVING, ULIP |
| GET | `/v1/quotes/{jobId}` | – | 200 | – |
| GET | `/v1/quotes/criteria` | – | 200 form | LOBs whose handler has a criteria path |
| POST | `/v1/quotes/criteria` | required | 200 | same |
| GET | `/v1/proposals/schema` | – | 200 | TERM, SAVING, ULIP |
| POST | `/v1/proposals` | required | **201** (`COMPLETED` or `PENDING`) | TERM, SAVING, ULIP |
| GET | `/v1/proposals/{jobId}` | – | 200 | – |
| POST | `/v1/payments` | required | **201** | any registered |
| GET | `/v1/status/{applicationNumber}` | – | 200 | any |
| POST | `/v1/master-data/lookup` | **exempt** | 200 + `X-Master-Cache` | Term / Save master paths |
| GET | `/v1/products/ui-data` | – | 200 | Term |
| POST | `/v1/ulip/funds/list` | required | 200 | ULIP |
| POST | `/v1/ulip/funds/performance` | required | 200 | ULIP |

Upstream paths per LOB: Term `/insurance/lifeterm/v1/{quote,quote/poll/{reqId},proposal,proposal/poll/{reqId},master/lookup,master/getproductuidata}`;
Saving/ULIP `/insurance/lifesave/v1/{quote,quote/poll/{reqId},proposal,proposal/poll/{reqId},master/lookup,fund/list,fund/performance}`.

## 3. Operation flows

### 3.1 Idempotency filter (all mutating `/v1/**` except master data)

1. No key → `400 MISSING_IDEMPOTENCY_KEY` (envelope written by the filter).
2. Hash the raw body (SHA-256). Key known: same hash → replay stored status / body; different →
   `409 IDEMPOTENCY_CONFLICT`.
3. Unknown key → run the handler; store the response if status < 500 (`putIfAbsent`).

### 3.2 Quote

```mermaid
sequenceDiagram
    participant Hub as Caller (Hub)
    participant A as 1sb-integration
    participant P as bank-persistence
    participant S as 1SB
    Hub->>A: POST /v1/quotes (Idempotency-Key)
    A->>A: LifeQuoteValidator (422 on any field error)
    A->>P: POST /internal/v1/jobs (QUOTE, PENDING)
    A->>S: POST /insurance/life{term|save}/v1/quote (no auto retry)
    S-->>A: reqId
    A->>P: PATCH /internal/v1/jobs/{id}/status (RUNNING, externalReqId)
    A-->>Hub: 202 {jobId, PENDING}
    loop AsyncJobPoller: 1 s ×2, cap 30 s, ≤ 20 attempts
        A->>S: GET …/quote/poll/{reqId}
        A->>P: POST …/poll-attempts
    end
    A->>P: POST …/offers ; PATCH status COMPLETED / PARTIAL / TIMEOUT (POLL_TIMEOUT) / FAILED (POLL_ERROR)
    Hub->>A: GET /v1/quotes/{jobId} → offers only when COMPLETED / PARTIAL
```

### 3.3 Proposal

1. `lob` → attribution (`AGENT_ATTRIBUTION_MISSING`) → `consentRef` (`403 CONSENT_REQUIRED`,
   audit `CONSENT_REF_MISSING`) → values / product / manufacturer present → answers valid against the
   loaded schema → bound quote job usable (`410 QUOTE_EXPIRED`) and product matches a selectable
   offer (`422 VALIDATION_ERROR`).
2. Create `PROPOSAL` job; store REQUEST raw payload **before** the call (evidence; one retry;
   failure → job `FAILED`, `503`, nothing sent).
3. Submit; insurer business refusal → `422 PROPOSAL_REJECTED` (audit). Sync completion → job
   `COMPLETED` with `applicationNumber`; else `PENDING` + poll. Response `201` either way.

### 3.4 Payment link

`lob`, `applicationNumber` → optional `jobId` payable check (`409 PROPOSAL_NOT_PAYABLE`) →
evidence-first raw payload → 1SB payment URL → must be `https://` (`502`) → payment session
`AWAITING_PAYMENT` in persistence → audit `PAYMENT_URL_RETRIEVED` → `201`.

### 3.5 Status, master data, product UI, ULIP funds

- Status: `lob`, `applicationNumber`, `insurerCode` → 1SB status → normalised `bankStatus`
  (`UW_REQUIREMENTS`, `UW_IN_PROGRESS`, `CUSTOMER_DECISION`, `PAYMENT*`→`PAYMENT`,
  `POLICY_ISSUED`→`ISSUED`, closed set → `CLOSED`, else `UNKNOWN`); raw text dropped; audit
  `APPLICATION_STATUS_CHECKED`.
- Master data: in-process cache by `(lob, category, manufacturer, entityIds)` with TTL; upstream
  failure serves a cached entry as `STALE`, else `503`.
- Product UI: `productId`, `manufacturerId` required; empty data → `502 UPSTREAM_BAD_RESPONSE`.
- ULIP funds: `lob = ULIP`, `sumAssured`, `members`; performance also needs the pin.

## 4. Validation rules (quote — `LifeQuoteValidator`, errors collected per field)

| Field | Rule | Code |
|---|---|---|
| `lob` | not null; TERM / SAVING / ULIP | `MISSING_REQUIRED_FIELD` / `UNSUPPORTED_LOB` |
| `mode` | blank, SINGLE or MULTI (aliases) | `VALIDATION_ERROR` |
| `category` | blank, PREMIUM, SUM_ASSURED or INCOME | `VALIDATION_ERROR` |
| `sumAssured` | required, > 0 | `MISSING_REQUIRED_FIELD` / `VALIDATION_ERROR` |
| `premiumAmount` | > 0; required when category PREMIUM | `VALIDATION_ERROR` / `MISSING_REQUIRED_FIELD` |
| `members` | non-empty; ≥ 1 life assured; role LIFE_ASSURED / PROPOSER | `MISSING_REQUIRED_FIELD` / `VALIDATION_ERROR` |
| `members[i].dob` | required; strict YYYY-MM-DD; not future; age 18–75 | `MISSING_REQUIRED_FIELD` / `VALIDATION_ERROR` |
| `members[i].gender` | Male / Female / Transgender / Others / M / F | `VALIDATION_ERROR` |
| `members[i].annualIncome` | required, > 0 | `MISSING_REQUIRED_FIELD` / `VALIDATION_ERROR` |
| `members[i].pincode` | required, `^[1-9][0-9]{5}$` | `MISSING_REQUIRED_FIELD` / `VALIDATION_ERROR` |
| `members[i].relationship` | required on PROPOSER in SINGLE | `MISSING_REQUIRED_FIELD` |
| `distribution.agentId` (or `rmEmployeeId`) | required | `MISSING_REQUIRED_FIELD` |
| `distribution.channelType` | blank, B2B or B2C | `VALIDATION_ERROR` |
| `selection.insurerCode`, `productCodes` | required in SINGLE | `MISSING_REQUIRED_FIELD` |
| `selection.policyTerm`, `premiumPaymentTerm` | 1–80; PPT ≤ PT | `VALIDATION_ERROR` |
| `selection.premiumFrequency` | blank, M, Q, HY, Y, S | `VALIDATION_ERROR` |
| `preferences.savingsProductType` (SAVING) | required; nonParticipating / Participating / ULIP | `MISSING_REQUIRED_FIELD` / `VALIDATION_ERROR` |

Top-level code: `422 UNSUPPORTED_LOB` if any field error is `UNSUPPORTED_LOB`, else
`422 VALIDATION_ERROR`. Proposal, eligibility, payment, status, product UI and ULIP rules are in
the contract descriptions. The 18–75 age band and 1–80 term limits are code constants today; under
`ADR-024` they belong in product configuration (open item).

Rule-engine usage: none today. Product eligibility has two sources (catalogue `eligibility_band`
vs 1SB gate criteria) with undefined precedence (readiness review §3.4).

## 5. DTO → domain → persistence mapping

| DTO | Domain | bank-persistence row |
|---|---|---|
| `CreateQuoteRequest` + key + actor | `CreateQuoteCommand` | `integration_job` (`job_type QUOTE`, `idempotency_key`, `created_by_actor`) |
| 1SB `reqId` | – | `integration_job.external_req_id` |
| poll result | `QuoteOffer` | `integration_job_offer` (+ `funds_json` for ULIP) |
| each poll | – | `job_poll_attempt` |
| 1SB request / response bodies | – | `raw_payload` (encrypted, `retain_until` +7 y) |
| `SubmitProposalRequest` | `SubmitProposalCommand` | `integration_job` (`PROPOSAL`, `application_number`) |
| `CreatePaymentRequest` | `CreatePaymentCommand` → `PaymentSession` | `payment_session` |

## 6. Invariants

| Invariant | Where |
|---|---|
| `distributorId` never caller-supplied (`INV-DIS-01`, `COMP-004`) | no request field; injected from secrets |
| No offers fabricated while in progress | `QuoteController.offersForStatus` |
| No `applicationNumber` leaked while in progress | `ProposalController.applicationNumberForStatus` |
| Consent reference before proposal submit (C2) | `ProposalService.submit` |
| Evidence before regulated calls (proposal, payment) | raw payload stored first, else no call |
| HTTPS payment URLs only | `PaymentService` |
| Submits never auto-retried | `OneSbHttpClient` / services |

## 7. Events, idempotency, timeouts, failure modes

- **Events:** none published. Audit actions (`QUOTE_CREATED`, `PROPOSAL_SUBMITTED`,
  `CONSENT_REF_MISSING`, `PAYMENT_URL_RETRIEVED`, `APPLICATION_STATUS_CHECKED`) go to
  `LoggingAuditEventPublisher` (log only); durable audit transport is `ADR-026` (**proposed**).
- **Idempotency:** filter as above; per-pod memory store (open item: shared store).
- **Timeouts:** 1SB connect 3 s / read 30 s (`ONESB_*_TIMEOUT_MS`); circuit breaker opens after 5
  failures for 30 s. Polling 1 s × 2, cap 30 s, 20 attempts.
- **Failure modes:** 1SB 401 → `502 UPSTREAM_AUTH_FAILURE`; 4xx → `422 UPSTREAM_BUSINESS_ERROR`
  (field errors preserved); 5xx / timeout / open circuit → `503 UPSTREAM_UNAVAILABLE`; other →
  `502 UPSTREAM_BAD_RESPONSE`. Persistence returning empty or failing is an unmapped
  `IllegalStateException` → Spring `500` (open item).

## 8. Deviations and open items

| # | Item | Owner |
|---|---|---|
| 1 | Proposal submit is `201` even when async (`PENDING`); quote is `202` — documented as is | Engineering (any change is a reviewed contract change) |
| 2 | `QUOTE_EXPIRED` answered as `410` (FUNC-004 AC-2) vs catalogue `409` | Engineering / QA |
| 3 | Idempotency store per pod; filter error content type `application/json` not `application/problem+json` | Engineering |
| 4 | Missing query params / bad `lob` enum → Spring 400 without envelope | Engineering |
| 5 | Persistence failures surface as unmapped 500 | Engineering |
| 6 | No inbound caller authentication | Deepali / Shivanshi (mesh) |
| 7 | Age / term limits hard-coded; should be configuration (`ADR-024`) | Rajal |
| 8 | Audit only logged | Mahesh / Shailja (`ADR-026` proposed) |
