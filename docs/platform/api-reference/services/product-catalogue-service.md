# product-catalogue-service — Product catalogue (#8)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | The bank-controlled answer to *"what may we sell, to whom, from which insurer"*: insurers (Group A / B), products, LOB and product class, effective-dated eligibility bands. Suitability (mapping output) and Quotation (offer eligibility) read the **same** matrix. It is bank configuration — **not** 1SB's product list leaking in |
| **Module** | `services/product-catalogue-service` · port 8094 · `service-id: catalogue` · layer L5 · INTERNAL |
| **Contract** | [`openapi.yaml`](../../contracts/product-catalogue-service/openapi.yaml) · [LLD](../../contracts/product-catalogue-service/LLD.md) — authoritative per [ADR-027](../../architecture-review/ADR-027-openapi-contract-authority.md); AI-DRAFTED; eligibility thresholds unset, awaiting Rajal |
| **Base path** | `/internal/v1/offerings` (HLD §5.3) |
| **Callers** | BFF (`/catalogue/product-classes`, `/catalogue/offerings`) · Suitability · Quotation |
| **Calls out** | administration-config (seeded versions) · Valkey `catalogue:` L2 read-through |
| **Owns data** | Aurora schema **`catalogue`** (`insurer`, `product`; `eligibility_band` superseded by rule set `CATALOGUE.ELIGIBILITY` — DDL change pending) · role `app_catalogue` · Valkey ACL user + prefix `catalogue:` |
| **Code status** | **SKELETON**. `GET /internal/v1/offerings` CONTRACTED; other shapes **PROPOSED**. Today the BFF serves product classes from a static list |

## 1. API index

| Method | Path | Status | Purpose |
|---|---|---|---|
| `GET` | `/internal/v1/offerings?lob&productClass&insurerId&at` | CONTRACTED | Eligible offerings (R0: LIFE · Group A · TERM/SAVINGS/ULIP) |
| `GET` | `/internal/v1/product-classes?lob` | PROPOSED | Replaces the BFF's static list (`SUG-20261007-srp`) |
| `GET` | `/internal/v1/products/{productCode}` | PROPOSED | One product with eligibility bands |
| `GET` | `/internal/v1/products/{productCode}/insurer-criteria?insurerId` | PROPOSED | Insurer criteria form, fetched through the Integration Hub |
| `POST` | `/internal/v1/eligibility:check` | PROPOSED | Two stages: rule set `CATALOGUE.ELIGIBILITY` first, then insurer criteria via the Hub; both must pass; precedence configurable ([LLD §5](../../contracts/product-catalogue-service/LLD.md#5-eligibility--two-sources-one-precedence)) |

## 2. Behaviour

- Response example (`GET /offerings?lob=LIFE&productClass=TERM`):
  `{ "items": [ { "insurerId": "INS01", "insurerGroup": "A", "productCode": "TERM-PLUS", "productName": "Term Plus", "lob": "LIFE", "productClass": "TERM", "effectiveFrom": "2026-10-01" } ] }`.
  Partner reps see only their own insurer (`(lob, insurerId)` scope).
- Resolution order: in-process L1 → Valkey L2 (S-25, 50 ms, a miss is a read) → Aurora. Timeout
  500 ms, 1 retry, breaker; degraded mode = read cache. Never serve past TTL when the store is down
  for a regulated decision.
- Group B insurers: catalogue entry + controlled redirect — **R1**, out of R0.

## 3. Data

`insurer` (`insurer_id`, `name`, `group_class`, `active`, effective window) · `product`
(`product_code`, `insurer_id`, `lob`, `product_class`, `version`, `payload` JSONB, effective
window) · `eligibility_band` (age, sum assured, occupation band, effective window).
DDL [`07-catalogue.sql`](../../data-architecture/schemas/07-catalogue.sql).

## 4. Sources

- [R0 HLD Boundary 4, §5.3](../../../architecture/R0-HLD.md#53-internal-service-apis-cluster-private) · [Store segregation](../../data-architecture/04-store-segregation-and-roadmap.md)
