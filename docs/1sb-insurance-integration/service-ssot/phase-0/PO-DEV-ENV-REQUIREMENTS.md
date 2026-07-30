# PO ↔ Dev acknowledgement — Phase 0 clarifications

**Date:** 2026-07-30  
**Participants:** Product Owner, Engineering  
**Status:** Accepted — drives config + 1SB ask email

---

## 1. What Product & Dev acknowledge

| Your input | Team decision |
|------------|---------------|
| Partner UAT API key/secret available temporarily; own credentials to be requested | **OK for local/dev spike only.** Track own `BCIBL` credentials as must-have. Never commit partner secrets. Document “borrowed UAT creds” as temporary in runbook. |
| AWS Secrets Manager later; properties file for now | **Configurable secrets source by env:** `PROPERTIES` \| `ENV` \| `AWS_SECRETS_MANAGER`. Dev/local = properties/env; UAT/Prod = AWS SM when ready. |
| UAT does **not** need IP whitelist | Mark IP whitelist as **Prod TODO** (not a UAT blocker). |
| UAT does **not** need curl proof from bank network | Mark curl proof as **Prod TODO** / optional for UAT. Local connectivity check still useful for eng. |
| Inbound auth not decided; services in same K8s | **Implement both JWT and mTLS adapters**; select via `auth.mode` / dual-ready. Default in-cluster can be mTLS later; JWT also ready for gateway path. |
| Start with **Life Insurance**; Saving & Term as subtypes | Reframe LOB: `LIFE` parent; subtypes `TERM` \| `SAVING` (ULIP/Annuity/Pension later under Life as needed). First vertical uses confirmed **Saving (E38)** while Term catalog fills in. |
| More insurers/products from 1SB later | Keep **list catalog**; email 1SB for full enabled product matrix per env. |
| Tracking board | Still create P0 tickets (internal) — not blocked on 1SB. |

---

## 2. Environment model (Dev / UAT / Prod)

```text
                    DEV (local/K8s-dev)     UAT                         PROD
1SB host            demo/UAT host           UAT host                    Prod host
Credentials         own when available;     own BCIBL UAT keys          own BCIBL prod keys
                    temp partner OK short
Secrets source      PROPERTIES / ENV        AWS SM (or ENV until SM)    AWS Secrets Manager
IP whitelist        N/A / not required      Not required (per bank)     REQUIRED — TODO
Connectivity proof  Eng optional curl       Optional                    REQUIRED — TODO
Inbound auth        NONE or JWT (dev)       JWT and/or mTLS             JWT and/or mTLS
Distributor         BCIBL                   BCIBL                       BCIBL (confirm prod id)
Catalog             demo products           UAT-enabled list            Prod-enabled list
```

**Replaceability:** same service binary; only config/profile + secrets backend + catalog change per env.

---

## 3. What we need from 1SB (by environment)

### Common (all envs)
1. Confirmation that distributor **`BCIBL`** is the correct id for our bank entity in each env  
2. **Dedicated API Key + Secret** for our partner (not shared with another partner long-term)  
3. Full **enabled product matrix**: insurer code, manufacturerId, productCode, product name, product type (LifeTerm / LifeSave / …), subtype flags (e.g. nonParticipating)  
4. Valid **agent / SP / PoSP** codes for our channel (`109337`, `8925` — confirm which env and which insurers they apply to)  
5. Sales channel / channel type / type-of-sale **allowed enums** for B2B assisted journeys  
6. Base URLs for **UAT and Prod** (demo URL already known: `https://demo.api.1silverbullet.tech`)  
7. Support contact + SLA for sandbox/UAT incidents  
8. Any **Postman collection / sample payloads** for LifeSave + LifeTerm quote → proposal → payment → status  

### Dev
- Access to demo or UAT APIs with our (or temporary) credentials  
- At least one LifeSave + ideally one LifeTerm product enabled for BCIBL  

### UAT
- Dedicated UAT API Key + Secret for BCIBL  
- UAT base URL (if different from demo)  
- Full UAT product/insurer list for Life (Term + Saving)  
- Confirmation **IP whitelist not required** for UAT (already assumed)  
- Test cards / payment URL behaviour notes if applicable  

### Prod
- Prod API Key + Secret for BCIBL  
- Prod base URL  
- Prod product/insurer allow-list  
- **IP whitelist process**: which bank egress CIDRs to submit, lead time, confirmation mail  
- Prod agent codes / distributor confirmation  
- Go-live checklist / change window expectations  

---

## 4. Internal engineering follow-ups (not 1SB)

| Item | Action |
|------|--------|
| Secrets SPI | `secrets.source=PROPERTIES\|ENV\|AWS_SECRETS_MANAGER` |
| Inbound auth | Both JWT + mTLS handlers; config switch |
| Life LOB model | Parent LIFE; subtypes TERM, SAVING |
| Prod TODO backlog | IP whitelist + curl/connectivity proof |
| Partner UAT keys | Use only until own keys issued; rotate when BCIBL keys arrive |
| Catalog | Append insurers/products as 1SB sends list |
| Tracking | Create P0 tickets from story board |

---

## 5. Outputs

- Config: `config/onesb/secrets-source.example.yaml`, auth dual-mode notes, Life LOB catalog notes  
- Email draft for Product → 1SB: [EMAIL-DRAFT-1SB-ONBOARDING-REQUEST.md](./EMAIL-DRAFT-1SB-ONBOARDING-REQUEST.md)  
- Updated gaps: [PHASE-0-DATA-AND-GAPS.md](./PHASE-0-DATA-AND-GAPS.md)
