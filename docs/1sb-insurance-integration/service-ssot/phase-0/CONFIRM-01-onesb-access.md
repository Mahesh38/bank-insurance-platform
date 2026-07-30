# CONFIRM-01 — 1SB Sandbox Access Confirmation

**Phase:** 0.1  
**Status:** `PARTIAL` — URL + distributor confirmed; credentials & IP whitelist still open  
**Owner:** Platform / 1SB RM  
**Last updated:** 2026-07-30  
**Data log:** [PHASE-0-DATA-AND-GAPS.md](./PHASE-0-DATA-AND-GAPS.md)

> Critical path (B, D, E) must be done before trusting demo connectivity.  
> Link: [ACTION-PLAN.md → Phase 0](../ACTION-PLAN.md#phase-0--access--alignment-before-code)

---

## Checklist

### A — Sandbox / Demo Endpoint

| # | Item | Status | Owner | Due |
|---|------|--------|-------|-----|
| A1 | Base URL obtained | ✅ `https://demo.api.1silverbullet.tech` | Product | 2026-07-30 |
| A2 | URL reachable from bank egress | ⬜ Pending | Platform | |
| A3 | URL in `config/onesb/provider-config.yml` | ✅ Done | Eng | 2026-07-30 |

Override via env: `ONESB_BASE_URL`.

---

### B — API Credentials — **STILL REQUIRED**

| # | Item | Status | Owner | Due |
|---|------|--------|-------|-----|
| B1 | `ONESB_API_KEY` from 1SB | ⬜ Pending | 1SB RM | |
| B2 | `ONESB_API_SECRET` from 1SB | ⬜ Pending | 1SB RM | |
| B3 | Stored in vault — NOT in git/chat | ⬜ Pending | Platform | |
| B4 | Vault path confirmed | ⬜ Pending | Platform | |
| B5 | Boot-time read verified | ⬜ Pending | Eng | |

**Vault path placeholders:**

```
Demo/Sandbox:
  secret/onesb/sandbox/api-key
  secret/onesb/sandbox/api-secret
```

---

### C — Distributor

| # | Item | Status | Owner | Due |
|---|------|--------|-------|-----|
| C1 | `distributorId` | ✅ `BCIBL` | Product | 2026-07-30 |
| C2 | Vault/env for shared envs | ⬜ Pending | Platform | |
| C3 | Defaults file updated | ✅ `distribution.defaults.yaml` | Eng | 2026-07-30 |

Also confirmed (defaults, overridable per journey):

| Field | Value |
|-------|-------|
| Sales Channel | Online |
| Channel Type | B2B |
| Agent ID | 109337 |
| Alternate Agent ID | 8925 |
| Type of sale | assisted / nonassisted |

---

### D — IP Whitelist / Egress — **STILL REQUIRED**

| # | Item | Status | Owner | Due |
|---|------|--------|-------|-----|
| D1 | Bank outbound CIDR identified | ⬜ Pending | Infra | |
| D2 | Submitted to 1SB RM | ⬜ Pending | Infra / PO | |
| D3 | 1SB confirms whitelist live | ⬜ Pending | 1SB RM | |
| D4 | Prod CIDR noted for later | ⬜ Pending | Infra | |

---

### E — Curl Proof — **STILL REQUIRED**

```bash
curl -v \
  --user "${ONESB_API_KEY}:${ONESB_API_SECRET}" \
  -H "Content-Type: application/json" \
  -d '{"lookUpCategory":"quote","entityIds":["CHANNEL","GENDER"]}' \
  https://demo.api.1silverbullet.tech/v1/master/lookup
```

| # | Item | Status | Owner | Due |
|---|------|--------|-------|-----|
| E1 | HTTP 200 from bank network | ⬜ Pending | Eng / Platform | |
| E2 | Sanitised proof stored (optional artefact) | ⬜ Pending | Eng | |

---

### F — Product enablement (see CONFIRM-02)

| # | Item | Status |
|---|------|--------|
| F1 | ≥1 product for distributor | ✅ ICICI / E38 GIFT Select (`LifeSave`) |
| F2 | First LOB decision (Saving vs obtain Term) | ⬜ Pending — CONFIRM-04 |

---

## Blockers If Missing

| Missing | Blocks |
|---------|--------|
| API key/secret (B) | All 1SB calls → 401 |
| IP whitelist (D) | Timeouts / connection refused |
| Curl proof (E) | No evidence connectivity works |
| LOB decision (F2) | Wrong handler built first |

---

## Sign-off

When B+D+E are ✅, set header to `Status: CONFIRMED` with name/date.

_Related: [PHASE-0-DATA-AND-GAPS.md](./PHASE-0-DATA-AND-GAPS.md) · [TODO-TRACKER.md](./TODO-TRACKER.md) · [config/onesb/](../../../../config/onesb/)_
