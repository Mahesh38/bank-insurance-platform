# ULIP quote — deltas from Term

**Status:** `DOC-020` draft under `CR-014` / `EPIC-002` — confirm path and field names against the 1SB portal before marking Done.  
**API (expected):** `POST /insurance/lifeulip/v1/quote` *(confirm with 1SB)*  
**Poll (expected):** `GET /insurance/lifeulip/v1/quote/poll/:requestId` *(confirm with 1SB)*  
**LOB discriminator (bank):** `lob=ULIP`

ULIP reuses the Term envelope with fund / NAV / premium-allocation specifics.

## Controls

| Field | Required | Values / notes |
|-------|----------|----------------|
| `typeOfQuote` | Yes | `Single Quote` / `Multi-Quote` |
| `quoteCategory` | Yes | Confirm ULIP categories (often `Premium`-led) |
| `includeBI` | Strongly recommended | ULIP illustrations commonly require BI |
| `distributor.*` | Yes | Same Term pattern; `channelType` `B2B` for RM-assisted |

## Members

| Field | Required | Why |
|-------|----------|-----|
| `memberType` | Yes | `Life Assured` / proposer |
| `memberSequenceNumber` | Yes | Member key |
| `gender`, `dateOfBirth` | Yes | Rating / eligibility |
| `annualIncome` | Yes | Financial UW / suitability inputs |
| `zipCode` | Yes | Serviceability |

## Product / funds

| Field | Required | Why |
|-------|----------|-----|
| `product` | Yes | ULIP family token — **confirm exact 1SB enum** |
| `insuranceAndProducts[]` | Conditionally | Single Quote pinning |
| Fund allocation / risk profile fields | Conditionally | ULIP-specific — capture only after portal schema confirmed; do not invent fund codes |
| `premiumPaymentFrequency` / PPT / term | Conditionally | Product rules |

## Mapping notes (bank → 1SB)

- Handler package: `lob.life.ulip` (new under `EPIC-002`).
- Typed 1SB DTOs only (`REFACTOR-002`); no Map assembly.
- Suitability / risk-profile evidence remains a bank journey concern; the adapter maps fields, it does not invent suitability outcomes.

## Open confirmations (block FUNC-019 Done)

1. Exact submit and poll paths on the 1SB sandbox.
2. Fund allocation schema and mandatory risk-profile fields.
3. BI defaults for Multi-Quote vs Single Quote.
