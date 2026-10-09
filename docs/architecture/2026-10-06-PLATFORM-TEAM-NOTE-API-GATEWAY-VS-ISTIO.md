# Platform team note — Inbound edge (historical Option A pack; superseded by ADR-023)

**Audience:** AWS / bank platform team · CTO landing-zone reviewers  
**Purpose:** Historical one-screen for the former API Gateway ask.  
**Standing:** **SUPERSEDED 2026-10-09** for inbound product by [`ADR-023`](../platform/architecture-review/08-architecture-decision-log.md). Keep for audit trail of the Option A defence. If this note disagrees with an ADR, the ADR wins (`HA-02`).

**Canonical BOM now:** [`R0-LLD.md`](./R0-LLD.md) §1.1 #7–#9, §1.3, §3 · **security:** [`2026-10-09-ADR-023-SECURITY-CONTROL-MAP.md`](./2026-10-09-ADR-023-SECURITY-CONTROL-MAP.md)  
**Binding ADRs now:** `ADR-023` (ingress NLB+Istio) · `ADR-020` (outbound Apigee) · S1–S11 mandatory

---

## 1. Ask of the platform team (**current — ADR-023**)

Provision **exactly** this north–south path for R0:

```text
device → Cloudflare Enterprise (SaaS) → F5-XC (SaaS WAF)
      → Ingress NLB                 ← first AWS hop (SG = F5/CF CIDRs only)
      → Istio Ingress Gateway       ← path split (/* · /api/* · /callbacks/pg/*)
      → nip-web / NIP BFF
```

Separately: **Apigee is outbound only** — 1SB, SMS, AD-verify, EBS leave via Apigee (`ADR-020`). Flutter never calls Apigee or Keycloak.

**Do not provision:** Amazon API Gateway · VPC Link · Internal ALB-as-path-router · AWS App Mesh · Kong / Nginx Plus · in-VPC F5 BIG-IP · public Keycloak routes · NLB SG 0.0.0.0/0.

<details><summary>Historical Option A ask (superseded)</summary>

```text
device → Cloudflare → F5-XC → Amazon API Gateway → VPC Link → Internal ALB → nip-web / NIP BFF
```

</details>

---

## 2. Why API Gateway (the five controls it carries)

| # | Control | Why it must live *before* EKS |
|---|---------|--------------------------------|
| 1 | **First AWS hop** after bank SaaS perimeter | Matches bank north–south pattern; keeps workload VPCs private (no IGW on app VPCs) |
| 2 | **Request validation / size / throttle** | Rejects bad or abusive traffic before pods; Internal ALB only path-routes |
| 3 | **VPC Link** into Internal ALB | EKS and domain services never get a public listener |
| 4 | **Separate PG-callback route** (TB-6) | IP-allowlisted to AU Bank PG; not on the RM session path |
| 5 | **Access logs** | Indexed with ALB / firewall logs (`ADR-013`); edge evidence for ops and review |

AuthN/AuthZ and business logic stay in the BFF + PDP + domain services — API Gateway is **not** the IdP and **not** a second BFF.

---

## 3. Istio is a different layer

| | Amazon API Gateway | Istio (service mesh) |
|---|--------------------|----------------------|
| Plane | **North–south** (internet → AWS) | **East–west** (pod ↔ pod) |
| R0 status | **In BOM** (`ADR-018`) | **Out of R0** (`CR-012` §3; `R0-LLD` §1.3) — S14 conversation |
| R0 substitute | — | `NetworkPolicy` + IRSA + in-app breakers + egress firewall (`ADR-010`) |

**“Use Istio instead of API Gateway” conflates two problems.** Even if a mesh is admitted later, inbound still needs a governed AWS front door unless `ADR-018` is formally amended.

---

## 4. If API Gateway is refused — what must be replaced

Dropping API Gateway is a **trust-boundary change** (review trigger G8). It is not a Terraform preference.

| Job today on API Gateway | Must re-home (written, with owner) |
|--------------------------|-------------------------------------|
| Public HTTPS / first AWS ingress | Public NLB/ALB (or bank edge landing) — **reopens** the External ALB pattern `ADR-018` withdrew |
| Schema / size / throttle before EKS | Istio/Envoy filters, another API product, or accepted residual risk (Deepali) |
| VPC Link / private cluster | Equivalent private integration design |
| PG callback IP allowlist + separate route | Equivalent TB-6 control |
| Edge access logs | New log source into the operational pipe |

**External load balancer:** yes — without API Gateway you almost certainly need a **public** AWS LB in front of Istio Ingress Gateway / Internal ALB. That is the opposite of the current rule *“only API Gateway is public.”*

A control-mapping table with Deepali acceptance is mandatory before any alternative is drawn on HLD/LLD. Draft path: `CR-017` Option B → new ADR amending `ADR-018` (not a silent BOM edit).

---

## 5. Recommended meeting outcome

1. **Confirm** Amazon API Gateway + Internal ALB as R0 inbound Proxy 1 / Proxy 2 (`ADR-018`).  
2. **Confirm** Istio / App Mesh remains **out of R0**; revisit at S14 with a separate CR.  
3. **Confirm** Apigee stays outbound (`ADR-020`); do not put Apigee on the RM/mobile front door without SPIKE-001 written answers.  
4. If platform cannot provision API Gateway: escalate with a **written refusal + proposed substitute** against the table in §4 — do not invent a mesh-for-gateway swap in Terraform.

**Owners to close:** Mahesh (structure) · Deepali (security outcome) · Shivanshi (landing-zone request) · bank platform team (provisioning commitment).

**Detailed Infra Head POC (Keycloak + full change checklist):**
[`2026-10-08-INFRA-POC-API-GATEWAY-KEYCLOAK-ISTIO.md`](./2026-10-08-INFRA-POC-API-GATEWAY-KEYCLOAK-ISTIO.md)

**Refs:** `R0-LLD` · `03-solution-architecture-r0` deployment properties · `2026-09-14-HUMAN-DIRECTION-APIGEE-EGRESS-IDP.md` · `CR-017` · `ADR-022`.
