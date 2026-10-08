# Justification / rebuttal — Why External ALB/NLB + Istio Ingress is **not** the superior choice for this R0 architecture

**Audience:** Infra Head · bank platform / infra team · Architecture (Mahesh) · Security (Deepali) · SRE (Shivanshi)  
**Purpose:** Written response to the claim that *“For your specific architecture, an External ALB/NLB paired with Istio Ingress Gateway inside EKS is the superior and cleaner choice over AWS API Gateway.”*  
**Date:** 2026-10-08  
**Standing:** AI-drafted under [`CR-017`](../governance/change-requests/CR-017-inbound-api-gateway-vs-istio.md) / [`ADR-022`](../platform/architecture-review/08-architecture-decision-log.md) / `SUG-20261006-apg`. Does **not** manufacture human sign-off. ADRs win on conflict (`HA-02`).

**Related pack**

| Doc | Role |
|-----|------|
| [`2026-10-08-INFRA-POC-API-GATEWAY-KEYCLOAK-ISTIO.md`](./2026-10-08-INFRA-POC-API-GATEWAY-KEYCLOAK-ISTIO.md) | Full Infra Head POC (Keycloak + checklist) |
| [`2026-10-06-PLATFORM-TEAM-NOTE-API-GATEWAY-VS-ISTIO.md`](./2026-10-06-PLATFORM-TEAM-NOTE-API-GATEWAY-VS-ISTIO.md) | One-pager |
| [`R0-LLD.md`](./R0-LLD.md) §1.1, §1.3, §3 | Binding BOM and hop table |
| `ADR-018` · `ADR-020` · `CR-012` §3 | Binding decisions |

**Freshness:** `04-STAGE_GATES.md` past 14-day artefact window (WARN) — disclosed; no stage edit in this change.

---

## 0. Verdict (one paragraph)

The ALB/NLB + Istio Ingress proposal is a **coherent alternative topology for a later mesh-first estate**, but it is **not** superior for *this* R0 architecture as ratified. It rests on three false premises: (1) that we already run an “internal mesh”, (2) that API Gateway is only valuable if it validates JWTs at the perimeter, and (3) that Cloudflare + F5 already make any AWS inbound governance hop “duplicate overhead.” In our design, Amazon API Gateway is the **deliberate first AWS hop** into a **private** estate (`ADR-018`); auth lives in the BFF by design (`ARCH-019`); Istio is **explicitly out of R0** (`CR-012` §3). Adopting the proposed flow **reintroduces a public ALB** that Architecture already withdrew, adds a mesh control plane during foundation, and forces a full trust-boundary remapping (`CR-017` Option B) — it does not remove hops; it **replaces** managed hops with self-operated ones.

**Recommended position for Infra Head:** Reject the swap for R0. Keep Cloudflare → F5-XC → **API Gateway** → VPC Link → **Internal ALB** → nip-web / BFF. Keep Keycloak private. Park Istio to S14 unless Boards 1+4 accept a written control map.

---

## 1. Where the proposal is **correct** (we agree)

These points align with our ADRs and must not be argued away:

| Proposal statement | Our position |
|--------------------|--------------|
| Flutter Web runs in the browser; API calls go browser → edge → BFF | Agree — `nip-web` serves assets; JSON is never cached at CDN for authenticated APIs (`R0-LLD` §3.1) |
| Keycloak is private; Flutter must **not** do browser OAuth directly against Keycloak | Agree — token-hiding BFF is mandatory (`ARCH-019`, auth README §5–§6) |
| BFF is the OAuth2 confidential client / session broker | Agree — provider tokens stay in Valkey session vault (`ADR-011`) |
| F5 → origin should be locked (SG allowlist and/or shared secret / mTLS) | Agree as a **control pattern** — applicable whether origin is API Gateway **or** a public ALB |
| Path split `/` → Flutter web · `/api/*` → BFF | Agree — already the Internal ALB (and Gateway) contract |

**Conclusion from agreement:** The proposal correctly understands the **identity** plane. That does **not** decide the **inbound AWS** plane. Keycloak/BFF correctness does not imply “drop API Gateway.”

---

## 2. False premises in the superiority claim

### 2.1 “Private EKS cluster that already runs its own internal mesh”

**False for R0.** Service mesh (Istio / App Mesh) was **deliberately not admitted** (`CR-012` §3; `R0-LLD` §1.3). East–west today is:

- Kubernetes `NetworkPolicy`
- IRSA (no static keys in pods)
- In-app timeouts / breakers (Resilience4j)
- Egress inspection (`ADR-010`) — which is **not** a mesh

The proposal therefore compares API Gateway to **Istio we do not have**, then concludes Gateway is redundant *because of* that mesh. That is circular. Admitting Istio is a separate, expensive decision (S14 / Option B), not a free property of “EKS + BFF + IAM.”

### 2.2 “API Gateway shines when validating JWTs… otherwise it is a dumb pass-through”

**Category error.** In *our* architecture, JWT/OIDC validation at the public perimeter is **intentionally refused**:

- Flutter never holds OAuth tokens (`ARCH-019`)
- Session is opaque; BFF stores provider tokens server-side
- Business authZ is the PDP (`identity-authorization-service`), re-checked on regulated actions
- API Gateway API keys are **not** used as user auth (`R0-LLD` §3 hop table)

API Gateway’s jobs here are **not** IdP jobs:

| Gateway control (inbound) | Why it is not “dumb” |
|---------------------------|----------------------|
| First **AWS** HTTPS endpoint after bank SaaS | Only public AWS hop; workload VPCs have **no IGW** |
| VPC Link into Internal ALB | Keeps EKS/Keycloak/Aurora private without a public Service |
| Request size / schema / throttle before pods | Abuse and overload protection **before** Karpenter spends nodes |
| Separate **PG-callback** route + IP allowlist (TB-6) | Money path isolated from RM session path |
| Access logs into operational pipe (`ADR-013`) | Edge forensics without putting evidence only in a mesh |

Calling a non-JWT edge “dumb” is measuring Gateway against a pattern we rejected (perimeter JWT for Flutter).

### 2.3 “Cloudflare + F5 already handle edge… so AWS edge is duplicate overhead”

**Partial truth, wrong conclusion.** Cloudflare + F5-XC are the **bank SaaS perimeter** (CDN/DDoS/WAF/bot). They are **not**:

- An AWS account boundary / VPC integration product  
- Our VPC Link / private integration construct  
- Our PG-callback route owned in our account  
- Our DR re-point target inside AWS (`R0-LLD` D10 class)

Every design still needs **some** first AWS hop. The proposal puts an **internet-facing ALB/NLB** there. We put **API Gateway**. That is a **substitution**, not hop elimination:

```text
Proposal hops:  CF → F5 → Public ALB/NLB → Istio Ingress → (pods)
Our hops:       CF → F5 → API Gateway → VPC Link → Internal ALB → pods
```

Hop count is similar; ownership and blast radius differ. Human Architecture direction already named Gateway as the AWS entry (`2026-09-14-HUMAN-DIRECTION…` §1).

### 2.4 Capacity / “cost at scale” as the deciding argument

R0 is **correctness- and evidence-constrained**, not throughput-constrained:

> ~100 journey starts/hour BAU, ~7/minute at Q4 peak (CAP-A*). Do not scale from CPU.  
> — `R0-LLD` capacity context; `CR-012` ~100/hour

API Gateway per-request pricing is a **legitimate revisit trigger at high volume**, not a reason to introduce a mesh + public ALB during S08/S09 foundation. Cost at scale belongs in a dated `RISK` / ADR revisit when CAP numbers change — not as R0 topology dogma.

### 2.5 gRPC / WebSockets / “first-class streaming”

R0 north–south contract is **HTTPS JSON** to the BFF (Flutter ↔ BFF). There is no R0 requirement for public gRPC or browser WebSockets through the edge. Using protocol breadth as a deciding factor imports **future** capabilities into a **present** decision (classic SF3 / over-engineering). If a named consumer later needs streaming at the edge, raise a CR with evidence — do not overturn `ADR-018` prophylactically.

---

## 3. Point-by-point response to the comparison table

| Dimension | Claim for Gateway | Claim for ALB+Istio | Our justification |
|-----------|-------------------|---------------------|-------------------|
| **Edge redundancy** | Duplicate of CF+F5 | Minimal — LB just passes TLS | CF+F5 ≠ AWS entry. Gateway is AWS governance + private integration, not a second WAF. Public ALB is also “another hop.” |
| **VPC integration** | VPC Link + private path = extra hop | Native LB Controller to pods | VPC Link is the **point**: no public pods, no IGW on workload VPCs. “Direct to pods/ip mode” from a **public** LB increases cluster exposure and contradicts `ADR-018` “no public ALB onto EKS.” |
| **BFF & auth fit** | Gateway wasted if no JWT at edge | Istio can validate tokens at mesh boundary | We **do not want** perimeter JWT for Flutter. Optional Istio `RequestAuthentication` on opaque-session traffic is a poor fit and risks double policy (mesh vs PDP). Auth stays BFF + PDP. |
| **Protocols** | Limited gRPC/WS | Native HTTP/2, gRPC, WS | Not an R0 inbound requirement. Do not decide edge product on unused protocols. |
| **Cost at scale** | Per-million expensive | Flat ALB + LCU + cluster CPU | Acknowledge for later CAP growth. R0 load does not justify mesh opex + public LB blast radius now. |

---

## 4. Response to the recommended traffic flow

### 4.1 Proposed flow (restated)

```text
Browser/Flutter → Cloudflare → F5 → Internet-facing ALB/NLB
  → Istio Ingress Gateway → Flutter pod | BFF → Keycloak (private) / microservices
  + Istio PeerAuthentication STRICT (full mesh mTLS)
```

### 4.2 Binding conflicts

| Element | Conflicts with | Severity |
|---------|----------------|----------|
| Internet-facing ALB/NLB as AWS entry | `ADR-018` (public ALB **withdrawn**; Gateway is AWS entry) | Blocks without ADR amend |
| Istio Ingress + STRICT mesh | `CR-012` §3 / `R0-LLD` §1.3 (mesh not R0) | Requires mesh admit CR |
| Path routing only at Istio | `R0-LLD` §3 (Internal ALB is Proxy 2; Gateway Proxy 1) | Redesign of two-proxy model |
| Assumption “already have mesh” | Current BOM | False premise |

### 4.3 What the proposal still gets right inside the cluster

Private Keycloak, BFF as confidential client, Flutter not calling IdP — **unchanged under Option A**. You do **not** need a public ALB to preserve those properties; they already hold behind API Gateway → Internal ALB.

### 4.4 “Restrict ALB to F5 egress CIDRs + secret header”

Good control — **and it is not unique to ALB**. Equivalent patterns:

- API Gateway resource policies / WAF association / private integration  
- Cloudflare/F5 origin authentication toward Gateway  

SG lock-down of a public ALB to F5 CIDRs mitigates drive-by internet hits; it does **not** restore the “no public ALB” standing constraint, and CIDR drift of SaaS egress pools is an operational hazard we must own either way.

### 4.5 ALB vs NLB nuance in the proposal

| Proposal preference | Our note |
|---------------------|----------|
| NLB for throughput / source IP / Istio TLS | Irrelevant at CAP-A; source IP for PG callbacks is already designed as **Gateway route allowlist** (TB-6), not “preserve client IP through mesh” |
| ALB for ACM then into Istio | We already use ACM on **API Gateway** (public) and internal certs on Internal ALB — without a public ALB |

---

## 5. Latency / “unnecessary hops” — honest accounting

| Hop | Option A (ours) | Option B (proposal) | Eliminated? |
|-----|-----------------|---------------------|-------------|
| Cloudflare | Yes | Yes | No |
| F5-XC | Yes | Yes | No |
| AWS public entry | API Gateway | Public ALB/NLB | **Replaced**, not removed |
| Into cluster | VPC Link → Internal ALB | Istio Ingress (LB target) | **Replaced** |
| Path to BFF/web | Internal ALB rules | Istio VirtualService | **Replaced** |
| Auth | BFF + PDP | BFF + PDP (± Istio authz) | Same core |
| Mesh sidecar | No | Yes (STRICT) | **Added** latency & CPU |

Expected R0 user-visible latency is dominated by **BFF → domain → (Apigee) → 1SB/bank**, not by one managed AWS proxy. Trading Gateway for Istio sidecars typically **adds** east–west overhead on every hop while claiming to save north–south latency — a poor trade at CAP-A.

---

## 6. Security & compliance impact if we accepted the proposal

| Change | Impact |
|--------|--------|
| Public LB in public subnets | Trust boundary moves onto a construct `ADR-018` withdrew (G8) |
| Istio admit in foundation | Control-plane/ops maturity risk while GATE-S08 still open |
| STRICT mTLS everywhere | Good *eventually*; premature without mesh admit and Deepali acceptance |
| Istio token validation “if needed” | Temptation to bypass or duplicate BFF/PDP — reject unless jointly designed |
| PG callback redesign | TB-6 must be re-homed; money-path regression risk |
| Log evidence path | ALB/Envoy logs replace Gateway logs — update `ADR-013` ingest; do not lose edge forensics |

Identity plane (Keycloak private, token-hiding) can remain correct — **and still** leave inbound posture worse relative to standing ADRs.

---

## 7. Corrected architectural comparison (decision table)

| Dimension | AWS API Gateway (our R0) | External ALB/NLB + Istio Ingress (proposal) | Winner for **this** R0 |
|-----------|--------------------------|-----------------------------------------------|-------------------------|
| Fits `ADR-018` / human direction | Yes | No (needs amend) | **Gateway** |
| Assumes mesh already present | No | Yes (false) | **Gateway** |
| Matches token-hiding BFF (no perimeter JWT) | Yes (by design) | Neutral / temptation to add mesh JWT | **Gateway** |
| Keeps workload VPCs without public ALB onto EKS | Yes | No | **Gateway** |
| PG-callback TB-6 already designed | Yes | Must redesign | **Gateway** |
| Ops surface during S08/S09 | Managed AWS + Internal ALB | + Istio control plane + public LB | **Gateway** |
| CAP-A cost | Modest | ALB+LCU + mesh CPU | Gateway adequate |
| Future high-QPS / streaming edge | Revisit trigger | Stronger | **Later** (not R0) |
| Private Keycloak + BFF OAuth | Supported | Supported | Tie (not decisive) |

---

## 8. What Infra should provision (unchanged ask)

```text
Cloudflare → F5-XC → Amazon API Gateway → VPC Link → Internal ALB
  → GET /*  nip-web
  → /api/*  NIP BFF / workforce-access-bff
Keycloak: private, behind identity-provider-adapter only
Istio / App Mesh: DO NOT PROVISION in R0
External / public ALB: DO NOT PROVISION
```

If platform cannot provision Gateway: follow `CR-017` Option B — filled control map + Board 1/4 — **not** silent adoption of this proposal.

---

## 9. Talking points for Infra Head (60 seconds)

1. **We agree** on private Keycloak and BFF-as-confidential-client — that is already our design.  
2. **We do not have an internal mesh** in R0; Istio was refused (`CR-012`). The comparison assumes a product we deliberately excluded.  
3. **API Gateway is not our IdP** and is not supposed to validate Flutter JWTs; it is the first AWS hop, private integration, throttle/validate, and PG-callback front door.  
4. **Public ALB + Istio replaces** that hop; it does not remove Cloudflare/F5, and it reopens a pattern Architecture withdrew (`ADR-018`).  
5. **Cost/streaming arguments** are future CAP triggers, not R0 topology drivers at ~100 journeys/hour.  
6. **Decision:** keep Gateway for R0; park Istio; any swap is a formal ADR amend with Deepali’s residual-risk acceptance.

---

## 10. Document control

| Field | Value |
|-------|-------|
| Suggestion | `SUG-20261006-apg` (recurrence — rebuttal to ALB+Istio superiority claim) |
| CR | `CR-017` |
| Status | AI-DRAFTED justification · human review outstanding |
| Does not | Approve Option B · edit stage state · provision IaC |
