# Scenario 10 — Multi-quote: request, poll, partial success, select an offer

[← API reference home](../README.md) · Prev: [09](./09-pre-quote-reference-data.md) · Next: [11](./11-proposal-and-underwriting.md)

**Status:** the **provider half is IMPLEMENTED** (1SB adapter: job, submit, poll, offers,
persistence). The platform half (BFF route → Journey → Quotation → Hub) is **CONTRACTED /
PROPOSED**.

**"Multi-quote"** = `mode: MULTI`: one bank quote fans out to every eligible insurer in the
matrix through 1SB, and the RM compares the offers. `mode: SINGLE` pins one product (`selection`).

## 1. End-to-end sequence

```mermaid
sequenceDiagram
    autonumber
    participant App as NIP-APP
    participant BFF as BFF
    participant JO as journey-orchestration
    participant QS as quotation-service
    participant SU as suitability-service
    participant HUB as integration-hub
    participant A as 1sb-integration-service
    participant P as bank-persistence
    participant SB as 1SB (via Apigee)
    participant LS as lead-service

    App->>BFF: POST /api/v1/journeys/{jid}/quotes {cover, members, preferences} (Idempotency-Key)
    BFF->>JO: quotes (stage must be CONSENT_CAPTURED)
    JO->>QS: POST /internal/v1/quotes {refs from journey_ref: suitabilityId, consentId}
    QS->>SU: GET …/validity?customerId&lob (500 ms, no retry)
    alt not usable / timeout
        QS->>QS: DynamoDB item REJECTED · outbox QuoteRejectedNoSuitability
        QS-->>JO: 403 SUITABILITY_REQUIRED
        JO-->>BFF: 403 (stage unchanged)
    end
    QS->>QS: DynamoDB TransactWriteItems: META REQUESTED (encrypted request) + IDEM + OPEN_QUOTE + OUTBOX QuoteRequested
    QS->>HUB: POST /internal/v1/provider-requests {QUOTE, MULTI} (key quote:{qid}:submit)
    HUB->>HUB: route LIFE/TERM → ONE_SB · inject distributor/agent · bulkhead
    HUB->>A: POST /v1/quotes (Idempotency-Key)
    A->>A: LifeQuoteValidator · TermQuoteHandler
    A->>P: POST /internal/v1/jobs → PENDING
    A->>P: raw REQ
    A->>SB: POST /insurance/lifeterm/v1/quote
    SB-->>A: {reqId}
    A->>P: raw RES · PATCH status RUNNING + externalReqId
    A-->>HUB: 202 {jobId, PENDING}
    HUB-->>QS: 202 {providerRequestId}
    QS->>QS: UpdateItem IN_PROGRESS, providerJobId
    QS-->>JO: 202 {quoteId}
    JO->>JO: stage QUOTING · ref quoteId
    BFF-->>App: 202 {quoteId}

    loop adapter poller (1 s, 2 s, 4 s … ≤ 30 s, max 20)
        A->>SB: GET …/quote/poll/{reqId}
        A->>P: POST poll-attempts
    end
    A->>P: PATCH COMPLETED | PARTIAL · POST offers

    loop app polls
        App->>BFF: GET /api/v1/journeys/{jid}/quotes/{quoteId}
        BFF->>QS: GET /internal/v1/quotes/{quoteId}
        QS->>HUB: POST /internal/v1/provider-requests/{id}
        HUB->>A: GET /v1/quotes/{jobId}
        A->>P: GET job + offers
        A-->>HUB: {status, offers}
        HUB-->>QS: canonical
        QS->>QS: validate offers (premium > 0, SA > 0) · QUOTED / PARTIALLY_QUOTED / FAILED · rewrite envelope
        QS-->>BFF: {state, offers, partialErrors, validUntil}
        BFF-->>App: 200
    end
    QS->>LS: POST /internal/v1/leads/{leadId}/bi-generated (first BI → QUALIFIED)
    QS->>SU: POST …/lock {quoteId}

    App->>BFF: POST /api/v1/journeys/{jid}/quotes/{quoteId}/selection {offerId}
    BFF->>JO: selection
    JO->>QS: POST /internal/v1/quotes/{quoteId}/selection
    QS->>QS: one txn: valid_until? · selected_* cols + envelope (siblings NOT_SELECTED) · SELECTED · outbox OfferSelected
    QS-->>JO: 200
    JO->>JO: stage QUOTE_SELECTED · ref offerId
    BFF-->>App: 200
```

## 2. Request (BFF, contract)

Money fields are **decimal strings** (OpenAPI `Money`). Figures below are the suitability worked
example (`SUIT-TC-REF-01`) — illustrative, not bank pricing policy
([MISSING-PRICES.md](../../contracts/MISSING-PRICES.md)).

```json
{
  "mode": "MULTI",
  "category": "SUM_ASSURED",
  "requestedCover": "25600000.00",
  "members": [ { "role": "LIFE_ASSURED", "dob": "1988-04-12", "gender": "MALE", "tobacco": false,
                 "annualIncome": "1200000.00", "pincode": "400001" } ],
  "preferences": { "paymentFrequency": "YEARLY" }
}
```

No `suitabilityId`, `consentId`, `distributorId` or agent id in the body — Journey supplies the
references, the Hub supplies attribution.

## 3. Sample poll body when `state = QUOTED`

Insurer premiums arrive at quote time via Hub / 1SB (not bank-seeded). Example shape after
validation (`premium > 0`, `sumAssured > 0`):

```json
{
  "quoteId": "01HZXEXAMPLE00000000000001",
  "state": "QUOTED",
  "offers": [
    {
      "offerId": "01HZXEXAMPLE00000000000002",
      "insurerCode": "INS01",
      "productCode": "TERM-BASIC-01",
      "premiumAmount": "25000.00",
      "taxAmount": "4500.00",
      "sumAssured": "25600000.00",
      "premiumFrequency": "YEARLY",
      "offerState": "AVAILABLE",
      "outOfBound": false
    },
    {
      "offerId": "01HZXEXAMPLE00000000000003",
      "insurerCode": "INS02",
      "productCode": "TERM-PLUS-01",
      "premiumAmount": "27500.00",
      "taxAmount": "4950.00",
      "sumAssured": "25600000.00",
      "premiumFrequency": "YEARLY",
      "offerState": "AVAILABLE",
      "outOfBound": false
    }
  ],
  "validUntil": "2026-11-08T18:30:00+05:30"
}
```

Amount payable on the selected offer = `premiumAmount + taxAmount` (here `"29500.00"` for INS01).

## 4. Poll response states the app must render

| `state` | Meaning | App |
|---|---|---|
| `IN_PROGRESS` | Insurers still answering | Keep polling (back off: 1 s → 5 s) |
| `QUOTED` | All insurers answered | Show offers |
| `PARTIALLY_QUOTED` | Some offers + some insurer failures | Show offers **and** which insurers failed — this is success |
| `FAILED` | Zero valid offers | Offer re-quote |
| `TIMED_OUT` | Poll budget exhausted | Offer re-quote (journey may go `ABANDONED`) |
| `REJECTED` | Suitability gate refused | Go back to suitability |
| `EXPIRED` | Validity window passed | Re-quote |

## 5. Where every byte goes

| Data | Store |
|---|---|
| Bank quote (request + all offers) | DynamoDB `{env}-quotation-working`, item `QUOTE#{quoteId}/META`: `payloadEnc` (encrypted) + control attributes (`DB-DEC-0005`) |
| Adapter job, offers, poll attempts | `bank_persistence.integration_job`, `integration_job_offer`, `job_poll_attempt` |
| Exact 1SB request / response JSON | `bank_persistence.raw_payload` (AES-GCM, 7 years) |
| Journey stage and `quoteId` / `offerId` | `journey.journey`, `journey_ref`, `journey_transition` |
| Lead BI flag | `lead_lms.lead` (`bi_generated`, `reporting_class = ELIGIBLE`, `QUALIFIED`) |
| Evidence | Outbox → MSK → audit: `QuoteRequested`, `QuoteCompleted`, `OfferSelected` (+ adapter `QUOTE_CREATED`) |
| Structured `quote` / `offer` | Aurora `quotation`, **only after** `PaymentReconciled` (materialisation) |

## 6. Errors

`403 SUITABILITY_REQUIRED` · `409 QUOTE_EXPIRED` (select too late) · `409 ILLEGAL_TRANSITION` (wrong
stage) · `422 VALIDATION_ERROR` / `UNSUPPORTED_LOB` (adapter field rules) · `409
IDEMPOTENCY_CONFLICT` · `503 UPSTREAM_UNAVAILABLE` (Hub / provider breaker open) · `504
QUOTE_TIMEOUT`.

## 7. Pages

[Quotation](../services/quotation-service.md) · [Integration Hub](../services/integration-hub-service.md) · [1SB adapter](../services/1sb-integration-service.md) · [Domain to Hub](../interactions/domain-to-integration-hub.md) · [1SB adapter to 1SB](../interactions/onesb-adapter-to-1sb.md) · [Payload storage](../data/payload-storage-and-transactions.md)
