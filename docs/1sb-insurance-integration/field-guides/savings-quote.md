# Savings quote — portal-aligned

**Status:** `DOC-020` aligned to 1SB portal (2026-09-03) under `CR-014` / `EPIC-002`  
**API:** `POST /insurance/lifesave/v1/quote`  
**Poll:** `GET /insurance/lifesave/v1/quote/poll/:requestId`  
**Portal:** [Saving Get quote](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/saving-consumer-request-insurance-v-1-consumer-insurance-post) · [Poll](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/get-saving-consumer-response-insurance-v-1-request-id-get)  
**LOB discriminator (bank):** `lob=SAVING`  
**Product type (1SB):** `LifeSave`

Gateway hub: [Insurance Gateway API](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api)

Savings reuses the Term envelope (distributor + personalInformation + product + additionalSetup) with Saving-specific product filters.

## Controls

| Field | Required | Values / notes |
|-------|----------|----------------|
| `typeOfQuote` | Yes | `Single Quote` / `Multi-Quote` |
| `quoteCategory` | Yes | `Premium` / `Sum Assured` / `Income`. Live ULIP Multi-Quote for `BCIBL` succeeds with `Premium`. |
| `includeBI` | Recommended | Savings often needs BI |
| `distributor.distributorID` | Yes | Tenant id (`BCIBL` on demo) |
| `distributor.agentId` | Yes | Saving schema spelling is **`agentId`** (camelCase `d`). Sending only `agentID` yields `INSGW_NO_VALID_PRODUCT_FOUND` on lifesave even when Term accepts `agentID`. Adapter emits both. |
| `distributor.channelType` | Yes | `B2B` / `B2C` |
| `distributor.salesChannel` | Optional in docs; send `Online` | Enum `[Online, Others]` |

## Product

| Field | Required | Why |
|-------|----------|-----|
| `product.productType` | Yes | `LifeSave` (confirmed catalog / portal) |
| `product.savingsProductType[]` | Yes (live schema rejects omit) | `nonParticipating` \| `Participating` \| `ULIP`. Demo `BCIBL` catalog is **ULIP-only**; `nonParticipating` / omit both fail. Adapter default is `["ULIP"]`. |
| `insuranceAndProducts[]` | Conditionally | Mandatory for Single Quote pinning — use `insuranceCompanyCode` + `productCode[]`, not `manufacturerId` |

## Related operations (Saving category)

| Operation | Portal page |
|-----------|-------------|
| Gate criteria GET/POST | `get-savinggatecriteria-form-…` / `post-savinggatecriteria-form-…` |
| Proposal form / submit / poll | `get-saving-proposal-form-…` / `submit-saving-proposal-form-…` / `get-saving-proposal-response-…` |
| ULIP fund list / performance | See [ulip-quote.md](./ulip-quote.md) — **not** a separate quote base path |

## Mapping notes

- Handler: `SavingQuoteHandler` → paths under `/insurance/lifesave/v1/…`
- Typed body: `LifeQuoteRequest` (`REFACTOR-002`)
- Default filter in code: `savingsProductType=["ULIP"]` (live demo catalog). E38 GIFT Select (`nonParticipating`) is not currently quotable for `BCIBL`.
- ULIP is the same Saving API; bank `lob=ULIP` is a discriminator only (`UlipQuoteHandler` shares this path).
