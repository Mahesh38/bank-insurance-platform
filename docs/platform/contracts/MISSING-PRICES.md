# Missing prices — monetary configuration still unset

[← Contracts index](./README.md)

Inventory of every **price / amount / monetary band** that the R0 contracts name but that this
branch deliberately does **not** invent. Sourced from a re-review of PR #150 under
`SUG-20261022-rdy`. Illustrative **OpenAPI / scenario examples** use the suitability worked
example (`SUIT-TC-REF-01`) and are **not** business thresholds.

| # | What is missing | Where it lives | Owner to set | Current strict behaviour |
|---|---|---|---|---|
| 1 | `highPremiumThreshold` (INR annual) + every other `LEAD.EXCEPTION` threshold / per-rule action | `libs/bank-rule-engine/.../LEAD.EXCEPTION.json` `parameters.*` | Exception Handling BRD owner (D2, E7, E9) | Every lead `BLOCK` / `EH-CONFIG-MISSING` until set. SAMPLE overlay `500000` exists **for tests only** (`LEAD.EXCEPTION.SAMPLE-overlay.json`) — never import |
| 2 | Product eligibility bands: `minSumAssured`, `maxSumAssured`, `minAge`, `maxAge`, `allowedOccupationBands` per `productCode` | Seed `CATALOGUE.ELIGIBILITY` (this branch); LLD §5.1 | Rajal | Every product `NOT_ELIGIBLE` / `CAT-CONFIG-MISSING` until band rows are published |
| 3 | Refund auto-approval amount threshold | `payment-service` LLD / BUILD-BRIEF (PAY-2) | Mahesh + Finance | Every refund needs a checker |
| 4 | S09 AWS cost envelope (MSK, Valkey, OpenSearch, NFW, VPN/DX, Apigee run-rate) | [`COST-ASSUMPTIONS.md`](../../governance/controls/COST-ASSUMPTIONS.md) · `DEP-20260824-cst` · `NFR-OPEN-6` | Kalpana + Shivanshi | GATE-S09 entry blocked; agents must not invent INR/USD |
| 5 | Live insurer offer premiums / tax | Quotation ← Hub ← 1SB at quote time | Insurer via 1SB | Not bank-seeded; schemas require `premiumAmount` + `taxAmount` on the selected / payment-hold path |

## Illustrative examples used in contracts (not configuration)

From suitability rule pack §3.4 (`SUIT-TC-REF-01`):

| Field | Example (INR, Money string) |
|---|---|
| Annual income | `"1200000.00"` |
| Declared / sample premium | `"25000.00"` |
| Recommended / requested cover | `"25600000.00"` |
| Existing cover (worked example) | `"500000.00"` |
| Sample offer tax (GST 18 % on premium for illustration) | `"4500.00"` |
| Sample amount payable (`premium + tax`) | `"29500.00"` |

These values appear on OpenAPI `Money` / `SelectedPremium` examples and in api-reference
scenarios 10 and 12 so developers see concrete shapes. They do **not** relax items 1–4 above.
