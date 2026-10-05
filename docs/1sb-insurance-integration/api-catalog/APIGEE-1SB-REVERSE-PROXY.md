# Apigee reverse-proxy pack — 1SB Insurance Gateway (R0)

**Audience:** Bank API platform / Apigee team (configure; do not redesign the journey)  
**From:** NIP / `1sb-integration-service` (WS-1)  
**Status:** `AI-DRAFTED` — ready to send. Does **not** close `SPIKE-001` edition / private hostname / per-env IP answers. Does **not** manufacture T4, Security or Apigee-team acceptance.  
**Work:** `SUG-20261005-apx` · `DEP-20260914-apg` · `ADR-020` · `SPIKE-001` remaining answer 4 (per-path onboard)  
**Cover email:** [`../service-ssot/phase-0/EMAIL-DRAFT-APIGEE-1SB-PROXY.md`](../service-ssot/phase-0/EMAIL-DRAFT-APIGEE-1SB-PROXY.md)

---

## 1. What we are asking you to build

Apigee is the bank’s **outbound reverse proxy** to 1SilverBullet (1SB). Our EKS pods must **never** call a `*.1silverbullet.tech` origin. The adapter’s HTTP base URL becomes **your proxy URL**. 1SB allowlists **Apigee egress IPs**, not our spoke NAT Elastic IPs (`ADR-020`).

```text
NIP domain services
  → Integration Hub
    → 1sb-integration-service  (adapter.onesb.* only)
      → Apigee reverse proxy   ← you configure this
        → 1SB Insurance Gateway
```

This pack is **1SB egress only**. It is **not**:

- inbound RM / NIP-APP traffic (that stays Cloudflare → F5-XC → **Amazon API Gateway** — `ADR-018`)
- Flutter / NIP-APP calling Apigee (forbidden)
- CBS / EBS / AD-verify (separate **private** Apigee targets; no Cloudflare/F5 hairpin)
- AU Bank Payment Gateway **callbacks** (inbound API Gateway, IP-allowlisted)

**Proxy style:** pass-through reverse proxy. Same HTTP method, path, query string and JSON body. **No** request/response transformation, **no** SOAP wrap, **no** field mapping, **no** 1SB-to-bank translation (that stays in `adapter.onesb.*`).

---

## 2. Headline numbers (Wave 1 — configure now)

| | Count |
|---|---|
| **1SB operations to proxy now** | **21** |
| Path prefixes to allowlist | **3** |
| Recommended Apigee API proxies | **1** (or 3 if you prefer one proxy per prefix) |
| Environments | **3** (`dev` / `uat` / `prod`) — separate proxy revision or env, not one shared target |
| Inbound webhooks from 1SB | **0** (we poll) |

Wave 1 is Life only: **Term** + **Saving / ULIP**. Health, Motor, Annuity, Pension, CKYC and payment-intimation stay off this ticket (`BOOT` / Phase 5).

---

## 3. Recommended Apigee product shape

| Item | Ask |
|---|---|
| **Product name** | `NIP-1SB-OUTBOUND` (or bank naming standard) |
| **Proxy name** | `nip-onesb` |
| **Base path on Apigee** | none / `/` — **preserve** the 1SB path (`/insurance/lifeterm/v1/quote` stays that path) |
| **Target** | 1SB host **per environment** (table §5) |
| **Verb** | Pass through `GET` and `POST` only |
| **Allowlist** | Only the three prefixes in §4. Everything else → **404** from Apigee (do not open the whole 1SB surface) |
| **Auth, pod → Apigee** | Bank standard for spoke → Apigee (mTLS or App key). Confirm the private hostname |
| **Auth, Apigee → 1SB** | HTTP Basic (`API_Key` / `API_Secret`) as 1SB documents today. Either **pass through** `Authorization` from the adapter **or** inject from Apigee KVM. Do **not** put 1SB secrets in git or in proxy XML in clear text |
| **TLS** | HTTPS to 1SB. If UAT/prod 1SB requires **client mTLS**, terminate on Apigee and present the bank client cert. Network Firewall on **pod → Apigee** must not need to decrypt the 1SB payload |
| **Timeout** | Target connect ≥ **3 s**; target read / io ≥ **35 s** (adapter read timeout is 30 s) |
| **Retry** | **No retry on POST**. GET poll may follow your standard idempotent GET retry |
| **Payload** | JSON. Request size ≥ **2 MB** (proposal submit). Response streaming or 2 MB buffer |
| **Query params** | Pass through unchanged (`productId`, `manufacturerId`, `version`, …) |
| **Path params** | Pass through (`{requestId}` on poll URLs) |
| **CORS** | **Off** — server-to-server only |
| **Cache** | **Off** on every resource. Quotes, proposals and identity must not be cached |
| **Spike arrest** | Fine; do not 429 a single RM quote under normal use. We are not a public API |
| **Logging** | **No request/response bodies** (PII, PAN, medical, OTP). Log verb, path **template** (not raw PAN/CIF), status, latency, `reqId` if present. No `Authorization` |

Adapter config after you publish the proxy:

```text
ONESB_BASE_URL=<Apigee proxy base, no trailing slash>
# today, before onboard: https://demo.api.1silverbullet.tech
```

---

## 4. Three prefixes to open (Wave 1)

| # | Prefix | Why |
|---|---|---|
| P1 | `/insurance/lifeterm/v1/**` | Term quote, poll, gate criteria, masters, product UI, proposal |
| P2 | `/insurance/lifesave/v1/**` | Saving + ULIP (ULIP is **not** a `/lifeulip` prefix) |
| P3 | exact paths in §6.3 | Application status + 1SB payment-URL building block |

Do **not** invent `/insurance/lifeulip/**`. ULIP uses Saving paths with `savingsProductType=["ULIP"]` in the JSON body.

---

## 5. Targets per environment

| Our Spring profile | Apigee env (suggested) | 1SB target host | Data |
|---|---|---|---|
| `dev` / `local` | `nip-dev` | `https://demo.api.1silverbullet.tech` until 1SB issues a dedicated host | Synthetic only |
| `uat` | `nip-uat` | **Please confirm** with 1SB (not the public demo if they have a UAT host) | No production CIF |
| `prod` | `nip-prod` | **Please confirm** with 1SB | Production |

`dev` may still stub 1SB in Java. **`uat` and `prod` must hit real 1SB through this proxy.**

**Please return, per environment:**

1. Proxy base URL (becomes `ONESB_BASE_URL`)
2. **Egress IP / CIDR list** that 1SB must allowlist
3. Private URL / connectivity from the insurance spoke (no Cloudflare / F5 hairpin)
4. How the pod authenticates to Apigee

---

## 6. Wave 1 operations — 21 APIs

Source of truth for **paths we actually call**: adapter handlers under `services/1sb-integration-service` (`Term*`, `Saving*`, `Ulip*`, `OneSbMasterDataAdapter`, `OneSbProductUiDataAdapter`, `OneSbStatusAdapter`, `OneSbPaymentAdapter`, `OneSbUlipFundAdapter`). Portal pages: [Insurance Gateway](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api). Alignment: [`../service-ssot/API-ALIGNMENT-1SB-GATEWAY-2026-09-03.md`](../service-ssot/API-ALIGNMENT-1SB-GATEWAY-2026-09-03.md).

### 6.1 Term — 9 operations (`P1`)

| # | Method | 1SB path (unchanged on the proxy) | Typical query | Adapter |
|---|---|---|---|---|
| 1 | `POST` | `/insurance/lifeterm/v1/quote` | — | `TermQuoteHandler` |
| 2 | `GET` | `/insurance/lifeterm/v1/quote/poll/{requestId}` | — | same, poll |
| 3 | `GET` | `/insurance/lifeterm/v1/quote/gateCriteria` | `productId`, `manufacturerId` | `LifeEligibilitySupport` |
| 4 | `POST` | `/insurance/lifeterm/v1/quote/gateCriteria` | `productId`, `manufacturerId` | same |
| 5 | `GET` | `/insurance/lifeterm/v1/master/getproductuidata` | `productId`, `manufacturerId` | `OneSbProductUiDataAdapter` |
| 6 | `GET` | `/insurance/lifeterm/v1/proposal` | `productId`, `manufacturerId`, `version` | `TermProposalHandler` |
| 7 | `POST` | `/insurance/lifeterm/v1/proposal` | — | same |
| 8 | `GET` | `/insurance/lifeterm/v1/proposal/poll/{requestId}` | — | same |
| 9 | `POST` | `/insurance/lifeterm/v1/master/lookup` | — | `OneSbMasterDataAdapter` |

### 6.2 Saving + ULIP — 10 operations (`P2`)

ULIP reuses every Saving path. Do not add a second proxy family.

| # | Method | 1SB path | Typical query | Adapter |
|---|---|---|---|---|
| 10 | `POST` | `/insurance/lifesave/v1/quote` | — | `SavingQuoteHandler` / `UlipQuoteHandler` |
| 11 | `GET` | `/insurance/lifesave/v1/quote/poll/{requestId}` | — | same |
| 12 | `GET` | `/insurance/lifesave/v1/quote/gateCriteria` | `productId`, `manufacturerId` | `LifeEligibilitySupport` |
| 13 | `POST` | `/insurance/lifesave/v1/quote/gateCriteria` | `productId`, `manufacturerId` | same |
| 14 | `GET` | `/insurance/lifesave/v1/proposal` | `productId`, `manufacturerId`, `version` | `SavingProposalHandler` / `UlipProposalHandler` |
| 15 | `POST` | `/insurance/lifesave/v1/proposal` | — | same |
| 16 | `GET` | `/insurance/lifesave/v1/proposal/poll/{requestId}` | — | same |
| 17 | `POST` | `/insurance/lifesave/v1/master/lookup` | — | `OneSbMasterDataAdapter` |
| 18 | `POST` | `/insurance/lifesave/v1/fund/list` | — | `OneSbUlipFundAdapter` |
| 19 | `POST` | `/insurance/lifesave/v1/fund/performance` | — | same |

**Known 1SB demo defect (not an Apigee defect):** #18 and #19 returned `404 NO_ROUTE` on the public demo on 2026-09-13. Still proxy them; 1SB must enable the routes on the host you target.

### 6.3 Shared building blocks — 2 operations (`P3`)

| # | Method | 1SB path | Notes |
|---|---|---|---|
| 20 | `POST` | `/LifeTerm/prostat/` | Application / policy status. Trailing slash is part of the documented path — do not strip it |
| 21 | `POST` | `/v1/payment/url` | 1SB payment-URL building block. **R0 customer premium** is AU Bank PG on the customer device; the adapter still has this call (`FUNC-007`). Proxy it |

Do **not** proxy Building Blocks `POST /v1/master/lookup` on Wave 1. Live demo **404s**; the adapter uses the LOB-scoped lookups (#9 and #17) instead.

---

## 7. Wave 2 — do **not** configure on this ticket

Onboard only when a named work item asks. Opening these now widens the 1SB blast radius for no R0 journey.

| Family | Paths (indicative) | Why later |
|---|---|---|
| Health | `/insurance/lifehealth/v1/**` | WS-1 Phase 5 |
| Motor | `/insurance/motor/v1/**` | WS-1 Phase 5 |
| Payment intimation | portal payment-intimation | `FUNC-008`, Phase 5.3 |
| Get requirements | `POST /insurance/:apiId/getReq` | not wired in the adapter yet |
| Doc upload / download | `docupload` / `docdownload` | not wired yet |
| 1SB OTP send / verify | `sendotp` / `otp-verify` | Bank consent OTP is the bank SMS gateway (`#17`), not 1SB |
| CKYC / penny-drop / customer-info | building-block pages | Health / later KYC |
| Annuity / Pension / Group | portal families | Out of R0 |
| 1SB inbound webhook | — | R0 **polls**; do not publish a public callback |

---

## 8. Headers

| Header | Direction | Action |
|---|---|---|
| `Authorization` | pod → Apigee → 1SB | Forward **or** replace with KVM Basic. Never log |
| `Content-Type` | both | Forward (`application/json`) |
| `Accept` | out | Forward |
| `Idempotency-Key` / `X-Actor-Id` | **bank inbound only** | **Do not** send to 1SB. These are our BFF/service headers |
| 1SB `reqId` (body, sometimes echoed) | response | Forward unchanged |
| Any `X-` you add for trace | optional | Fine if 1SB ignores unknown headers |

`distributorId` is **server-derived in the adapter**. Apigee must not overwrite body fields.

---

## 9. Timeouts, retries, failure

| Call class | Adapter budget | Apigee target |
|---|---|---|
| Connect | 3 s | ≥ 3 s |
| Quote / proposal **POST** (submit) | 30 s read, **no auto-retry** | ≥ 35 s; **retry = 0** |
| Gate / master / product UI / status / payment-URL POST or GET | 30 s read | ≥ 35 s; no POST retry |
| Quote / proposal **GET poll** | backoff 1 s → 30 s cap, ≤ 20 attempts from Java | GET retry OK; do not change poll path |

On 1SB 4xx/5xx: return the **same status and body** to the adapter. Do not map 1SB errors into Apigee fault JSON on Wave 1 (we already normalise in Java). A generic Apigee 503 when the target is down is acceptable.

---

## 10. Security and compliance (non-negotiable)

- Server-to-server only. No product subscription for NIP-APP / Flutter.
- No PII in Apigee analytics, debug, or trace in `uat`/`prod`. Disable Trace in prod.
- India residency for any Apigee log/analytics sink that could see metadata.
- Do not publish **our** inspection-VPC NAT EIPs to 1SB. Send **your** egress IPs to the 1SB onboarding thread (`DEP-20260824-eip` rebases onto `DEP-20260914-apg`).
- Internal bank APIs (CBS, AD-verify) are a **different** Apigee product and must stay **private**.

---

## 11. Suggested call order (so you can smoke-test the proxy)

Term happy path the adapter will exercise after onboard:

```text
POST /insurance/lifeterm/v1/master/lookup
GET  /insurance/lifeterm/v1/quote/gateCriteria?productId=&manufacturerId=
POST /insurance/lifeterm/v1/quote
GET  /insurance/lifeterm/v1/quote/poll/{requestId}
GET  /insurance/lifeterm/v1/master/getproductuidata?productId=&manufacturerId=
GET  /insurance/lifeterm/v1/proposal?productId=&manufacturerId=&version=
POST /insurance/lifeterm/v1/proposal
GET  /insurance/lifeterm/v1/proposal/poll/{requestId}
POST /LifeTerm/prostat/
```

Saving / ULIP: same sequence under `/insurance/lifesave/v1/…` plus `POST …/fund/list` and `POST …/fund/performance` when the product is ULIP.

A 401 from 1SB on a routed path is useful (proves the proxy). A 404 from Apigee means the prefix was not allowed. A 404 from 1SB on #18/#19 is the known demo gap.

---

## 12. What we need back (checklist)

- [ ] Product + proxy created in `dev` / `uat` / `prod`
- [ ] 21 Wave-1 paths pass through (or the three prefixes allow them)
- [ ] `ONESB_BASE_URL` values for all three envs
- [ ] Egress IP/CIDR list per env (for 1SB allowlist)
- [ ] Private connectivity note (spoke → Apigee, no public hairpin)
- [ ] Auth method pod → Apigee, and whether Apigee injects 1SB Basic or passes ours
- [ ] Target timeouts ≥ 35 s; POST retry disabled
- [ ] Body logging disabled
- [ ] Wave-2 prefixes **not** opened

Portal: [https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api)  
Internal catalog: [`README.md`](./README.md)

---

## 13. Authority and residuals

| Residual | Owner |
|---|---|
| Apigee edition, private hostname, per-env IPs | Bank API platform + Shivanshi (`SPIKE-001`) |
| 1SB dedicated credentials + their allowlist of **your** IPs | Product / 1SB RM (`CONFIRM-01`) |
| Spoke firewall on pod → Apigee | Deepali (`ADR-010` remainder) |
| This path list | Amit / WS-1 adapter (this file) |
| Human send of the cover email | Bancassurance / Platform (not an agent signature) |
