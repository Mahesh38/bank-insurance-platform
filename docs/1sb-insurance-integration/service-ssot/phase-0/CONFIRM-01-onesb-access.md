# CONFIRM-01 — 1SB Sandbox Access Confirmation

**Phase:** 0.1  
**Status:** `PENDING`  
**Owner:** _assign below_  
**Due:** _fill in_  
**Last updated:** 2026-07-30

> This checklist must be fully ticked ✅ before Phase 1 (foundations + HTTP client) can begin.  
> Blockers in the final section explain exactly what breaks if each item is missing.  
> Link: [ACTION-PLAN.md → Phase 0](../ACTION-PLAN.md#phase-0--access--alignment-before-code)

---

## Checklist

### A — Sandbox Endpoint

| # | Item | Status | Owner | Due |
|---|------|--------|-------|-----|
| A1 | Sandbox base URL obtained from 1SB RM | ⬜ Pending | | |
| A2 | URL reachable from bank egress (curl/telnet port 443 succeeds) | ⬜ Pending | | |
| A3 | URL recorded in `config/onesb/provider-config.yml` sandbox profile | ⬜ Pending | | |

Expected URL pattern (placeholder until confirmed): `https://sandbox.1silverbullet.tech`

---

### B — API Credentials

| # | Item | Status | Owner | Due |
|---|------|--------|-------|-----|
| B1 | `ONESB_API_KEY` received from 1SB | ⬜ Pending | | |
| B2 | `ONESB_API_SECRET` received from 1SB | ⬜ Pending | | |
| B3 | Credentials stored in vault (path below) — NOT in git | ⬜ Pending | | |
| B4 | Vault path confirmed with Platform team | ⬜ Pending | | |
| B5 | Service can read credentials at boot (vault test or env-var smoke test) | ⬜ Pending | | |

**Vault path placeholders** (confirm with Platform):

```
Sandbox:
  secret/onesb/sandbox/api-key
  secret/onesb/sandbox/api-secret

Prod (for reference, Phase 6):
  secret/onesb/prod/api-key
  secret/onesb/prod/api-secret
```

**Vault path owner:** _TBD — Platform / DevSecOps team_

---

### C — Distributor ID

| # | Item | Status | Owner | Due |
|---|------|--------|-------|-----|
| C1 | `distributorId` assigned by 1SB for sandbox | ⬜ Pending | | |
| C2 | `distributorId` stored in vault: `secret/onesb/sandbox/distributor-id` | ⬜ Pending | | |
| C3 | Value injected via `ONESB_DISTRIBUTOR_ID` env var and verified at boot | ⬜ Pending | | |

---

### D — IP Whitelist / Egress

| # | Item | Status | Owner | Due |
|---|------|--------|-------|-----|
| D1 | Bank outbound IP range (CIDR) identified for sandbox environment | ⬜ Pending | | |
| D2 | Outbound IP range submitted to 1SB RM for whitelisting | ⬜ Pending | | |
| D3 | 1SB confirms whitelist is live for sandbox | ⬜ Pending | | |
| D4 | Bank prod CIDR identified (for Phase 6 — document now) | ⬜ Pending | | |

**Infra/Network team contact for CIDR:** _TBD_  
**1SB RM contact for whitelist requests:** _TBD_

---

### E — Curl Proof

> Run this command once items A–D are complete. Replace placeholders before running.  
> This proves end-to-end connectivity before any code is written.

```bash
# Template — fill in real values; do NOT commit output to git
curl -v \
  --user "${ONESB_API_KEY}:${ONESB_API_SECRET}" \
  -H "Content-Type: application/json" \
  -d '{"distributorId":"${ONESB_DISTRIBUTOR_ID}","masterType":"INSURER"}' \
  https://sandbox.1silverbullet.tech/insurance/v1/master/lookup

# Expected: HTTP 200 with JSON array of insurers
# If 401: credentials incorrect or not yet active
# If connection refused / timeout: IP not whitelisted or URL wrong
```

| # | Item | Status | Owner | Due |
|---|------|--------|-------|-----|
| E1 | Curl proof command executed successfully (HTTP 200) | ⬜ Pending | | |
| E2 | Response saved as `docs/.../phase-0/sandbox-curl-proof.json` (sanitise keys before committing) | ⬜ Pending | | |

---

### F — Term Product Enablement (Phase 0.2 dependency)

| # | Item | Status | Owner | Due |
|---|------|--------|-------|-----|
| F1 | At least one Term product confirmed quotable in sandbox for this distributor | ⬜ Pending | | |
| F2 | Product ID and manufacturer ID noted for fixture data | ⬜ Pending | | |

_This item is owned by [Action 0.2](../ACTION-PLAN.md#phase-0--access--alignment-before-code) but recorded here for single-checklist convenience._

---

## Blockers If Missing

| Missing item | What it blocks |
|---|---|
| Sandbox URL (A) | Cannot wire `provider-config.yml`; cannot run connectivity smoke test |
| API key/secret (B) | HTTP client cannot authenticate; all 1SB calls return 401 |
| Vault path / access (B4, B5) | Service won't boot in any environment (fail-fast on missing secrets — by design) |
| Distributor ID (C) | All API requests will be rejected by 1SB (distributorId is mandatory on every call) |
| IP whitelist (D) | All outbound calls time-out or are refused; no amount of correct credentials helps |
| Curl proof (E) | No sandbox evidence → Phase 1 HTTP client work proceeds without validation |
| Term product enabled (F) | Cannot run Phase 3 Term vertical-slice integration tests |

---

## Sign-off

When all items are ✅, update this file header:

```
Status: CONFIRMED
Confirmed by: <name>
Date confirmed: <YYYY-MM-DD>
```

and link to the vault path + curl proof artefact in the row notes above.

---

_Related: [TODO-TRACKER.md](./TODO-TRACKER.md) · [ACTION-PLAN.md](../ACTION-PLAN.md) · [config/onesb/provider-config.yml](../../../../config/onesb/provider-config.yml)_
