# Savings quote — deltas from Term

**Status:** `DOC-020` draft under `CR-014` / `EPIC-002` — confirm path and field names against the 1SB portal before marking Done.  
**API (expected):** `POST /insurance/lifesaving/v1/quote` *(confirm with 1SB)*  
**Poll (expected):** `GET /insurance/lifesaving/v1/quote/poll/:requestId` *(confirm with 1SB)*  
**LOB discriminator (bank):** `lob=SAVING`

Savings reuses the Term envelope (distributor + personalInformation + product + additionalSetup) with product-family and money-input differences.

## Controls

| Field | Required | Values / notes |
|-------|----------|----------------|
| `typeOfQuote` | Yes | `Single Quote` / `Multi-Quote` |
| `quoteCategory` | Yes | Likely `Premium` / `Sum Assured` / `Income` — **confirm allowed set for Savings** |
| `includeBI` | Strongly recommended | Savings often needs BI; default preference `withBI` unless product says otherwise |
| `distributor.*` | Yes | Same Term pattern; `channelType` `B2B` for RM-assisted |

## Members

| Field | Required | Why |
|-------|----------|-----|
| `memberType` | Yes | Typically `Life Assured` / proposer when distinct |
| `memberSequenceNumber` | Yes | Member key |
| `gender`, `dateOfBirth` | Yes | Rating / eligibility |
| `annualIncome` | Yes | Financial UW |
| `tobacco` | Conditionally | Confirm whether Savings rating uses tobacco class |
| `zipCode` | Yes | Serviceability |

## Product

| Field | Required | Why |
|-------|----------|-----|
| `product` | Yes | Savings / endowment family token — **confirm exact 1SB enum** |
| `insuranceAndProducts[]` | Conditionally | Mandatory for Single Quote pinning |
| `policyTerm` / `premiumPayingTerm` | Conditionally | Savings-specific tenor fields — confirm names |
| `premiumPaymentFrequency` | Conditionally | Single Quote / pinned products |

## Mapping notes (bank → 1SB)

- Bank `CreateQuoteCommand` stays LOB-agnostic; `SavingQuoteHandler` owns 1SB shape.
- Do **not** assemble the body via `Map.put` — use typed `adapter.onesb` request models (`REFACTOR-002`).
- Poll exhaustion follows `onesb.poll.max-attempts` / backoff (`NFR-007`).

## Open confirmations (block FUNC-015 Done)

1. Exact submit and poll paths on the 1SB sandbox.
2. Product family token and required Savings-only fields.
3. Whether BI is mandatory for Multi-Quote.
