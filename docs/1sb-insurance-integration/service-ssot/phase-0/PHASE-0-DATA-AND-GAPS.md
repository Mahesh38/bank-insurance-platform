# Phase 0 — Data received & remaining gaps

**Last updated:** 2026-07-30  
**Ack:** [PO-DEV-ENV-REQUIREMENTS.md](./PO-DEV-ENV-REQUIREMENTS.md)

---

## 1. Confirmed / decided

| Area | Value / decision |
|------|------------------|
| Demo URL | `https://demo.api.1silverbullet.tech` |
| Distributor ID | `BCIBL` |
| Sales Channel / Channel Type | `Online` / `B2B` |
| Agent ID / Alternate | `109337` / `8925` (confirm with 1SB per env) |
| Type of sale | `assisted` / `nonassisted` |
| Sample product | ICICI / E38 / GIFT Select / **LifeSave** / nonParticipating |
| LOB strategy | **Life Insurance** parent; subtypes **Term** + **Saving** (start with Saving E38; Term when listed) |
| Secrets | Configurable: **PROPERTIES** (now) → **AWS Secrets Manager** (UAT/Prod when ready) |
| Partner UAT keys | Temporary spike only; **request dedicated BCIBL keys** |
| IP whitelist | **Not required for UAT**; **Prod TODO** |
| Curl proof | **Not required for UAT**; **Prod TODO** |
| Inbound auth | Not decided → **build JWT + mTLS**, switch via config (`JWT_AND_MTLS`) |
| Catalog | Multi insurer/product **list**; more rows when 1SB sends matrix |

---

## 2. Still open (internal vs 1SB)

### Ask 1SB (see email draft)

| # | Item | Env |
|---|------|-----|
| S1 | Dedicated API Key + Secret for BCIBL | Dev/Demo, UAT, Prod |
| S2 | Base URLs if different per env | UAT, Prod |
| S3 | Full Life product matrix (Term + Saving, all insurers) | Demo, UAT, Prod |
| S4 | Confirm agent IDs `109337` / `8925` applicability | All |
| S5 | Channel / type-of-sale enums | All |
| S6 | Prod IP whitelist process + lead time | Prod |
| S7 | Samples / Postman / support contacts | All |

Email: [EMAIL-DRAFT-1SB-ONBOARDING-REQUEST.md](./EMAIL-DRAFT-1SB-ONBOARDING-REQUEST.md)

### Internal (not blocked on 1SB for scaffolding)

| # | Item | Status |
|---|------|--------|
| I1 | Secrets source config (PROPERTIES \| ENV \| AWS SM) | ✅ Example added |
| I2 | Dual inbound auth config (JWT + mTLS) | ✅ Example added |
| I3 | Life subtype catalog model (TERM + SAVING) | ✅ In products catalog |
| I4 | P0 tracking board tickets | ⬜ Create (CONFIRM-05) |
| I5 | Kickoff sign-off on Life-first / Saving subtype first | ⬜ CONFIRM-04 |
| I6 | Prod TODO: IP whitelist + connectivity proof | ⬜ Tracked for Phase 6 |

---

## 3. Env cheat-sheet

| | Dev | UAT | Prod |
|--|-----|-----|------|
| 1SB host | demo (or UAT) | UAT URL | Prod URL |
| Creds | own / temp partner | **own BCIBL** | **own BCIBL** |
| Secrets source | PROPERTIES/ENV | ENV or AWS SM | **AWS SM** |
| IP whitelist | — | Not required | **Required** |
| Inbound auth | NONE/JWT | JWT and/or mTLS | JWT and/or mTLS |

---

## 4. Scorecard

| Phase 0 item | Status |
|--------------|--------|
| Base URL | ✅ |
| Distributor + channel defaults | ✅ |
| ≥1 Life product (Saving) | ✅ E38 |
| Life Term product | ⬜ Ask 1SB |
| Dedicated API credentials | ⬜ Ask 1SB (temp partner keys only) |
| Secrets backend configurable | ✅ Design done |
| IP whitelist UAT | ✅ N/A |
| IP whitelist Prod | ⬜ TODO |
| Inbound auth dual-ready | ✅ Design done |
| Kickoff / board | ⬜ Internal |
| Multi product list from 1SB | ⬜ Ask 1SB |
