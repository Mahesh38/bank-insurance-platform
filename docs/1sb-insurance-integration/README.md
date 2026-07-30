# 1Silverbullet (1SB) Insurance Integration Guide

**Audience:** Bank insurance platform engineering, architecture, RM journey product, and QA  
**Source docs:** [1SB Insurance Gateway API](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api) (v1.0.0)  
**Demo base URL:** `https://demo.api.1silverbullet.tech`  
**Auth:** HTTP Basic (`API_Key` / `API_Secret` from 1SB relationship manager) + IP whitelisting + `distributorId`

---

## Why this package exists

A bank wants to stand up an insurance platform for **existing customers** with an **RM-assisted journey**, using **1SB as the insurance middleware/aggregator** today, while keeping the option to **replace 1SB later** with an in-house middleware.

This package turns the scattered 1SB OpenAPI pages into:

1. A **canonical insurance domain model** (bank-owned, LOB-agnostic)
2. A **replaceable adapter architecture** (1SB today → bank middleware tomorrow)
3. A **journey-first API map** (assessment → quote → proposal → UW → payment → issue)
4. **Field-level guidance** (mandatory / when / why) for Term, Health, Motor, and shared building blocks
5. An **integration checklist** for RM-assisted bancassurance

---

## How to read this (recommended order)

### Implementing the integration service (developer SSOT)

| Order | Doc | Purpose |
|------:|-----|---------|
| 1 | **[service-ssot/README.md](./service-ssot/README.md)** | **Entry point — single source of truth index** |
| 2 | [service-ssot/00-po-architect-design-session.md](./service-ssot/00-po-architect-design-session.md) | PO ↔ Architect decisions & defaults |
| 3 | [service-ssot/PRODUCT-BACKLOG.md](./service-ssot/PRODUCT-BACKLOG.md) | Epics, stories, priorities, AC, DoD |
| 4 | [service-ssot/FUNCTIONAL-NFR-COMPLIANCE-MAP.md](./service-ssot/FUNCTIONAL-NFR-COMPLIANCE-MAP.md) | Functional vs NFR vs compliance vs shared JARs |
| 5 | [architecture/1sb-integration-service-architecture.md](./architecture/1sb-integration-service-architecture.md) | Full technical architecture |

### Background / 1SB research

| Order | Doc | Purpose |
|------:|-----|---------|
| 1 | [01-executive-overview.md](./01-executive-overview.md) | What 1SB is, platform layers, bank goal fit |
| 2 | [architecture/replaceable-middleware.md](./architecture/replaceable-middleware.md) | How to integrate without locking into 1SB |
| 3 | [canonical-model/contexts.md](./canonical-model/contexts.md) | Domain contexts and bounded contexts |
| 4 | [journeys/universal-lob-journey.md](./journeys/universal-lob-journey.md) | Universal journey + LOB deltas |
| 5 | [api-catalog/README.md](./api-catalog/README.md) | Endpoint catalog by LOB and building blocks |
| 6 | [field-guides/README.md](./field-guides/README.md) | Mandatory fields, when, why |
| 7 | [02-rm-assisted-bank-checklist.md](./02-rm-assisted-bank-checklist.md) | Practical build checklist |
| 8 | [reference/](./reference/) | Extracted schemas & source links |

---

## One-line architecture principle

> **Bank UI + RM workflow + bank domain APIs never call 1SB shapes directly.**  
> They call a **Bank Insurance Gateway** that owns canonical requests/responses.  
> A **1SB Adapter** translates canonical ↔ 1SB. Later, swap the adapter for a bank-owned carrier connector fabric.

```text
Customer / RM App
        │
        ▼
Bank Insurance Platform (suitability, journey state, CRM, KYC, payments UX)
        │
        ▼
Bank Insurance Gateway API  ←── canonical contracts (stable)
        │
        ├── 1SB Adapter (now)
        └── Bank Middleware Adapters (later, per insurer or aggregator)
                │
                ▼
         Insurers (via 1SB today)
```

---

## LOBs covered by 1SB retail docs

| LOB | 1SB category | Core path prefix (confirmed) |
|-----|--------------|------------------------------|
| Term life | Retail → Term | `/insurance/lifeterm/v1/...` |
| Health | Retail → Health | `/insurance/lifehealth/v1/...` |
| Motor | Retail → Motor | `/insurance/motor/v1/...` |
| Saving / ULIP | Retail → Saving / ULIP | Same journey pattern as Term (confirm path with 1SB RM) |
| Annuity | Retail → Annuity | Same journey pattern as Term |
| Pension | Retail → Pension | Same journey pattern as Term |
| Group / Embedded | Group | Separate catalog; secondary for bank retail launch |
| Shared ops | Building Blocks | Payment, status, KYC, OTP, docs, masters |

---

## Document maintenance note

Content was derived from the public 1SB developer portal OpenAPI pages (retail, building-blocks, group, FAQs). Enum values and product-specific proposal form fields are **dynamic** — always refresh from Master Lookup + Get Proposal Form at runtime. Treat this guide as the **integration brain**, not a frozen OpenAPI dump.
