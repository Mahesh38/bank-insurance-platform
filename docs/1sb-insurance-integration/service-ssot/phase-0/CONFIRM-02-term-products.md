# CONFIRM-02 — Insurer & Product Allow-List (multi-entry)

**Phase:** 0.2  
**Status:** `PARTIAL` — one Savings product confirmed; Term not yet; multi-insurer list incomplete  
**Owner:** Product Owner + 1SB Relationship Manager  
**Data log:** [PHASE-0-DATA-AND-GAPS.md](./PHASE-0-DATA-AND-GAPS.md)

---

## Purpose

Record **all** insurers/products enabled for distributor `BCIBL`. Catalog is a **list** (many insurers × many products). Code must not assume a single product.

Config template: [`config/catalog/products.example.yaml`](../../../../config/catalog/products.example.yaml)

---

## Confirmed entries

| manufacturerId | insurer | productCode | productName | productType | lob | enabled | status |
|----------------|---------|-------------|-------------|-------------|-----|---------|--------|
| ICICI | ICICI Prudential Life | E38 | GIFT Select | LifeSave | SAVING | true | ✅ Confirmed 2026-07-30 |

Add rows below as 1SB enables more products (same or other insurers).

| manufacturerId | insurer | productCode | productName | productType | lob | enabled | status |
|----------------|---------|-------------|-------------|-------------|-----|---------|--------|
| | | | | | | false | ⬜ Pending |

---

## Checklist

| # | Item | Status |
|---|------|--------|
| C2-1 | ≥1 manufacturerId + productCode confirmed | ✅ ICICI / E38 |
| C2-2 | LOB / productType confirmed | ✅ LifeSave → SAVING (**not Term**) |
| C2-3 | Decision: first build LOB = Saving **or** obtain Term product | ⬜ Pending kickoff |
| C2-4 | Second product/insurer for multi-quote (recommended) | ⬜ Pending |
| C2-5 | Sandbox/demo quote returns offers for E38 | ⬜ Pending (needs API key + IP whitelist) |
| C2-6 | `products.yaml` deployed to env config (not only example file) | ⬜ Pending |
| C2-7 | Distributor agreement covers each enabled product | ⬜ Confirm with 1SB RM |

**Exit criterion:** At least one `enabled: true` product for the **chosen first LOB**, quoteable in demo, plus LOB decision signed in CONFIRM-04.

---

## How multiple insurers/products work

1. Append another block under `insurance.catalog.products` — no code change.  
2. Set `lob-enabled.saving` / `term` / etc. independently.  
3. Multi-quote: call with LOB only, or pin selected catalog entries.  
4. Handlers read catalog via port — never hardcode `E38` / `ICICI` in Java.

Legacy Term-only template kept for reference: [`term-products.example.yaml`](../../../../config/catalog/term-products.example.yaml) — prefer **products.example.yaml** going forward.
