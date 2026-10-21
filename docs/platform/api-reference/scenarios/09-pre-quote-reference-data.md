# Scenario 09 — Pre-quote reference data: products, masters, eligibility criteria, ULIP funds

[← API reference home](../README.md) · Prev: [08](./08-consent-otp.md) · Next: [10](./10-multi-quote-and-selection.md)

**Status:** adapter side **IMPLEMENTED** (1SB masters, product UI data, eligibility criteria, ULIP
funds); BFF product classes **IMPLEMENTED (static)**; catalogue offerings, config forms and the Hub
read path **CONTRACTED / PROPOSED**.

## 1. The rule that shapes this scenario

**The UI and the BFF never receive 1SB or insurer wire codes.** Hop is UI → BFF → owning domain
service → Integration Hub → adapter. 1SB master lookups are a **provider feed behind the adapter**,
not a frontend contract (standing constraint, `SUG-20260913-acl`). Dropdown values the RM sees are
**bank** codes owned by the bank (catalogue / configuration), mapped to provider codes inside the
Hub / adapter.

## 2. Who serves what

| RM needs | BFF route | Served by | Behind it |
|---|---|---|---|
| Product classes (Term / Savings / ULIP) | `GET /api/v1/catalogue/product-classes` | BFF static list today → catalogue (target) | `catalogue.product` |
| Offerings for this LOB / insurer | `GET /api/v1/catalogue/offerings` | product-catalogue | `catalogue.*` + Valkey L2 |
| Field rules for a form (lengths, patterns, required) | `GET /api/v1/config/forms/{formId}` | administration-config | `administration.configuration_record` |
| Occupation / gender / frequency option lists | via the owning domain service | Catalogue / config (bank codes) | Seeded from 1SB masters through Hub `MASTER_LOOKUP` → adapter `POST /v1/master-data/lookup` (cached, `X-Master-Cache`) |
| Eligibility (gate) criteria for Savings / ULIP | via Quotation | Hub `ELIGIBILITY_CRITERIA_GET/SUBMIT` | adapter `GET`/`POST /v1/quotes/criteria` |
| Product brochure / UI copy | via Catalogue | Hub `PRODUCT_CONTENT` | adapter `GET /v1/products/ui-data` → `/insurance/lifeterm/v1/master/getproductuidata` |
| ULIP fund list and performance | via Quotation | Hub `ULIP_FUNDS` | adapter `POST /v1/ulip/funds/list` · `/performance` |

## 3. Example — eligibility criteria for a ULIP product

```mermaid
sequenceDiagram
    participant BFF
    participant QS as quotation-service
    participant HUB as integration-hub
    participant A as 1SB adapter
    participant SB as 1SB (via Apigee)
    BFF->>QS: get criteria (productCode, insurerCode)
    QS->>HUB: POST /internal/v1/provider-queries {ELIGIBILITY_CRITERIA_GET, lob LIFE, productClass ULIP, insurerCode}
    HUB->>HUB: route → ONE_SB; map insurerCode → manufacturerId (inside Hub/adapter)
    HUB->>A: GET /v1/quotes/criteria?lob=ULIP&productCode&manufacturerId
    A->>SB: gate-criteria form API
    SB-->>A: form
    A-->>HUB: ProposalSchema (pages/sections/fields, bank envelope)
    HUB-->>QS: canonical form
    QS-->>BFF: form for the RM
```

Submitting answers: `POST /v1/quotes/criteria` requires an `Idempotency-Key` at the adapter
(mutating `/v1`), plus `lob`, `productCode`, `manufacturerId`, `values`, and an agent id.

## 4. Caching

| Data | Cache | TTL owner |
|---|---|---|
| 1SB masters | Adapter (`MastersCacheProperties`) | Adapter config |
| Proposal / criteria schemas | Adapter (WS-1 design: 1 h) | Adapter config |
| Catalogue | L1 in-process → Valkey `catalogue:` | Catalogue |
| Configuration | L1 → L2 to resolution TTL; fail closed after | Config |

## 5. Pages

[Integration Hub](../services/integration-hub-service.md) · [1SB adapter §3.3, §3.9, §3.10](../services/1sb-integration-service.md#33-get--post-v1quotescriteria) · [Product catalogue](../services/product-catalogue-service.md) · [Administration config](../services/administration-config-service.md)
