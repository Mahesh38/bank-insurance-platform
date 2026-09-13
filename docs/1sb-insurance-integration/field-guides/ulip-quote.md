# ULIP quote — portal-aligned (Saving API + ULIP filter)

**Status:** `DOC-020` aligned to 1SB portal (2026-09-03) under `CR-014` / `EPIC-002`  
**Quote API:** `POST /insurance/lifesave/v1/quote` with `product.savingsProductType=["ULIP"]`  
**Poll:** `GET /insurance/lifesave/v1/quote/poll/:requestId`  
**LOB discriminator (bank):** `lob=ULIP`  
**Product type (1SB):** `LifeSave` (ULIP is a Saving subtype filter, not a separate path prefix)

Gateway hub: [Insurance Gateway API](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api)

## Important portal fact

There is **no** `/insurance/lifeulip/v1/quote` in the retail portal sitemap. ULIP appears as:

1. A value of `savingsProductType` on the **Saving** quote request (`nonParticipating` | `Participating` | `ULIP`)
2. Supplementary Saving-category ops:
   - [ULIP list](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/ulip-list-saving-consumer-request-insurance-v-1-consumer-insurance-post)
   - [ULIP performance](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/ulip-performance-saving-consumer-request-insurance-v-1-consumer-insurance-post)

## Quote controls

Same envelope as [savings-quote.md](./savings-quote.md). Handler sets:

| Field | Value |
|-------|-------|
| `product.productType` | `LifeSave` |
| `product.savingsProductType` | `["ULIP"]` |

## Supplementary APIs (not the quote submit)

Portal ULIP category documents **only** these two ops (SavingConsumerRequest body):

| API | Documented 1SB path | Bank API |
|-----|---------------------|----------|
| ULIP list | `POST /insurance/lifesave/v1/fund/list` | `POST /v1/ulip/funds/list` |
| ULIP performance | `POST /insurance/lifesave/v1/fund/performance` | `POST /v1/ulip/funds/performance` |

Performance OpenAPI requires `product.insuranceAndProducts` (bank `selection.insurerCode` + `selection.productCodes`). Both POSTs require `Idempotency-Key`. Distributor `distributorID` is injected from secrets, never from the caller.

Do **not** call guessed ALB stubs (`…/quote/ulipList`, `…/quote/ulipPerformance`). There is no `/insurance/lifeulip/…` prefix and no save-quote / send-quote API.

**Sandbox (2026-09-13, `demo.api.1silverbullet.tech`):** documented `POST /insurance/lifesave/v1/fund/list` and `/fund/performance` return **404 NO_ROUTE** (auth and unauth). Unauthenticated `POST …/quote/ulipList` returns **401** because ALB treats `/quote/*` as a catch-all — that is not a live fund API. Authenticated Life calls that *are* routed still fail with `auth_api_internal_server` (500). Adapter stays on the OpenAPI paths. GATE-P4 4.1 is not claimed.

Funds **also** appear on the Saving quote poll under `productDetails.planOption.investmentOptions.fundDetails`. The adapter maps those rows onto bank `QuoteOffer.funds` (`FUNC-026`). GET `/v1/quotes/{jobId}` returns them after poll completes. `FUNC-027` is the documented list/performance pair.

## Mapping notes

- Handler: `UlipQuoteHandler` shares `/insurance/lifesave/v1/…` with Savings — no `/lifeulip` prefix
- Typed body: `LifeQuoteRequest` with ULIP filter
- Distributor: serialise `agentId` (Saving schema); `agentID` alone fails product matching
- Proposal path family: Saving proposal endpoints (portal `submit-saving-proposal-form-…`)
