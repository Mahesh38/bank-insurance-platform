# Email draft — Platform → Apigee / bank API platform (1SB reverse proxy)

**From:** Bank Platform / Bancassurance  
**To:** Apigee / API platform support  
**Cc:** Shivanshi (SRE), Amit (Engineering), Mahesh (Architecture), 1SB onboarding owner (optional)  
**Attach:** [`../../api-catalog/APIGEE-1SB-REVERSE-PROXY.md`](../../api-catalog/APIGEE-1SB-REVERSE-PROXY.md)  
**Subject:** NIP — please onboard 21 1SB Insurance Gateway APIs on Apigee as a pass-through reverse proxy (R0 Life)

---

Hi Team,

Please configure **Apigee as the outbound reverse proxy** from our Insurance Distribution Platform (NIP) to **1SilverBullet (1SB)**. Our services must not call `*.1silverbullet.tech` from EKS. 1SB will allowlist **your egress IPs**, not our spoke NAT addresses (`ADR-020`).

This ticket is **1SB egress only**. It is not inbound NIP-APP traffic (that stays on Amazon API Gateway) and not CBS / AD-verify (separate private proxies later).

### Ask in one line

**1 Apigee product, 1 reverse-proxy (or 3 prefix proxies), 3 environments, 21 1SB operations, pass-through JSON — no transformation.**

### Numbers

| | |
|---|---|
| Operations to open **now** (Wave 1) | **21** |
| Path prefixes | **3** — `/insurance/lifeterm/v1/**`, `/insurance/lifesave/v1/**`, plus 2 exact shared paths |
| LOB | Life **Term** + **Saving / ULIP** only |
| Environments | `dev` (demo target), `uat`, `prod` |
| Inbound webhooks from 1SB | **None** — we poll |

ULIP is **not** a fourth prefix. It uses `/insurance/lifesave/v1/…` with a JSON flag.

### Wave 1 API list (please proxy these paths unchanged)

**Term (9)**

1. `POST /insurance/lifeterm/v1/quote`  
2. `GET /insurance/lifeterm/v1/quote/poll/{requestId}`  
3. `GET /insurance/lifeterm/v1/quote/gateCriteria`  
4. `POST /insurance/lifeterm/v1/quote/gateCriteria`  
5. `GET /insurance/lifeterm/v1/master/getproductuidata`  
6. `GET /insurance/lifeterm/v1/proposal`  
7. `POST /insurance/lifeterm/v1/proposal`  
8. `GET /insurance/lifeterm/v1/proposal/poll/{requestId}`  
9. `POST /insurance/lifeterm/v1/master/lookup`

**Saving / ULIP (10)**

10. `POST /insurance/lifesave/v1/quote`  
11. `GET /insurance/lifesave/v1/quote/poll/{requestId}`  
12. `GET /insurance/lifesave/v1/quote/gateCriteria`  
13. `POST /insurance/lifesave/v1/quote/gateCriteria`  
14. `GET /insurance/lifesave/v1/proposal`  
15. `POST /insurance/lifesave/v1/proposal`  
16. `GET /insurance/lifesave/v1/proposal/poll/{requestId}`  
17. `POST /insurance/lifesave/v1/master/lookup`  
18. `POST /insurance/lifesave/v1/fund/list`  
19. `POST /insurance/lifesave/v1/fund/performance`

**Shared (2)**

20. `POST /LifeTerm/prostat/` (keep the trailing slash)  
21. `POST /v1/payment/url`

Do **not** open Health, Motor, CKYC, 1SB OTP, doc upload, or payment-intimation on this ticket. Do **not** open Building Blocks `POST /v1/master/lookup` (demo 404; we use #9 and #17).

### Proxy rules (please follow)

- Same method, path, query and JSON body — **no mapping**
- Target: `https://demo.api.1silverbullet.tech` for `dev`; UAT/Prod hosts from 1SB (we will confirm)
- Auth to 1SB today: HTTP Basic (API key / secret). Pass through `Authorization` **or** inject from your KVM
- Timeouts: connect ≥ 3 s, read ≥ **35 s**
- **No retry on POST** (quote/proposal submit is not idempotent at 1SB)
- **No cache**, no CORS, no body logging (PII)
- Allow only the three prefixes above; everything else 404

1SB docs: https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api  
Full pack (tables, headers, smoke sequence, Wave 2 exclusion): attached / in repo `docs/1sb-insurance-integration/api-catalog/APIGEE-1SB-REVERSE-PROXY.md`

### Please send us back

1. Apigee **base URL per env** (we will set `ONESB_BASE_URL`)  
2. **Egress IP / CIDR list per env** (we send this to 1SB for allowlist — not our NAT EIPs)  
3. How our pod authenticates to Apigee, and the **private** hostname from the insurance spoke  
4. Confirm POST retry is off and body logging is off  
5. ETA for `dev` then `uat`

Happy to walk through the 21 paths on a short call if that is faster than a ticket ping-pong.

Thanks and regards,  
[Name]  
[Role — Platform / Bancassurance]  
AU Bank Insurance Distribution Platform (NIP)  
[Email / Phone]  
Distributor ID (1SB): **BCIBL**

---

## Internal notes (do not paste into email)

- Binding hop: `ADR-020`. Dependency: `DEP-20260914-apg`. Spike leftovers: edition, private URL, per-env IPs (`SPIKE-001`).
- Adapter still defaults to `https://demo.api.1silverbullet.tech` via `ONESB_BASE_URL`.
- Fund list/performance (#18/#19) 404 on public demo — still proxy; 1SB must enable.
- R0 premium is AU Bank PG on the customer device; still proxy #21 because `FUNC-007` calls it.
- CBS / AD-verify Apigee products are a **later** mail, not this one.
- An agent must not send this as a human signature.
