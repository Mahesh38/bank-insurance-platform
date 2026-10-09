# Mahesh contemplation guide — Aligning R0 inbound to bank-standard External LB + Istio

**Audience:** Mahesh (Architecture owner — the only real human decision-maker for topology in this programme’s current operating model)  
**Status:** **SUPERSEDED as contemplation — decision taken 2026-10-09.** Binding outcome is [`ADR-023`](../platform/architecture-review/08-architecture-decision-log.md) (Accepted). This file remains as process evidence.  
**Date:** 2026-10-09  
**Standing:** AI-drafted decision-prep under [`CR-017`](../governance/change-requests/CR-017-inbound-api-gateway-vs-istio.md) / `SUG-20261006-apg`. Architecture owner subsequently accepted Option B; cascade follows ADR-023 + S1–S11 (`HA-02`).

**Context Mahesh is weighing**

- Bank estate primary pattern: **External ALB/NLB + Istio** for services.  
- Amazon API Gateway: **rarely/never used** in the bank; platform expertise is thin.  
- Repo “boards” (Deepali, Shivanshi, Rajal, …) are **AI personas** that assemble evidence; they are not separate human vetoes in practice. Mahesh owns Architecture structure and may change it — but should still **run the decision framework** so the change is evidence-grade and cascade-complete.  
- Current binding text still says API Gateway inbound (`ADR-018`). Contemplating a reverse is legitimate; **silent diagram edits are not**.

**Freshness:** `04-STAGE_GATES.md` past 14-day window (WARN) — disclosed.

---

## 1. What Mahesh should do *while only thinking* (no ADR yet)

Do **not** treat contemplation as acceptance. Recommended sequence:

| Step | Action | Why |
|------|--------|-----|
| 1 | Keep this file (or a dated note) as **CONTEMPLATING** | Separates “thinking” from “decided” |
| 2 | Name the **problem**, not the product | Problem = “R0 inbound must be operable by bank infra that standardises on LB+Istio and lacks API Gateway skill” — not “Istio is cooler” |
| 3 | Classify constraint | Bank standard ≈ **external estate mandate** (stronger than preference). Record as assumption or dependency (e.g. extend `DEP-20261006-igw`) |
| 4 | Run alternatives explicitly | A) Keep Gateway + buy/borrow Gateway skill · B) Adopt bank LB+Istio · C) Hybrid (Gateway only for PG-callback; LB+Istio for RM) · D) Defer mesh, public ALB only (usually worse) |
| 5 | Open a **SPIKE** (written answers) before rewriting HLD | Bank Istio edition/version; ingress pattern (NLB vs ALB); who operates mesh; F5→origin auth; whether STRICT mTLS is mandatory day one |
| 6 | Draft **new ADR** that **supersedes/amends `ADR-018`** (and softens `CR-012` mesh refusal for the admitted scope) — status `PROPOSED` until Mahesh marks `ACCEPTED` | Topology change requires ADR (`04` decision framework §8) |
| 7 | Only after ADR Accepted: update `R0-LLD`, solution architecture, diagrams, auth edge picture, ARB pack | Diagrams are renderings; ADRs own truth (`HA-02`) |
| 8 | Update landing-zone ask to platform team | They already prefer this; give them the **post-ADR** BOM, not a half-edited SVG |

### 1.1 Authority reality in *this* repo

| Seat | In practice here | What Mahesh still owes the file |
|------|------------------|--------------------------------|
| Mahesh / Board 1 | Real Architecture owner | Structure, ADRs, BOM, HLD/LLD |
| Deepali / Board 4 | AI persona | Still produce a **Security impact section** (exposure, mTLS, F5 lock-down, Keycloak stays private). Mahesh may accept residual risk **as Architecture owner**, but must not pretend a separate human Security officer signed unless one exists outside the repo |
| Shivanshi / Board 7 | AI persona | Still produce **ops impact** (mesh on-call, upgrades, DR). Platform team in the bank is the real ops counterpart |
| Shailja / Board 6 | AI persona | Flag G10/evidence-path moves (edge logs, callbacks). Escalate to a real bank compliance human only if regulatory evidence obligations change |
| Agents | Draft only | Never mark ADR `ACCEPTED` or stage `PASSED` for Mahesh |

**Rule of thumb:** AI boards = structured challenge and evidence. Mahesh = the signature that makes the standard change. Contemplation ≠ signature.

### 1.2 What *not* to do while contemplating

- Do not redraw `r0-lld.svg` / HLD to show public ALB “to explore.”  
- Do not delete API Gateway from `R0-LLD` BOM without an Accepted ADR.  
- Do not tell platform “we decided Istio” until step 6 is Accepted.  
- Do not expose Keycloak or put Flutter on OAuth against Keycloak as part of the swap.  
- Do not silently drop Apigee outbound (`ADR-020`) — that plane is independent.

---

## 2. If Mahesh decides to **go ahead** — decision shape

**Recommended ADR title (draft):**  
`ADR-0XX — North-south ingress aligns to bank standard: Cloudflare → F5-XC → External ALB/NLB → Istio Ingress; Amazon API Gateway withdrawn for RM/mobile front door`

**Problem statement (suggested):**  
Bank infra standard and skill centre on External LB + Istio; API Gateway is non-standard and expertise-scarce. Continuing Gateway creates delivery and operability risk for S09 even if technically coherent.

**Authority class:** `A3_JOINT_REVIEW` at minimum (Security + SRE evidence sections), Accepted by Mahesh as Architecture owner when ready. Treat bank standard as the driver; record residual risks Mahesh accepts.

**Reversibility:** LOW after hostnames, F5 origins, and partner/PG allowlists bind to the public LB.

**Revisit triggers:** Bank adopts API Gateway later; mesh ops cost exceeds envelope; STRICT mTLS blocks a mandatory flow (e.g. inspection of 1SB mTLS — already exempt on egress).

---

## 3. What must change **beyond** “add External LB + Istio”

External LB + Istio are the visible tip. The architecture cascade is larger.

### 3.1 Decisions to make *inside* the LB+Istio choice

| # | Decision | Options | Why it matters |
|---|----------|---------|----------------|
| D1 | **NLB vs ALB** (public) | NLB (TLS in Istio, source IP) · ALB (ACM at LB, L7 before Istio) | Cert ownership, HTTP/2, header controls |
| D2 | **Mesh scope** | Ingress-only · Full sidecars · Sidecars only in selected namespaces | Ops cost; `CR-012` overturn breadth |
| D3 | **PeerAuthentication** | STRICT day one · PERMISSIVE then STRICT | Breaks plain HTTP probes / legacy clients if rushed |
| D4 | **Keep Internal ALB?** | Remove (Istio does path split) · Keep as extra hop | Avoid double reverse-proxy without reason |
| D5 | **API Gateway residual?** | Remove entirely · Keep **only** PG-callback route (TB-6) | Hybrid can reduce money-path redesign |
| D6 | **F5 → origin auth** | SG to F5 egress CIDRs · shared secret header · mTLS | Bypass resistance once origin is a public LB |
| D7 | **Istio edition/operator** | Bank standard version / Ambient vs sidecar | Skill and support model |

### 3.2 Network & landing zone (not “just LB”)

| Change | Detail |
|--------|--------|
| Public subnets | Internet-facing LB needs public subnets (or bank EDGE pattern). Today’s “no public ALB” / “workload VPC no IGW” language must be rewritten carefully: **workload pods stay private**; **LB** may sit in public/DMZ subnets |
| Security groups | Restrict LB SG to F5 (and Cloudflare if applicable) egress CIDRs; document CIDR refresh owner |
| Remove VPC Link | API Gateway → Internal ALB path goes away for RM traffic |
| DNS / Cloudflare origin | Origin becomes public LB DNS name, not API Gateway |
| ACM / certs | Public cert on ALB or on Istio Gateway; private cert story for east–west |
| TGW / EDGE / FortiGate | Confirm spoke attachment still valid; public LB does not invent a second hub |

### 3.3 Edge & application contracts

| Change | Detail |
|--------|--------|
| Path routing | Move `GET /*` → nip-web and `/api/*` → BFF from Internal ALB (or Gateway) to **Istio VirtualService / Gateway** |
| Request validation / size limits | Re-home from API Gateway to Envoy filters or WAF-only + BFF validation |
| Throttle / abuse | Re-home to Envoy + existing Valkey per-principal counters (`ADR-011`) — do not rely only on F5 |
| PG callbacks (TB-6) | Redesign allowlisted route on public LB/Istio **or** keep a thin API Gateway only for callbacks (D5) |
| Partner/IPR front door | Keep **one** hostname / same ingress unless Product accepts a split |
| OpenAPI / BFF base URL docs | Update “first AWS hop” language everywhere consumers read it |

### 3.4 Identity — mostly **unchanged**, but must be protected

| Keep | Do not break while swapping edge |
|------|----------------------------------|
| Keycloak **private** | Never publish Keycloak via Istio Gateway |
| Token-hiding BFF | Flutter still never holds OAuth tokens |
| PDP as business authZ SoT | Do not replace PDP with Istio `AuthorizationPolicy` for insurance rules |
| AD-verify via Apigee private | Unrelated to inbound LB choice |
| Adapter-neutral IdP | Cognito later still possible |

Optional later: Istio `RequestAuthentication` for **service** JWT — not for Flutter session cookies. If used, design jointly so it does not bypass BFF.

### 3.5 Mesh / east–west (if full Istio)

| Change | Detail |
|--------|--------|
| Sidecar injection policy | Which namespaces (`edge`, `core-sales`, …) |
| mTLS STRICT | BFF → domain → Keycloak over mesh mTLS |
| Retries/timeouts | Align with Resilience4j — avoid double-retry |
| NetworkPolicy | Keep defence-in-depth or consciously thin it; do not claim mesh alone |
| Metrics/tracing | Mesh telemetry + existing AMP/X-Ray/ADOT story |
| Upgrades / canary | Istio revision strategy; who pages |

### 3.6 Observability, DR, cost, ARB

| Change | Detail |
|--------|--------|
| Logs | Replace API Gateway access logs with ALB/NLB + Envoy access logs in `ADR-013` ingest |
| Dashboards/alerts | Ingress Gateway, public LB 5xx, mesh control plane |
| DR (D10 class) | Failover runbook: re-point Cloudflare/F5 to DR region LB + Istio, not DR API Gateway |
| Cost envelope | Public LB LCU + Istio CPU/memory vs Gateway per-request — update `RISK-012` / S09 budget |
| ARB / HLD / LLD / SVGs | All north–south pictures; BOM #7/#8; DO-NOT-PROVISION list inverted for Gateway vs public ALB |
| Tech lifecycle pack | Add Istio; remove or demote API Gateway lifecycle row |
| Platform justification Q&A | Update “why no public ALB” answers |

### 3.7 What typically does **not** need to change for this swap

| Leave alone (unless a separate decision) | Why |
|------------------------------------------|-----|
| Apigee **outbound** (`ADR-020`) | Different plane |
| Aurora one-cluster / schemas (`ADR-008`) | Unrelated |
| MSK / outbox (`ADR-012`) | Unrelated |
| Valkey sessions (`ADR-011`) | Still required for token-hiding |
| Flutter never calling Apigee/Keycloak | Still required |
| 1SB adapter anti-corruption | Unrelated |
| Product journey scope (R0 assisted Life) | Unrelated |

---

## 4. Suggested work breakdown if Mahesh says “go”

```text
W0  SPIKE — bank Istio+LB standard written answers (D1–D7)
W1  ADR-0XX PROPOSED → Mahesh ACCEPTED (amends ADR-018; scopes mesh vs CR-012)
W2  R0-LLD BOM rewrite + 03-solution-architecture deployment properties
W3  Auth README edge diagram (Gateway → public LB+Istio); Keycloak stays private
W4  Diagrams / SVGs regenerate from code after LLD truth moves
W5  Landing-zone request to platform (matches bank standard)
W6  PG-callback design (Gateway residual or Istio route)
W7  Runbooks: origin lock-down, DR, mesh upgrade
W8  ARB pack / justification Q&A refresh
```

Agents may draft W1–W8 artefacts; **only Mahesh** marks the ADR Accepted and authorises diagram truth to flip.

---

## 5. Contemplation checklist (Mahesh personal)

Before flipping from “thinking” to “decided”:

- [ ] Problem written as bank-standard / skill constraint (not fashion)  
- [ ] Alternatives A–D scored (Gateway skill hire vs LB+Istio align vs hybrid callback)  
- [ ] SPIKE answers for D1–D7 exist in writing from bank platform  
- [ ] Residual risks listed (public LB exposure, mesh ops, TB-6 redesign) — Mahesh accepts or mitigates  
- [ ] Security/ops evidence sections drafted (even if AI personas)  
- [ ] Cascade list in §3 assigned owners/dates  
- [ ] ADR Accepted **before** any SVG shows public ALB as truth  

---

## 6. Relationship to prior pack

| Doc | Role now |
|-----|----------|
| One-pager / Infra POC / Rebuttal | Still valid as **Option A defence** and as challenge material while contemplating |
| This file | **Option B contemplation process** + full cascade if Mahesh adapts to bank standard |
| `CR-017` | Escalation vehicle; Option B becomes the likely path **if** Mahesh Accepts the amending ADR |
| `ADR-022` | Reaffirmation of Gateway — would be **superseded** by the new ADR, not quietly ignored |

---

## 7. Bottom line for Mahesh

1. **Thinking is fine** — record it; run the framework; spike bank standard details.  
2. **You can change the architecture** — you are the Architecture owner; AI boards do not block you in this operating model.  
3. **Changing means ADR + cascade**, not only “draw External LB and Istio.” Section 3 is the real work.  
4. **Identity plane stays** — private Keycloak, token-hiding BFF, PDP — unless you make a *second* deliberate decision to break them (do not).  
5. **Until ADR Accepted, BOM and diagrams still show API Gateway** so platform is not dual-messaged.

When Mahesh is ready to decide (not only contemplate), next artefact is a **PROPOSED amending ADR** + SPIKE answers — agents can draft those on request.
