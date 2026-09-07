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
| `quoteCategory` | Yes | `Premium` / `Sum Assured` / `Income` |
| `includeBI` | Recommended | Savings often needs BI |
| `distributor.*` | Yes | Same Term pattern; `channelType` `B2B` for RM-assisted |

## Product

| Field | Required | Why |
|-------|----------|-----|
| `product.productType` | Yes | `LifeSave` (confirmed catalog / portal) |
| `product.savingsProductType[]` | Yes (Saving schema) | `nonParticipating` \| `Participating` \| `ULIP` — filters Saving family |
| `insuranceAndProducts[]` | Conditionally | Mandatory for Single Quote pinning |

## Related operations (Saving category)

| Operation | Portal page |
|-----------|-------------|
| Gate criteria GET/POST | `get-savinggatecriteria-form-…` / `post-savinggatecriteria-form-…` |
| Proposal form / submit / poll | `get-saving-proposal-form-…` / `submit-saving-proposal-form-…` / `get-saving-proposal-response-…` |
| ULIP fund list / performance | See [ulip-quote.md](./ulip-quote.md) — **not** a separate quote base path |

## Mapping notes

- Handler: `SavingQuoteHandler` → paths under `/insurance/lifesave/v1/…`
- Typed body: `LifeQuoteRequest` (`REFACTOR-002`)
- Default filter in code today: `savingsProductType=["nonParticipating"]` (E38 GIFT Select)
