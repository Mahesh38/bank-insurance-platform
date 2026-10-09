# CR-017 — Inbound edge: bank-standard NLB + Istio (Option B accepted)

**Change request:** CR-017  
**Date raised:** 2026-10-06  
**Status:** **ACCEPTED (Architecture owner)** — Mahesh accepted Option B 2026-10-09. Binding transcription is [`ADR-023`](../../platform/architecture-review/08-architecture-decision-log.md) + [`R0-LLD`](../../architecture/R0-LLD.md) cascade. Security controls S1–S11 are mandatory; residual mesh/CIDR risk recorded under Architecture owner (no separate human Security officer in-repo). Agents **do** update BOM/diagrams to match ADR-023; agents still **do not** edit stage state or manufacture T4 human signatures.  
**Change type:** `ARCH` (with `INFRA` and `SEC` consequences)  
**Runtime impact:** None until S09 Terraform apply — design/BOM now binds to NLB + Istio.  
**Origin:** `SUG-20261006-apg` — human:stakeholder (platform-team challenge: may not use AWS API Gateway; Istio / External LB offered as alternative)  
**Workstream:** WS-3 — AU Bank Insurance Distribution Platform  
**Stage:** S08 — Engineering Foundation · S09 overlapped (Platform & Environment Foundation)

---

## 1. Request

Obtain a **written, board-owned** answer to the platform team’s challenge that Amazon API Gateway will not be used for inbound, and that Istio (and/or an external load balancer) should cover the edge instead.

This CR:

| # | Does | Does not |
|---|------|----------|
| A | Records the challenge and the binding baseline (`ADR-018`, `ADR-020`, `CR-012` §3) | Change stage state or mark any ADR Accepted |
| B | Drafts talking points and a control-mapping table for the platform meeting | Provision Terraform or amend live BOM without human sign-off |
| C | Originally recommended Option A; **Architecture owner chose Option B** | Impersonate a separate Deepali human signature beyond Architecture-owner residual-risk acceptance |
| D | Option B (Istio Ingress + public LB) admitted via **ADR-023** with S1–S11 | Drop security controls when swapping the product |

Companion artefacts (same suggestion):

- One-pager: [`docs/architecture/2026-10-06-PLATFORM-TEAM-NOTE-API-GATEWAY-VS-ISTIO.md`](../../architecture/2026-10-06-PLATFORM-TEAM-NOTE-API-GATEWAY-VS-ISTIO.md)
- **Infra POC (detailed, Keycloak + change checklist):** [`docs/architecture/2026-10-08-INFRA-POC-API-GATEWAY-KEYCLOAK-ISTIO.md`](../../architecture/2026-10-08-INFRA-POC-API-GATEWAY-KEYCLOAK-ISTIO.md)
- **Rebuttal to ALB+Istio superiority claim:** [`docs/architecture/2026-10-08-REBUTTAL-ALB-ISTIO-VS-API-GATEWAY.md`](../../architecture/2026-10-08-REBUTTAL-ALB-ISTIO-VS-API-GATEWAY.md)
- **Mahesh contemplation (bank-standard ELB+Istio — not ADR yet):** [`docs/architecture/2026-10-09-MAHESH-CONTEMPLATION-BANK-STANDARD-ELB-ISTIO.md`](../../architecture/2026-10-09-MAHESH-CONTEMPLATION-BANK-STANDARD-ELB-ISTIO.md)
- Clarifying ADR draft: `ADR-022` in [`08-architecture-decision-log.md`](../../platform/architecture-review/08-architecture-decision-log.md)

---

## 2. Problem being solved

Platform / landing-zone conversations are mixing **three** different products:

1. **Amazon API Gateway** — R0 **inbound** Proxy 1 (`ADR-018`, human direction 2026-09-14).  
2. **Apigee** — R0 **outbound** bank API plane (`ADR-020`).  
3. **Istio / App Mesh** — **target-state** east–west mesh; **explicitly refused for R0** (`CR-012` §3; `R0-LLD` §1.3).

If the platform team refuses API Gateway without a control-for-control substitute, S09 edge provisioning (P4 band in `R0-LLD`) cannot proceed without either:

- violating standing ADRs, or  
- inventing a public ALB / Istio Ingress design that reopens trust boundaries Deepali has not accepted.

The programme needs a single escalation package the stakeholder can take into that meeting — not an ad-hoc BOM edit.

---

## 3. Current binding baseline (do not silently overturn)

```text
Inbound:  device → Cloudflare (SaaS) → F5-XC (SaaS) → Amazon API Gateway → VPC Link → Internal ALB → BFF
Outbound: pod → (inspection) → Apigee → 1SB / bank APIs
Mesh:     NOT R0 — NetworkPolicy + IRSA + Resilience4j; mesh = S14
Public ALB: WITHDRAWN (ADR-018)
```

Evidence: `ADR-018`, `ADR-020`, `R0-LLD` §3, `03-solution-architecture-r0` deployment properties, `2026-09-14-HUMAN-DIRECTION-APIGEE-EGRESS-IDP.md` §2.2, `CR-012` §3.

---

## 4. Options

### Option A — Keep Amazon API Gateway (recommended)

- Platform provisions API Gateway + VPC Link + Internal ALB per `R0-LLD` BOM #8–#9.  
- Istio stays out of R0.  
- `ADR-022` records the clarification that mesh ≠ inbound gateway.  
- **Severity if ignored:** S09 edge blocked or non-compliant with ADRs (`A1` structural).

### Option B — Replace API Gateway with public LB + Istio Ingress Gateway

Only if **all** of the following are written and accepted:

1. Control mapping (table in §5) complete with Deepali acceptance of residual risk.  
2. New ADR **amending** `ADR-018` (and touching `ADR-016` / `ADR-020` inbound clauses as needed).  
3. Explicit admit of Istio (or Ingress-only subset) into R0 — overturns `CR-012` §3 mesh refusal for at least the ingress plane; east–west sidecars remain a separate decision.  
4. PG-callback TB-6 redesigned on the new front door.  
5. Shivanshi updates the landing-zone request; Kalpana re-prices S09 edge.

Sketch (not approved):

```text
device → Cloudflare → F5-XC → Public NLB/ALB → Istio Ingress Gateway → (optional Internal ALB) → BFF
```

**Cost / risk:** Reintroduces public AWS LB; expands EKS attack surface; operational mesh maturity during GATE-S08; log/evidence path change; likely T4 Architecture + Security.

### Option C — Apigee on inbound as well as outbound

Deferred until `SPIKE-001` remaining written answers exist (edition, private path, product onboard). Human direction 2026-09-14: **do not redraw ingress** onto Apigee until those answers land. Not a silent substitute for API Gateway.

### Option D — Do nothing / informal BOM edit

**Reject.** Silence or Terraform drift against ADRs is SF4 relative to standing constraints.

---

## 5. Control mapping (Option B — filled; binds ADR-023 S1–S11)

| Control formerly on API Gateway / identity ADRs | Option B home | Owner | Outcome |
|-----------------------------|---------------|-------|---------|
| First AWS HTTPS hop | Ingress NLB (or bank ALB) | Shivanshi / platform | Accepted |
| Request size / schema limits | Envoy limits on Istio Gateway (S9) | Shivanshi + Amit | Accepted |
| Throttling / abuse before pods | F5 + Envoy + Valkey per-principal (S9) | Deepali design / platform ops | Accepted |
| Private EKS / no public domain Services | Private-app pods; private EKS API (S4) | Shivanshi | Accepted |
| PG callback IP allowlist + separate route (TB-6) | Istio `/callbacks/pg/*` + signature (S8) | Deepali + Payments | Accepted |
| Edge access logs → operational search | NLB + Envoy → ADR-013 (S10) | Shivanshi | Accepted |
| No open public LB (`ADR-018` intent) | NLB SG = F5/CF CIDRs only + F5 secret/mTLS (S2–S3) | Deepali design / platform | Accepted (compensating) |
| Partner/IPR same front door | Same NLB + Istio + BFF | Mahesh | Accepted |
| Token-hiding / private Keycloak / PDP | Unchanged (S5–S7) | WS-2 | Accepted |

Companion: [`2026-10-09-ADR-023-SECURITY-CONTROL-MAP.md`](../../architecture/2026-10-09-ADR-023-SECURITY-CONTROL-MAP.md).

---

## 6. Scope-fit and stage-fit

| Lens | Code | Rationale |
|------|------|-----------|
| Stage fit | **SF1** | S09 platform foundation / edge is on-stage (overlapped). Clarifying the inbound product is prerequisite to a correct landing-zone request. Implementing Option B mesh breadth would be SF3 relative to `CR-012` unless boards overturn. |
| Scope fit | **SC0** | WS-3 in_scope includes S09 platform foundation; perimeter is already in `R0-LLD`. |
| Necessity | **MUST** (decision) / **NOT-NOW** (Option B implement) | A written platform + Architecture + Security position is MUST before S09 edge apply. Building Istio now is NOT-NOW without that position. |
| Risk tier if Option B admitted | **T4** | G8 production topology / public exposure; G1/G10 may also fire depending on authz and regulator-evidenced edge controls. |
| Priority | **P2 now · P1 at S09 edge provision** | Blocks correct P4 edge deliverable if unresolved; not a soft-P1 override until platform issues a hard refusal on the critical path. |

---

## 7. What this CR deliberately does not change

- `ADR-020` outbound Apigee plane.  
- Cloudflare / F5-XC SaaS perimeter.  
- R0 refusal of per-service Aurora, analytics warehouse, Cognito-as-IdP.  
- Application code, BFF contracts, or Flutter origins.  
- Stage fields in `CURRENT-STATE.yaml`.

---

## 8. Files in this escalation package

| File | Role |
|------|------|
| `docs/architecture/2026-10-06-PLATFORM-TEAM-NOTE-API-GATEWAY-VS-ISTIO.md` | One-pager for the meeting |
| `docs/architecture/2026-10-08-INFRA-POC-API-GATEWAY-KEYCLOAK-ISTIO.md` | Detailed Infra Head POC: Gateway vs Istio/ELB, Keycloak complementarity, Option B checklist |
| `docs/architecture/2026-10-08-REBUTTAL-ALB-ISTIO-VS-API-GATEWAY.md` | Point-by-point rebuttal of “ALB/NLB+Istio superior to API Gateway” claim |
| `docs/architecture/2026-10-09-MAHESH-CONTEMPLATION-BANK-STANDARD-ELB-ISTIO.md` | If Mahesh adapts to bank ELB+Istio: process, SPIKE, ADR, full cascade beyond LB+mesh |
| `docs/platform/architecture-review/08-architecture-decision-log.md` (`ADR-022`) | Clarifying ADR draft (Option A) — superseded if Mahesh Accepts amending ADR |
| `docs/governance/registers/SUGGESTION-REGISTER.md` (`SUG-20261006-apg`) | Triage record |
| `docs/governance/registers/DECISION-REGISTER.md` | ADR-022 index row |
| `docs/governance/registers/DEPENDENCY-REGISTER.md` (`DEP-20261006-igw`) | External: platform written commitment |
| `docs/architecture/README.md` | Navigation row |

---

## 9. Verdict (Architecture owner — 2026-10-09)

```text
Board 1 (Mahesh): ACCEPTED Option B → ADR-023 (NLB + Istio; API Gateway withdrawn).
                  Conditions = mandatory S1–S11; Apigee outbound unchanged.
Board 4 (Deepali): Compensating controls drafted as S1–S11; residual mesh/CIDR risk
                   recorded under Architecture owner (no separate human Security
                   officer in-repo for a second signature).
Board 7 (Shivanshi): Landing-zone request asks for NLB + Istio, not API Gateway;
                     operate F5 CIDR refresh + mesh STRICT cutover.
```

Binding ADR: `ADR-023`. Superseded clarifying draft: `ADR-022`. Superseded inbound hop: `ADR-018`.

---

## 10. Remaining owners / dates

| Item | Owner | Target |
|------|-------|--------|
| Platform implement NLB + Istio per ADR-023 | Bank platform · Shivanshi | S09 P4 (`DEP-20261006-igw` rebased) |
| F5 egress CIDR list + refresh cadence (S2) | Bank platform + Deepali design | Before first non-dev apply |
| PeerAuthentication PERMISSIVE→STRICT cutover date | Shivanshi + Amit | Dated cutover in runbook |
| S09 edge cost delta vs API Gateway | Kalpana + Shivanshi | Cost envelope refresh |
