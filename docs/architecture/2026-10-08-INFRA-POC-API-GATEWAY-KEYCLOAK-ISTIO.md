# Infra POC / justification pack — Amazon API Gateway, Keycloak, and why Istio / External ALB are not substitutes (R0)

**Audience:** Infra Head · AWS / bank platform (infra) team · SRE (Shivanshi) · Security (Deepali) · Architecture (Mahesh)  
**Purpose:** Detailed written justification for the R0 inbound edge and identity stack — suitable for an infra design review or landing-zone challenge.  
**Date:** 2026-10-08  
**Standing:** AI-drafted evidence under [`CR-017`](../governance/change-requests/CR-017-inbound-api-gateway-vs-istio.md) / [`ADR-022`](../platform/architecture-review/08-architecture-decision-log.md) / `SUG-20261006-apg` (recurrence). Does **not** manufacture Architecture, Security, or Infra Head sign-off. If this file disagrees with an ADR, the ADR wins (`HA-02`).

**Freshness note:** `docs/governance/04-STAGE_GATES.md` was past its 14-day artefact window at drafting (WARN). Stage posture still comes from BOOT / `CURRENT-STATE.yaml`; this POC does not change stage state.

**One-screen companion:** [`2026-10-06-PLATFORM-TEAM-NOTE-API-GATEWAY-VS-ISTIO.md`](./2026-10-06-PLATFORM-TEAM-NOTE-API-GATEWAY-VS-ISTIO.md)  
**Canonical BOM / hops:** [`R0-LLD.md`](./R0-LLD.md) §1.1, §1.3, §3  
**Identity SSOT:** [`docs/platform/authentication-authorization/README.md`](../platform/authentication-authorization/README.md)  
**Binding ADRs:** `ADR-018` · `ADR-020` · `ADR-022` (Proposed) · `ARCH-018`/`ARCH-019` · `CR-012` §3

---

## 0. Executive ask (what we need from infra)

1. **Provision** Amazon API Gateway as the **first AWS inbound hop** (Proxy 1), VPC-linked to an **Internal ALB** (Proxy 2) — `ADR-018`.  
2. **Do not provision** an External / public ALB as the AWS entry, and **do not** substitute Istio Ingress Gateway for API Gateway at R0 — `ADR-022`, `CR-012` §3.  
3. **Provision** Keycloak as a **private** IdP workload (behind the identity adapter); **never** expose Keycloak or Cognito to Flutter / NIP-APP — `ARCH-018`/`ARCH-019`, `ADR-020`.  
4. Treat **API Gateway and Keycloak as complementary layers**, not alternatives. Removing API Gateway because “we have Keycloak” is a category error (see §5).

**Recommended verdict for Infra Head:** Accept Option A (keep API Gateway + private Keycloak). Any Option B (Istio / public LB) requires a Board 1 + Board 4 amendment of `ADR-018` with a completed control map (§8), not a Terraform preference.

---

## 1. The layered picture (who owns which security job)

These components solve **different** problems on **different** planes. Infra must not collapse them into one product.

```text
NORTH–SOUTH (internet / PG → AWS → cluster)
  device / NIP-APP
       │  TLS 1.3 · one hostname
       ▼
  Cloudflare Enterprise (SaaS)     CDN, DDoS
       ▼
  F5-XC (SaaS WAF)                 OWASP / bot / L7 rate (bank perimeter)
       ▼
  Amazon API Gateway               ★ first AWS hop · validate · throttle · VPC Link
       ▼
  Internal ALB                     path route: /* → nip-web · /api/* → BFF
       ▼
  NIP BFF / workforce-access-bff   ★ session · token-hiding · first PEP
       ▼
  Domain services (private)        ★ re-check PDP on regulated actions

IDENTITY (private — never on the public path)
  BFF → identity-provider-adapter → Keycloak (partners / OIDC box)
                                 → Apigee private → bank AD-verify (workforce)
  BFF → identity-authorization-service (PDP — business authZ SoT)

EAST–WEST (pod ↔ pod) — R0 WITHOUT a mesh
  NetworkPolicy + IRSA + in-app timeouts/breakers
  (Istio / App Mesh = target-state / S14 — NOT R0)

OUTBOUND (loading dock) — not this POC’s inbound topic
  pod → inspection → Apigee → 1SB / SMS / EBS / AD-verify
```

| Layer | Product | AuthN? | AuthZ? | Business logic? | Public? |
|-------|---------|--------|--------|-----------------|---------|
| Bank SaaS edge | Cloudflare + F5-XC | No | No | No | Yes (SaaS) |
| AWS inbound proxy | **API Gateway** | No (API key ≠ user auth) | No | Validate / size / throttle | **Yes — only AWS public hop** |
| In-VPC reverse proxy | Internal ALB | No | No | Path routing | No |
| Edge app | BFF | **Opaque session** | Calls PDP | Aggregation only | No (private) |
| IdP | **Keycloak** (private) | Credentials / OIDC tokens | Not business SoT | No | **No** |
| Business authZ | `identity-authorization-service` | — | **PDP** | Policy | No |
| Mesh | Istio | Service mTLS (if admitted) | Traffic policy | No | N/A — **out of R0** |

Sources: `R0-LLD` §3 hop table; auth README §3–§4; `ADR-018`/`ADR-020`.

---

## 2. Why we selected Amazon API Gateway

### 2.1 Design drivers (not fashion)

| Driver | What API Gateway gives us | Why it matters for insurance R0 |
|--------|---------------------------|----------------------------------|
| Align to bank north–south pattern | First AWS hop after Cloudflare/F5-XC SaaS | Matches AU Bank application perimeter style; does not invent a spoke Public ALB pattern (`ADR-018`) |
| Keep EKS private | VPC Link → Internal ALB; workload VPCs have **no IGW** | Domain services, Keycloak, Aurora, Valkey, MSK never get a public listener (`R0-LLD` §2) |
| Abuse resistance **before** pods | Request validation, payload size limits, throttling | RM / partner / PG traffic can spike; refuse junk before Karpenter burns capacity |
| Separate money path | Dedicated **PG-callback** API Gateway route, IP-allowlisted (TB-6) | Customer payment never shares the RM session chain; callbacks are signature-verified in Payment |
| Operability / evidence | API Gateway access logs → operational search pipe (`ADR-013`) | Edge forensics without treating OpenSearch as regulatory evidence |
| Partner / IPR same front door | One inbound product for RM and partner surfaces | Difference is PDP decision + query scope — not a second public entry (`03-solution-architecture-r0`) |
| No business logic at the edge | Gateway is a governance proxy only | AuthN/AuthZ stay in BFF + PDP; avoids “smart edge” that duplicates Keycloak or PDP |

### 2.2 What API Gateway is **not**

- Not the IdP (that is Keycloak / bank AD-verify).  
- Not the session store (that is ElastiCache Valkey behind the BFF — `ADR-011`).  
- Not Apigee (Apigee is **outbound** only — `ADR-020`).  
- Not a substitute for F5-XC WAF (bank SaaS perimeter stays).  
- Not an authorisation PDP (business decisions stay in `identity-authorization-service`).

### 2.3 Human Architecture direction already on file

> We **want Amazon API Gateway** as the first AWS hop.  
> — `2026-09-14-HUMAN-DIRECTION-APIGEE-EGRESS-IDP.md` §1 / §2.2

`ADR-018` withdrew an earlier External ALB-in-front-of-Gateway hop. Reintroducing a public ALB *instead of* Gateway reverses that correction.

---

## 3. Why not Istio (as the inbound substitute)

### 3.1 Different plane

| | Amazon API Gateway | Istio |
|---|--------------------|-------|
| Plane | **North–south** (client → AWS) | **East–west** (pod ↔ pod), optionally Ingress Gateway |
| R0 status | **In BOM** | **Explicitly out** (`CR-012` §3; `R0-LLD` §1.3) |
| R0 substitute for mesh goals | — | `NetworkPolicy` + IRSA + Resilience4j + egress firewall (`ADR-010`) |
| Typical controls | Schema/size/throttle, VPC Link, managed public endpoint, access logs | Sidecar mTLS, retries, traffic shifting, (if used) cluster ingress |

**“Use Istio instead of API Gateway” mixes two layers.** Even a future mesh admit at S14 does **not** automatically remove the need for a governed AWS front door unless `ADR-018` is amended.

### 3.2 Why mesh was refused for R0 (`CR-012` §3)

- Envoy sidecar per pod for mTLS/retries that NetworkPolicy + IRSA + in-app breakers already cover at R0 scale (~100 journey starts/hour class of load in robustness analysis).  
- Operational surface: mesh control plane + upgrades + on-call while `GATE-S08` is still open (`RISK-014` class of concern).  
- `ADR-010` egress inspection is **not** a mesh and must not be sold as one.

### 3.3 If someone means “Istio Ingress Gateway only”

That is still **not** a free swap:

- Needs a **public** AWS load balancer (NLB/ALB) in front — reopens External ALB (forbidden by `ADR-018` without amendment).  
- Moves schema/throttle/callback allowlist into Envoy config owned by the app platform, not a managed API product.  
- Puts the first AWS TLS listener on a path that can more easily expose cluster ingress misconfiguration.  
- Requires Deepali acceptance of residual risk and a **new** ADR amending `ADR-018` (`CR-017` Option B).

---

## 4. Why not an External / public load balancer (as AWS entry)

| Claim | Response |
|-------|----------|
| “Other bank apps use a Public ALB” | That ALB is **that application’s** AWS entry. This platform’s AWS entry is **API Gateway** (`ADR-018`). Do not clone the Public VPC + Public ALB + peering pattern (`ADR-009` attach-as-spoke). |
| “We need an external LB if we drop API Gateway” | Correct — and that is why dropping Gateway is expensive: you **must** reintroduce a public AWS hop the programme already withdrew. |
| “Put External ALB in front of API Gateway” | Already considered and **withdrawn** (`ADR-018` / `ADR-016` amendment). Extra hop, extra cert/DNS, no security gain over Cloudflare → F5 → Gateway. |
| “Internal ALB is enough alone” | Internal ALB cannot be the internet endpoint without a public front (Gateway or public LB). Alone it only path-routes inside the VPC. |

**Standing constraint:** only API Gateway is public on the AWS side; no public NLB/ALB onto EKS; no public OpenSearch / broker (`R0-LLD` §1.3, §2).

---

## 5. Why we need Keycloak — and why that does **not** remove API Gateway

### 5.1 What Keycloak is for

Keycloak is the **initial private IdP** (`ARCH-018`):

| Responsibility | Keycloak / adapter | Not Keycloak |
|----------------|--------------------|--------------|
| Partner / IPR credentials & OIDC ceremonies | Yes (provider) | — |
| Workforce AD password store / LDAP bind from EKS | **Forbidden** (`ADR-020`) | Bank AD via **Apigee private AD-verify API** |
| Business roles, SP certification, insurer scope, grants/denials | No | **`identity-authorization-service` (PDP)** |
| UI chrome for login / user-role mapping | No — **NIP-APP / Fireframe** | Keycloak admin console not shown to bank users |
| Tokens on the device | **Never** — BFF token-hiding (`ARCH-019`) | Opaque session cookie / Keychain handle only |
| Replaceability | Behind `identity-provider-adapter-service` | Cognito (or other OIDC) can replace later without Flutter rewrite |

### 5.2 The category error: “We have Keycloak, so we don’t need API Gateway”

| Question | Keycloak answers | API Gateway answers |
|----------|------------------|---------------------|
| Who is this user? | Yes (after private ceremony via BFF/adapter) | No |
| May this principal sell / see this insurer? | No (PDP does) | No |
| Is this HTTP request well-formed / oversized / flooding us **before** it hits EKS? | No | **Yes** |
| How does the internet reach a **private** cluster without public pods? | No | **VPC Link** |
| How do PG callbacks enter on a separate allowlisted route? | No | **Yes (TB-6)** |
| Should Flutter ever call the IdP? | **No** | Gateway fronts the **BFF**, not Keycloak |

Security is **defence in depth**. Keycloak without a governed north–south hop means either:

- Keycloak (or the BFF) becomes internet-reachable → violates “IdP private / Flutter never talks to IdP”, or  
- You still invent another public front (public ALB / Istio Ingress) → you rebuilt API Gateway poorly.

### 5.3 How API Gateway and Keycloak work **together** on a login

```text
1. NIP-APP → (CF → F5 → API Gateway → Internal ALB) → BFF /login
2. BFF creates pending login (state, nonce, PKCE); calls identity-provider-adapter
3. Workforce: adapter/BFF path uses bank AD-verify via Apigee private (not LDAP)
   Partner: OIDC ceremony against private Keycloak (callback only to BFF)
4. BFF exchanges code, resolves business identity, checks PDP prerequisites
5. BFF stores provider tokens in Valkey session vault; returns opaque session only
6. Later API calls: same Gateway → BFF session → PDP on regulated actions
```

API Gateway never sees the AD password and never issues OAuth tokens to the device. Its job on this path is **reachability + abuse controls + private integration**. Keycloak’s job is **credential/OIDC ceremony for the provider plane** (partners; workforce via bank API). Removing either breaks a different control.

### 5.4 Security outcomes if you “just use Keycloak at the edge”

| Failure mode | Consequence |
|--------------|-------------|
| Expose Keycloak hostname publicly | Token theft / admin chrome / realm attack surface; violates `ARCH-018`/`ADR-020` |
| Point Flutter at Keycloak | Tokens on device; provider lock-in; breaks token-hiding BFF (`ARCH-019`) |
| Skip Gateway; public ALB → BFF only | Lose managed request validation / dedicated PG route / standing “only Gateway is public” constraint |
| Use Gateway Cognito authorizer as user auth | Wrong plane; conflicts with token-hiding BFF and private IdP; Cognito is not R0 IdP (`R0-LLD` §1.3) |

---

## 6. What happens if we replace AWS API Gateway with Istio (Option B)

Assume the infra proposal: **drop API Gateway**, put **public NLB/ALB → Istio Ingress Gateway → (optional Internal ALB) → BFF**, and optionally roll out sidecars.

### 6.1 Immediate architecture deltas

| Area | Today (Option A) | After swap (Option B) |
|------|------------------|------------------------|
| First AWS hop | API Gateway | Public LB |
| Cluster ingress | Internal ALB (AWS LB Controller) | Istio Ingress Gateway (+ possibly still Internal ALB) |
| Public surface | Managed API product | LB + Envoy ingress in/near cluster |
| Mesh data plane | None | Sidecars (if full mesh) or ingress-only |
| PG callbacks | Separate Gateway route + IP allowlist | Must be redesigned on new front door |
| Edge logs | API Gateway access logs | ALB/NLB + Envoy access logs |
| ADR set | `ADR-018` holds | **Must amend** `ADR-018` (+ likely `ADR-016`, touch `ADR-020` inbound clauses, overturn `CR-012` mesh refusal for at least ingress) |

### 6.2 Security / compliance impact (Board 4 / Board 6 lenses)

| Impact | Severity class (indicative) | Notes |
|--------|-----------------------------|-------|
| Trust boundary moves closer to EKS | G8 / Deepali `S1`–`S0` depending on design | Misconfigured Gateway vs misconfigured Ingress are different blast radii |
| Loss of managed pre-pod validation unless rebuilt | Abuse / availability | Must re-home schema, size, throttle |
| TB-6 payment callback path redesign | Money movement adjacent (G5 if behaviour changes) | IP allowlist + signature verify must remain equivalent |
| Evidence / access-log pipeline change | G10 if regulator-askable edge evidence moves | Update `ADR-013` ingest list; do not put evidence only in a volatile index |
| Operational maturity during foundation | SRE `O1`/`O2` | Mesh + public LB while GATE-S08 open increases incident surface (`RISK-014` family) |
| Keycloak exposure risk if “simplify” wrongly | `S0` if IdP becomes public | Option B must **still** keep Keycloak private and Flutter token-hiding |

**Architecture severity:** amending inbound without control mapping is `A0`/`A1` rework territory — not a soft preference.

### 6.3 What does **not** get fixed by Istio

- Does not replace Keycloak, PDP, or token-hiding BFF.  
- Does not replace Cloudflare / F5-XC.  
- Does not replace Apigee on outbound.  
- Does not remove the need for Internal path routing discipline and NetworkPolicy.

---

## 7. If we change the architecture — full care list (checklist)

Use this as the Infra Head gate before any Terraform apply that drops API Gateway.

### 7.1 Governance (must complete before BOM change)

- [ ] Written platform refusal of API Gateway (or written cost/standard reason)  
- [ ] `CR-017` Option B control-mapping table **fully filled** (see §8)  
- [ ] New ADR **amending** `ADR-018` (do not silently edit `R0-LLD`)  
- [ ] Human Board 1 (Mahesh) + Board 4 (Deepali) position recorded — agents do not sign  
- [ ] Board 7 (Shivanshi) landing-zone request rewritten  
- [ ] Kalpana informed of S09 edge re-cost / critical-path impact  
- [ ] SPIKE-001 / Apigee inbound questions closed if Apigee is proposed on the front door instead  

### 7.2 Network & exposure

- [ ] Design public LB (NLB vs ALB), subnets, SG, TLS certs (ACM)  
- [ ] Confirm workload VPCs still have **no IGW** where required; only intended public hop  
- [ ] Cloudflare / F5 origin re-point runbook (DR hop D10 class)  
- [ ] DNS / hostname contract unchanged for NIP-APP (one hostname)  
- [ ] Partner and RM still share one front door (or document deliberate split + PDP impact)  

### 7.3 Ingress & mesh

- [ ] Istio revision, ingress gateway HA across AZs, resource quotas  
- [ ] Sidecar inject policy (namespaces); PDB; upgrade strategy  
- [ ] Equivalent of request validation / max body / rate limits in Envoy  
- [ ] Decide: ingress-only vs full mesh; do not “accidentally” sidecar everything  
- [ ] Interaction with AWS LB Controller / Internal ALB — avoid double proxies without a reason  

### 7.4 Identity & application security (Keycloak stays)

- [ ] Keycloak remains **private**; no Flutter → Keycloak  
- [ ] BFF token-hiding + Valkey session vault unchanged (`ADR-011`)  
- [ ] OAuth redirect URIs still BFF-only  
- [ ] AD-verify still via Apigee private — no LDAP from EKS  
- [ ] PDP re-check on regulated actions unchanged  
- [ ] Confirm Gateway authorizers / JWT at edge are **not** introduced as a bypass of BFF session  

### 7.5 Payment & callbacks

- [ ] PG callback allowlist redesigned on new front door (TB-6 equivalent)  
- [ ] Signature verification in Payment service still mandatory  
- [ ] Outbound PG session-create remains Apigee path  

### 7.6 Observability, DR, cost

- [ ] Access-log sources updated for OpenSearch / Firehose (`ADR-013`)  
- [ ] Dashboards/alerts for ingress Gateway, public LB, Envoy  
- [ ] DR runbook: failover of public LB + ingress + DNS (not only API Gateway)  
- [ ] Cost envelope for mesh + public LB vs managed API Gateway (`RISK-012` / `NFR-OPEN-6` class)  
- [ ] On-call ownership: who pages for Istio control plane vs former API Gateway  

### 7.7 Testing / evidence before calling it done

- [ ] Abuse/throttle tests at the new edge  
- [ ] Negative tests: Keycloak not reachable from internet  
- [ ] PG callback allowlist tests  
- [ ] Login + token-hiding regression (no tokens in browser storage)  
- [ ] ArchUnit / contract tests still green; no new public Service type  

---

## 8. Control remapping table (Option B must fill every cell)

| # | Control today on API Gateway | Option A (keep) | Option B mechanism | Owner | Deepali outcome |
|---|------------------------------|-----------------|--------------------|-------|-----------------|
| 1 | First AWS HTTPS hop | Keep Gateway | Public NLB/ALB | Shivanshi + platform | Pending |
| 2 | Request schema / size validation | Keep | Envoy / WAF-only / other | Platform + Amit | Pending |
| 3 | Throttle / abuse before pods | Keep | Envoy + Valkey counters / other | Platform + Deepali | Pending |
| 4 | VPC Link / private EKS | Keep | Equivalent private design | Shivanshi | Pending |
| 5 | PG callback IP allowlist + separate route (TB-6) | Keep | New front-door route | Payments + Deepali | Pending |
| 6 | Edge access logs | Keep | LB + Envoy → Firehose/OS | Shivanshi | Pending |
| 7 | No public ALB (`ADR-018`) | Keep | Explicit ADR amend | Mahesh | Pending |
| 8 | Partner/IPR same front door | Keep | Documented | Mahesh + Rajal | Pending |
| 9 | Flutter never reaches IdP | Keep (Gateway→BFF) | Must still hold | Deepali + WS-2 | Pending |
| 10 | Keycloak private | Keep | Must still hold | Shivanshi + WS-2 | Pending |

**Empty Option B cells = not ready to change architecture.**

---

## 9. Comparison summary (decision aid for Infra Head)

| Option | Inbound | Mesh | Keycloak | Fits ADRs? | Infra complexity | Security posture |
|--------|---------|------|----------|------------|------------------|------------------|
| **A — Recommended** | CF→F5→**API Gateway**→Internal ALB | None (R0) | Private | Yes (`ADR-018/020/022`) | Lower | Proven layered |
| B — Istio substitute | CF→F5→**Public LB**→Istio Ingress→… | Ingress ± sidecars | Still private | **No** until ADR amend | Higher | Residual risk until mapped |
| C — Apigee on inbound | Undrawn until SPIKE-001 | None | Private | Not yet | Bank API platform | Deferred |
| D — Expose Keycloak / skip Gateway | Anti-pattern | — | Public or semi-public | **Reject** | False simplicity | `S0` class |

---

## 10. Recommendation

1. **Select Amazon API Gateway** as R0 inbound Proxy 1 for the reasons in §2.  
2. **Reject Istio and External ALB as Gateway substitutes** at R0 for the reasons in §3–§4; keep mesh as an S14 conversation.  
3. **Keep Keycloak** as the private IdP behind the adapter (§5); keep token-hiding BFF; keep PDP as business authZ SoT.  
4. **Do not trade Keycloak for API Gateway or vice versa** — they secure different hops.  
5. If infra cannot provision Gateway, escalate with §8 completed — then a **new** ADR, not a quiet BOM edit (`CR-017`).

**Owners**

| Decision | Owner |
|----------|-------|
| Structure / ADR amend | Mahesh (Board 1) |
| Security residual risk | Deepali (Board 4) |
| Landing zone / provision | Shivanshi + bank infra platform |
| Cost / schedule impact | Kalpana (R12) |
| Identity runtime (Keycloak/BFF/PDP) | WS-2 + Amit packaging |

---

## 11. References (cite these in the review)

| Doc | Why |
|-----|-----|
| `ADR-018`, `ADR-020`, `ADR-022` | Binding inbound / split plane / clarifying reaffirm |
| `CR-012` §3, `CR-017` | Mesh refused; escalation options |
| `R0-LLD.md` §1.1 #7–#9, §1.3, §3 | BOM and hop table for platform team |
| `03-solution-architecture-r0.md` deployment properties | Perimeter statement |
| `2026-09-14-HUMAN-DIRECTION-APIGEE-EGRESS-IDP.md` | Human direction: Gateway inbound, Apigee outbound, Keycloak private |
| `docs/platform/authentication-authorization/README.md` | Keycloak, BFF, PDP, trust boundaries |
| `2026-10-06-PLATFORM-TEAM-NOTE-API-GATEWAY-VS-ISTIO.md` | One-pager for the meeting |

---

## 12. Document control

| Field | Value |
|-------|-------|
| Suggestion | `SUG-20261006-apg` (recurrence 2026-10-08 — deepen Keycloak + infra POC) |
| Change request | `CR-017` |
| Status | AI-DRAFTED justification · human Infra Head / Architecture / Security review outstanding |
| Not in scope of this file | Terraform modules, Istio install manifests, manufacturing T4 signatures |
