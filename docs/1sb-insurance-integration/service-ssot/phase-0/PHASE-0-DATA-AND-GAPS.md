# Phase 0 — Data received & remaining gaps

**Last updated:** 2026-07-30  
**Source:** Bank / 1SB onboarding data provided by product team

This document records what you already have, what was written into config templates, and **what is still required** before Phase 0 can close. Multiple insurers/products are first-class (config lists, not single hardcodes).

---

## 1. Confirmed from your input

| Area | Field | Value | Config location |
|------|-------|-------|-----------------|
| Endpoint | Demo base URL | `https://demo.api.1silverbullet.tech` | `config/onesb/provider-config.yml` → `ONESB_BASE_URL` |
| Distributor | Distributor ID | `BCIBL` | `config/onesb/distribution.defaults.yaml` (non-secret default; override via env in higher envs if needed) |
| Distributor | Sales Channel | `Online` | same |
| Distributor | Channel Type | `B2B` | same |
| Distributor | Agent ID | `109337` | same (default / demo agent; per-journey override from bank caller) |
| Distributor | Alternate Agent ID | `8925` | same |
| Distributor | Type of sale | `assisted` / `nonassisted` | same (enum; journey supplies actual value) |
| Insurer | Code / Name / Manufacturer ID | `ICICI` / ICICI Prudential Life / `ICICI` | `config/catalog/products.example.yaml` |
| Product | Code / Name | `E38` / GIFT Select | same |
| Product | Product type (LOB) | **`LifeSave`** (Savings) | same — see §3 |
| Product | Savings product type | `nonParticipating` | same |

---

## 2. Still required for Phase 0 (do not skip)

### Must-have (blocks sandbox connectivity / go-live of Phase 0 exit)

| # | Item | Why | Checklist |
|---|------|-----|-----------|
| R1 | **API Key** (`ONESB_API_KEY`) | HTTP Basic Auth to 1SB | [CONFIRM-01](./CONFIRM-01-onesb-access.md) B1 |
| R2 | **API Secret** (`ONESB_API_SECRET`) | HTTP Basic Auth | CONFIRM-01 B2 |
| R3 | Credentials in **vault** (not git/chat) | Security | CONFIRM-01 B3–B5 |
| R4 | **IP whitelist** — bank egress CIDR submitted + 1SB confirmed | Calls fail without it | CONFIRM-01 D1–D3 |
| R5 | **Curl proof** from bank network (HTTP 200) | Proves A–D together | CONFIRM-01 E1 |
| R6 | **Inbound auth mode** chosen: `JWT` or `MTLS` (+ issuer/JWKS or truststore) | Bank→service security | [CONFIRM-03](./CONFIRM-03-inbound-auth.md) |
| R7 | **Kickoff sign-off** (Case 2, LOB order, DRY/KISS) | Alignment | [CONFIRM-04](./CONFIRM-04-ssot-kickoff.md) |
| R8 | **P0 tickets** created on board | Tracking | [CONFIRM-05](./CONFIRM-05-tracking-board.md) |

### Strongly recommended before coding handlers

| # | Item | Why |
|---|------|-----|
| R9 | **Second insurer and/or product** for multi-quote testing | Your first entry is only ICICI / E38; multi-quote needs ≥2 enabled products or Multi-Quote against LOB without pin |
| R10 | Confirm whether **demo = sandbox** for your tenant or a separate sandbox URL exists | URL is set; still verify reachability + RM confirmation |
| R11 | Confirm **agent mapping**: is `109337` SP/PoSP for ICICI? When to use `8925`? | Wrong agentId → quote/proposal rejection |
| R12 | Confirm **Type of sale** values 1SB expects (`assisted` vs `nonassisted` spelling/enums) | Master lookup / varFields |
| R13 | Decide **first LOB to build** (see §3) | Plan said Term; your enabled product is LifeSave |

### Nice-to-have / later

| # | Item |
|---|------|
| R14 | Prod base URL + prod distributor/credentials (Phase 6) |
| R15 | Health / Motor / Term product rows when those LOBs are contracted |
| R16 | Per-RM agent codes table (bank HR/CRM → insurer agentId map) |

---

## 3. Important LOB note

Your confirmed product is:

```text
Product Type = LifeSave   →  1SB path family: /insurance/lifesave/...
NOT LifeTerm
```

Original action plan prioritized **Term first**. You now have a **Savings** product ready.

**Decision needed (CONFIRM-04 kickoff):**

| Option | Meaning |
|--------|---------|
| **A (recommended if only E38 is live)** | Change P0 vertical slice to **Saving (`lifesave`)** using GIFT Select / E38 |
| **B** | Keep Term-first plan — ask 1SB to enable at least one **LifeTerm** product for `BCIBL`, keep E38 as P1 Savings |
| **C** | Parallel stubs — still ship one LOB end-to-end first (KISS); do not build both at once |

Until this is decided, do not start Term handlers against a Savings-only catalog.

---

## 4. Multiple insurers & products — how we model it

Config is a **list**, not a single row:

```yaml
insurance.catalog.products:   # many entries
  - manufacturerId / productCode / lob / enabled / ...
  - ...
```

Rules:
- Add another insurer = new list entry (no code change)
- Disable a product = `enabled: false`
- Multi-quote can use LOB-level request without pinning, **or** pin via `insuranceAndProducts[]` from this allow-list
- Distribution defaults (channel, default agent) are separate from catalog; journey can override `agentId` / `typeOfSale` per request

See: [`config/catalog/products.example.yaml`](../../../../config/catalog/products.example.yaml)  
Distributor defaults: [`config/onesb/distribution.defaults.yaml`](../../../../config/onesb/distribution.defaults.yaml)

---

## 5. Quick status scorecard

| Phase 0 item | Status |
|--------------|--------|
| 0.1 Base URL | ✅ Provided |
| 0.1 Distributor ID | ✅ Provided (`BCIBL`) |
| 0.1 API key/secret | ❌ Missing |
| 0.1 IP whitelist | ❌ Missing |
| 0.1 Curl proof | ❌ Missing |
| 0.2 ≥1 product+insurer | ✅ Provided (ICICI / E38) — **LOB = LifeSave** |
| 0.2 Multi insurer/product | ⚠️ Structure ready; only 1 row filled — add more when available |
| 0.3 Inbound auth | ❌ Missing |
| 0.4 Kickoff / LOB decision | ❌ Missing (Term vs Save) |
| 0.5 Tracking board | ❌ Missing |

**Phase 0 is not complete** until R1–R8 are done and the LOB decision (R13) is signed.
