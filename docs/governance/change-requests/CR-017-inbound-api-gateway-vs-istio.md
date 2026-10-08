# CR-017 — Inbound edge: keep Amazon API Gateway; Istio is not an R0 substitute

**Change request:** CR-017  
**Date raised:** 2026-10-06  
**Status:** **DRAFT / ESCALATED** — awaiting human Architecture (Mahesh) + Security (Deepali) + SRE (Shivanshi) position. Not ratified. Agents must not treat this as approval to change the BOM.  
**Change type:** `ARCH` (with `INFRA` and `SEC` consequences)  
**Runtime impact:** None until a human-accepted Option B/C is transcribed into ADRs and `R0-LLD`. This CR packages a **decision**, not a provision change.  
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
| C | Recommends **Option A — keep API Gateway** | Impersonate Deepali’s security outcome or T4 Architecture signature |
| D | Sketches Option B (Istio Ingress + public LB) only as a **conditional** path requiring a new ADR | Admit Istio into R0 by side effect |

Companion artefacts (same suggestion):

- One-pager: [`docs/architecture/2026-10-06-PLATFORM-TEAM-NOTE-API-GATEWAY-VS-ISTIO.md`](../../architecture/2026-10-06-PLATFORM-TEAM-NOTE-API-GATEWAY-VS-ISTIO.md)
- **Infra POC (detailed, Keycloak + change checklist):** [`docs/architecture/2026-10-08-INFRA-POC-API-GATEWAY-KEYCLOAK-ISTIO.md`](../../architecture/2026-10-08-INFRA-POC-API-GATEWAY-KEYCLOAK-ISTIO.md)
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

## 5. Control mapping (Option B must fill every row)

| Control today (API Gateway) | Option A | Option B owner + mechanism | Deepali outcome |
|-----------------------------|----------|----------------------------|-----------------|
| First AWS HTTPS hop | Keep | Public LB / other | Pending |
| Request validation / size limits | Keep | Envoy / WAF-only / other | Pending |
| Throttling / abuse resistance before pods | Keep | … | Pending |
| VPC Link / private EKS | Keep | … | Pending |
| PG callback IP allowlist + separate route (TB-6) | Keep | … | Pending |
| Edge access logs → operational search | Keep | … | Pending |
| No public ALB (`ADR-018`) | Keep | Explicitly overturned | Pending |
| Partner/IPR same front door | Keep | … | Pending |

Empty Option B cells = **not ready to amend ADRs**.

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
| `docs/platform/architecture-review/08-architecture-decision-log.md` (`ADR-022`) | Clarifying ADR draft (Option A) |
| `docs/governance/registers/SUGGESTION-REGISTER.md` (`SUG-20261006-apg`) | Triage record |
| `docs/governance/registers/DECISION-REGISTER.md` | ADR-022 index row |
| `docs/governance/registers/DEPENDENCY-REGISTER.md` (`DEP-20261006-igw`) | External: platform written commitment |
| `docs/architecture/README.md` | Navigation row |

---

## 9. Recommended human verdict (draft — not a signature)

```text
Board 1 (Mahesh): APPROVED_WITH_CONDITIONS on Option A + ADR-022;
                  REWORK on Option B until §5 table is complete.
Board 4 (Deepali): Must accept residual risk for any Option B row;
                   Option A preserves current TB posture.
Board 7 (Shivanshi): Landing-zone request continues to ask for API Gateway
                     + Internal ALB; no public ALB; no Istio in R0 BOM.
```

Silence is not approval. Agents do not mark this CR Accepted.

---

## 10. Unresolved owners / dates

| Item | Owner | Target |
|------|-------|--------|
| Platform written position (provision API Gateway **or** formal refuse + substitute) | Bank platform team · chased by Shivanshi | See `DEP-20261006-igw` |
| Architecture human position on Option A vs B | Mahesh | Same chase window |
| Security outcome on any Option B mapping | Deepali | Before any ADR amend |
| S09 edge cost if Option B | Kalpana + Shivanshi | After Option B shape exists |
