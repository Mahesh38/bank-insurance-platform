# quotation-service — Quotation, Life cell (#10)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | Owns the **bank's** quote: request, multi-insurer price discovery through the Integration Hub, offer validation, selection, expiry. Enforces **C1** at the point of use (re-checks suitability itself; never trusts a flag on the journey). The quote id is bank-minted (ULID) — the provider job id is only an external reference (`TI-22`) |
| **Module** | `services/quotation-service` · port 8096 · `service-id: quotation` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1/quotes` (HLD §5.3) |
| **Callers** | Journey (create, select) · BFF (poll reads) · Payment (selected premium read, `INV-PAY-03`) |
| **Calls out** | Suitability (S-08 gate, 500 ms, fail closed) · Integration Hub (S-09 create, async-poll, **no retry on submit**) · Lead (`bi-generated` on first successful BI) · Catalogue (offer eligibility) · Configuration (validity window) |
| **Owns data** | **DynamoDB `{env}-quotation-working`** — working quote (control attributes + client-side-encrypted envelope) until payment (`DB-DEC-0005`) · Aurora schema **`quotation`** — `quote` + `offer` structured after `RECONCILED`, `idempotency_record` · role `app_quotation` |
| **Code status** | **PARTIAL — persistence only** (`SUG-20261021-lft`): DynamoDB working-state store, envelope encryption (AES-GCM, KMS data key), materialiser into structured Aurora rows, `POST /internal/v1/events/payment-reconciled`, `GET /internal/v1/quotes/{quoteId}/selected`; tested against embedded DynamoDB Local. Public command API not built. Paths CONTRACTED (HLD §5.1/§5.3); bodies **PROPOSED** from the domain model, DDL and the IF-1 adapter contract |

---

## 1. API index

| Method | Path | Status | Caller | Purpose |
|---|---|---|---|---|
| `POST` | `/internal/v1/quotes` | CONTRACTED · body PROPOSED | Journey | Create quote (C1 gate) → dispatch to Hub; `202` |
| `GET` | `/internal/v1/quotes/{quoteId}` | CONTRACTED | BFF (via journey route), Journey | Status + offers |
| `POST` | `/internal/v1/quotes/{quoteId}/selection` | PROPOSED | Journey | Select one offer atomically |
| `GET` | `/internal/v1/quotes/{quoteId}/selected` | PROPOSED | Payment | Selected premium, tax, sum assured, insurer, product (control columns only) |
| (event) | consumes `PaymentReconciled` | PROPOSED | MSK | Materialise structured `quote` + `offer` |

---

## 2. State model

`REQUESTED → IN_PROGRESS → QUOTED | PARTIALLY_QUOTED | FAILED | TIMED_OUT`; then
`→ SELECTED → CONVERTED` (proposal created) or `→ EXPIRED`; `REQUESTED → REJECTED` when the
suitability gate refuses (so the refusal is a **domain record**, not just a 403).
`PARTIALLY_QUOTED` **is success** (`INV-QUO-02`).

---

## 3. Endpoints (PROPOSED shapes)

### 3.1 `POST /internal/v1/quotes`

Headers: `X-Actor-Id`, `Idempotency-Key` = `journey:{journeyId}:quote:{n}` (server-derived).

```json
{
  "leadId": "01JQ…", "journeyId": "01JQ…", "customerId": "CUST-0433",
  "suitabilityId": "01JR…", "consentId": "01JS…",
  "lob": "LIFE", "productClass": "TERM", "mode": "MULTI", "category": "SUM_ASSURED",
  "requestedCover": 10000000,
  "members": [ { "role": "LIFE_ASSURED", "sequence": 1, "dob": "1990-04-12", "gender": "MALE",
                 "tobacco": false, "annualIncome": 1500000, "pincode": "400001" } ],
  "preferences": { "paymentFrequency": "YEARLY" }
}
```

Validation: ids present; `lob = LIFE`; product class in R0 set; members non-empty with DOB and
gender; no `distributorId` / agent fields (`422 ATTRIBUTION_NOT_CALLER_SUPPLIED`).

**Behind the scenes**

```text
1  C1 gate: GET suitability /assessments/{suitabilityId}/validity?customerId&lob   (500 ms, no retry)
     not usable / timeout / error → PutItem QUOTE#id/META state REJECTED
                                    outbox QuoteRejectedNoSuitability (Audit mandatory)
                                    → 403 SUITABILITY_REQUIRED
2  Resolve validity window and product matrix from configuration (fail closed)
3  ONE DynamoDB TransactWriteItems (quotation-working):
     Put QUOTE#{quoteId}/META   (state REQUESTED, control attributes, payloadEnc = encrypted request, ttl)
     Put IDEM#{key}/IDEM ; Put JOURNEY#{journeyId}/OPEN_QUOTE ; Put QUOTE#{quoteId}/OUTBOX#{evt} QuoteRequested
4  POST Hub /internal/v1/provider-requests {operation: QUOTE, canonical body}
     Idempotency-Key = quote:{quoteId}:submit     ← no automatic retry on submit
   → providerRequestId (Hub) ; Hub → 1SB adapter POST /v1/quotes → jobId
5  UpdateItem META state IN_PROGRESS, provider, providerJobId (CONDITION version)
6  202 { quoteId, state: IN_PROGRESS }
```

### 3.2 `GET /internal/v1/quotes/{quoteId}`

While `IN_PROGRESS`, Quotation polls the Hub (S-11: 1 s → 30 s cap, bounded) — or reacts to a
Hub completion — then ingests offers:

| Rule | Effect |
|---|---|
| `premium.amount <= 0` or `sumAssured <= 0` | Offer `INVALID`, excluded from selection, reason recorded (`INV-QUO-03`, `VR-081`) |
| ≥ 1 valid offer, all insurers answered | `QUOTED` |
| ≥ 1 valid offer, some insurers failed | `PARTIALLY_QUOTED` — per-insurer errors shown beside offers |
| 0 valid offers | `FAILED` (never a zero-offer success) |
| Poll budget exhausted | `TIMED_OUT` → journey `ABANDONED` with re-quote path |
| First successful BI | `POST lead /leads/{leadId}/bi-generated` (lead → `QUALIFIED`) and suitability `lock` |

Response:

```json
{
  "quoteId": "01JT…", "state": "PARTIALLY_QUOTED", "validUntil": "2026-10-28T23:59:59+05:30",
  "offers": [
    { "offerId": "01JU…", "insurerCode": "INS01", "productCode": "TERM-PLUS", "productName": "Term Plus",
      "premiumAmount": 12300.00, "taxAmount": 2214.00, "premiumFrequency": "YEARLY",
      "sumAssured": 10000000, "coverTerm": 30, "premiumPayingTerm": 30, "outOfBound": false, "offerState": "AVAILABLE" }
  ],
  "partialErrors": [ { "insurerCode": "INS02", "code": "UPSTREAM_BUSINESS_ERROR" } ]
}
```

Offers live **inside the encrypted envelope of the DynamoDB item** until payment; the response is built in process.

### 3.3 `POST /internal/v1/quotes/{quoteId}/selection`

```json
{ "offerId": "01JU…" }
```

One conditional `UpdateItem` on `QUOTE#{quoteId}/META` (strongly consistent read first); state must be `QUOTED`/`PARTIALLY_QUOTED`; `now <
valid_until` (else `409 QUOTE_EXPIRED`, `INV-QUO-04`); offer must be `AVAILABLE`; write
`selected_offer_id`, `selected_premium_amount`, `selected_tax_amount`, `selected_sum_assured`,
`selected_insurer_code`, `selected_product_code` **and** rewrite `payload_enc` marking every sibling
`NOT_SELECTED` (`INV-QUO-05` — never two selected); state `SELECTED`; outbox `OfferSelected`.
Response `200 { quoteId, state: "SELECTED", selectedOfferId }`.

### 3.4 `GET /internal/v1/quotes/{quoteId}/selected`

For Payment only: `{ quoteId, offerId, premiumAmount, taxAmount, sumAssured, insurerCode, productCode, currency: "INR" }`
from control attributes — Payment never decrypts the envelope. This read also sets `paymentInFlight = true` and removes the item's `ttl` so it cannot expire while money is moving.

### 3.5 On `PaymentReconciled`

Idempotent consumer: read the DynamoDB item → ONE Aurora transaction INSERT `quotation.quote`
+ one `quotation.offer` per envelope offer (`ON CONFLICT DO NOTHING`; selected = `SELECTED`, others `NOT_SELECTED`) → then DynamoDB `SET materializedAt` with `attribute_not_exists(materializedAt)` (item frozen, short TTL). See
[payload storage](../data/payload-storage-and-transactions.md#33-materialisation-into-aurora-after-reconciled).

## 4. Errors

`403 SUITABILITY_REQUIRED` · `409 QUOTE_EXPIRED` · `409 ILLEGAL_TRANSITION` · `409
IDEMPOTENCY_CONFLICT` · `422 ATTRIBUTION_NOT_CALLER_SUPPLIED` · `422 CONFIGURATION_UNRESOLVABLE` ·
`504 QUOTE_TIMEOUT` (read after budget) · `503 UPSTREAM_UNAVAILABLE` (Hub down — journey stage unchanged).

## 5. Sources

- [Domain model §4.4, §6](../../ws3-platform/01-domain-model-and-invariants.md) · [`DB-DEC-0005`](../../data-architecture/DB-DEC-0005-dynamodb-working-payload.md) · [DynamoDB design](../../data-architecture/schemas/20-dynamodb-working-payload.md) · [`08-quotation.sql`](../../data-architecture/schemas/08-quotation.sql) · [06 working payload](../../data-architecture/06-journey-orchestrator-and-working-payload.md) · [External call catalogue §2.2](../../../journey-execution/05-EXTERNAL-CALL-CATALOGUE.md) · [R0 solution architecture §5](../../ws3-platform/03-solution-architecture-r0.md)
