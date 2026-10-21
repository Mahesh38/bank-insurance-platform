# Platform team note — Inbound edge (historical Option A pack; superseded by ADR-023)

**Audience:** AWS / bank platform team · CTO landing-zone reviewers  
**Purpose:** One-screen for the **current** R0 inbound ask (`ADR-023`), plus collapsed Option A audit trail.  
**Standing:** **SUPERSEDED 2026-10-09** for inbound *product choice* by [`ADR-023`](../platform/architecture-review/08-architecture-decision-log.md) — the ask below is the Accepted path. If this note disagrees with an ADR, the ADR wins (`HA-02`).

**Canonical BOM now:** [`R0-LLD.md`](./R0-LLD.md) §1.1 #7–#9, §1.3, §3 · **security:** [`2026-10-09-ADR-023-SECURITY-CONTROL-MAP.md`](./2026-10-09-ADR-023-SECURITY-CONTROL-MAP.md) · **ops:** [`2026-10-09-ISTIO-OPS-ADDENDUM.md`](./2026-10-09-ISTIO-OPS-ADDENDUM.md)  
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

## 2. Where the five controls live now (`ADR-023` S1–S11)

| # | Control (must live *before* / around EKS) | Home under ADR-023 |
|---|-------------------------------------------|--------------------|
| 1 | **First AWS hop** after bank SaaS perimeter | Ingress NLB — SG = F5 (+CF) egress CIDRs only (S2) |
| 2 | **Request validation / size / throttle** | Envoy on Istio Gateway + F5 + Valkey per-principal (S9) |
| 3 | **Private cluster / no public domain listeners** | NLB → Istio Ingress only; pods/Keycloak/data private (S4–S5) |
| 4 | **Separate PG-callback route** (TB-6) | Istio `/callbacks/pg/*` + PG IP allowlist + signature (S8) |
| 5 | **Access logs** | NLB + Envoy → operational pipe (`ADR-013`, S10) |

AuthN/AuthZ and business logic stay in the BFF + PDP + domain services — neither NLB nor Istio is the IdP or a second BFF (S6–S7). Full map: [`2026-10-09-ADR-023-SECURITY-CONTROL-MAP.md`](./2026-10-09-ADR-023-SECURITY-CONTROL-MAP.md).

---

## 3. Istio’s R0 role (current)

| Plane | Product | R0 status |
|-------|---------|-----------|
| **North–south** (internet → AWS) | Ingress NLB + **Istio Ingress Gateway** | **In BOM** (`ADR-023`) |
| **East–west** (pod ↔ pod) | Istio sidecars → STRICT mTLS after dated cutover | **In BOM** (`ADR-023` S11); retain `NetworkPolicy` |
| **Outbound bank APIs** | Apigee | Unchanged (`ADR-020`) |
| **AWS App Mesh** | — | **Out** — do not run two meshes |

Amazon API Gateway and Internal ALB-as-path-router are **withdrawn**. Ops constraints: [`2026-10-09-ISTIO-OPS-ADDENDUM.md`](./2026-10-09-ISTIO-OPS-ADDENDUM.md).

---

## 4. What must not regress when provisioning

Dropping a former Gateway control without a home is a **trust-boundary failure** (G8) — not a Terraform preference.

| Job | Must remain true |
|-----|------------------|
| First AWS HTTPS hop | NLB (or bank-standard ALB) — never open SG to `0.0.0.0/0` |
| Size / throttle before overload | Envoy + F5 + Valkey (S9) |
| Private EKS / private Keycloak | No public NodePort/LB on domain or IdP Services (S4–S5) |
| PG callback isolation (TB-6) | Separate Istio route + IP allowlist + signature (S8) |
| Edge access logs | NLB + Envoy into `ADR-013` pipe (S10) |
| Mesh ≠ insurance PDP | AuthorizationPolicy must not replace PDP (S7) |

Binding checklist: S1–S11 in the security control map. Residual mesh/CIDR risk sits with Architecture owner.

---

## 5. Recommended meeting outcome (**current**)

1. **Confirm** Ingress NLB + Istio Ingress (+ sidecars) as R0 inbound (`ADR-023`); Amazon API Gateway **withdrawn**.  
2. **Confirm** S1–S11 are mandatory provision/verify criteria; F5 CIDR list + refresh cadence before first non-dev apply.  
3. **Confirm** Apigee stays **outbound only** (`ADR-020`); do not put Apigee on the RM/mobile front door without SPIKE-001 written answers.  
4. **Confirm** landing-zone / Terraform asks for NLB+Istio — not API Gateway, VPC Link, or Internal ALB-as-path-router.

**Owners to close:** Mahesh (structure) · bank platform / Shivanshi (NLB+Istio ops) · WS-2 (BFF/Keycloak invariants) · Kalpana (S09 edge cost delta).

**Detailed archive (Option A defence — do not provision from):**
[`2026-10-08-INFRA-POC-API-GATEWAY-KEYCLOAK-ISTIO.md`](./2026-10-08-INFRA-POC-API-GATEWAY-KEYCLOAK-ISTIO.md) ·
[`2026-10-08-REBUTTAL-ALB-ISTIO-VS-API-GATEWAY.md`](./2026-10-08-REBUTTAL-ALB-ISTIO-VS-API-GATEWAY.md)

**Refs:** `R0-LLD` · `03-solution-architecture-r0` · `2026-09-14-HUMAN-DIRECTION-APIGEE-EGRESS-IDP.md` · `CR-017` · `ADR-023`.

<details><summary>Historical Option A §§ (superseded 2026-10-09 — audit trail only)</summary>

### H2. Why API Gateway (the five controls it carried)

| # | Control | Why it had to live *before* EKS |
|---|---------|----------------------------------|
| 1 | **First AWS hop** after bank SaaS perimeter | Matches former bank north–south pattern; keeps workload VPCs private |
| 2 | **Request validation / size / throttle** | Rejects bad or abusive traffic before pods; Internal ALB only path-routed |
| 3 | **VPC Link** into Internal ALB | EKS and domain services never got a public listener |
| 4 | **Separate PG-callback route** (TB-6) | IP-allowlisted to AU Bank PG; not on the RM session path |
| 5 | **Access logs** | Indexed with ALB / firewall logs (`ADR-013`) |

### H3. Istio framed as a different layer (Option A position)

| | Amazon API Gateway | Istio (service mesh) |
|---|--------------------|----------------------|
| Plane | **North–south** | **East–west** |
| R0 status (then) | **In BOM** (`ADR-018`) | **Out of R0** (`CR-012` §3) — S14 conversation |
| R0 substitute (then) | — | `NetworkPolicy` + IRSA + in-app breakers + egress firewall (`ADR-010`) |

That framing was overturned for inbound + sidecars by `ADR-023` / `CR-017` Option B.

### H4. If API Gateway is refused — what had to be replaced

| Job then on API Gateway | Re-home required |
|--------------------------|------------------|
| Public HTTPS / first AWS ingress | Public NLB/ALB (or bank edge landing) |
| Schema / size / throttle before EKS | Istio/Envoy filters or accepted residual risk |
| VPC Link / private cluster | Equivalent private integration design |
| PG callback IP allowlist + separate route | Equivalent TB-6 control |
| Edge access logs | New log source into the operational pipe |

Path taken: `CR-017` Option B → `ADR-023` with S1–S11 filled.

### H5. Former recommended meeting outcome (Option A)

1. Confirm Amazon API Gateway + Internal ALB as R0 inbound Proxy 1 / Proxy 2 (`ADR-018`).  
2. Confirm Istio / App Mesh remains out of R0; revisit at S14.  
3. Confirm Apigee stays outbound (`ADR-020`).  
4. If platform cannot provision API Gateway: escalate with written refusal + substitute — do not invent a mesh-for-gateway swap in Terraform.

**Do not use H5 as today’s ask.** Use §5 above.

</details>
