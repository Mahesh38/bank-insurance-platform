# Dependency Register

Edges of the dependency graph. Edges outlive the items that discovered them, so they are
recorded once and reused by every later ordering computation.

**Owner:** Tech Lead · Architect (architectural and decision edges)
**Model:** [07-DEPENDENCY_MODEL.md](../07-DEPENDENCY_MODEL.md)
**Follow-up dates:** Delivery Lead (R12) — she sets and publishes the date and names the owner; she
never supplies the answer (Rule PA-1)

> **2026-09-30 — Tech Lead review (REVIEW AND TOUCH).** Freshness limit 14d was exceeded (16d).
> Every row below was re-read against the 2026-09-30 AIGEM daily sign-off. **Nothing was closed,
> resolved, or assumed.** External dependencies DEP-002, DEP-010, DEP-20260824-dx1,
> DEP-20260824-eip and DEP-20260824-cst remain OPEN. AIGEM suggested ESCALATE (or RE-DATE for
> `cst`); those are owner actions, not agent actions, and no new required-by date has been
> invented. Full note: [`signoff/2026-09-30/DEPENDENCY-REGISTER-REVIEW.md`](../signoff/2026-09-30/DEPENDENCY-REGISTER-REVIEW.md).

> **2026-09-11 — R12 ageing sweep.** Every follow-up date in this register had passed: the four
> external rows by 14–21 days, and the cost-envelope decision had no date at all. A follow-up date
> in the past is not a tracked dependency, it is a hope (Rule DEP-3), and four hopes had been
> sitting on the critical path for three weeks. Each row below is re-dated against its named owner.
> **Nothing was decided, resolved or assumed by this sweep** — an overdue external dependency is
> still external, and re-dating it makes the chase schedulable rather than pretending the answer
> is available.

---

## 1. Edges

| ID | From | Relation | To | Type | State | Notes |
|----|------|----------|----|------|-------|-------|
| DEP-001 | Gate 4.3 (bank consumer UAT) | `blocked_by` | Gate 4.2 (OpenAPI published) | HARD | OPEN | A consumer cannot integrate against an unpublished contract |
| DEP-002 | Gate 4.3 | `external` | Bank app team UAT slot | EXTERNAL | OPEN | Owner Rajal / Product; required by 2026-09-18 (re-dated 2026-09-11, was 2026-08-21) |
| DEP-003 | Gate 4.6 (performance smoke) | `blocked_by` | Gate 4.1 (sandbox E2E in CI) | SOFT | OPEN | Smoke reuses the E2E harness; could be built standalone at higher cost |
| DEP-004 | TD-014 (integration ↔ persistence E2E) | `enables` | Gate 4.1 | TECHNICAL | OPEN | Parked item whose trigger has fired |
| DEP-005 | Phase 5 (Expand LOBs) | `blocked_by` | Phase 4 gate | HARD | OPEN | "Do not start Health/Motor until Phase 3/4 exit is met" |
| DEP-006 | TD-010 (Redis idempotency) | `blocked_by` | Horizontal scale-out decision | DECISION | OPEN | Needs an ADR before implementation, not just capacity |
| DEP-007 | TD-006 (AWS Secrets Manager) | `blocked_by` | AWS deployment target confirmed | ENVIRONMENT | OPEN | Prod profile fails fast until then |
| DEP-008 | Gate 4.4 (compliance review) | `enables` | TD-023 scope (raw payload capture breadth) | COMPLIANCE | OPEN | The review decides how far capture must extend |
| DEP-009 | WS-2 Phase 2 (production IdP) | `blocked_by` | WS-2 Phase 1 gate | HARD | OPEN | Deliberate deferral behind the adapter |
| DEP-010 | WS-2 Phase 2 (AD federation) | `external` | Bank confirms AD technology | EXTERNAL | OPEN | Owner Mahesh / Architecture; required by 2026-09-18 (re-dated 2026-09-11, was 2026-08-21) |
| DEP-011 | TD-007 (tighten ArchUnit) | `requires` | Packages populated by LOB expansion | TECHNICAL | OPEN | Cannot tighten rules against empty packages |
| DEP-20260824-dx1 | R0 bank connectivity (`ADR-009`) | `external` | Bank terminates the VPN, publishes prefixes, opens its firewall, accepts the DX order | EXTERNAL | OPEN | Owner Shivanshi / SRE with the bank network team; required by 2026-09-18 (re-dated 2026-09-11, was 2026-08-28). The **pattern** is decided; the bank's own work is not. `uat` cannot leave stubs behind until the VPN half exists |
| DEP-20260824-eip | 1SB and AU Bank PG allowlists | `blocked_by` | Publication of the **correct** egress IPs — human 2026-09-14: likely **Apigee** IPs (`ASM-015`), **not** inspection-VPC NAT EIPs (`ADR-010`) until written | EXTERNAL | OPEN | Owner Shivanshi + bank API platform; required by 2026-09-18 (re-dated 2026-09-11). **Do not** send 1SB a spoke NAT list while `ASM-015` is the working belief. A stale allowlist is indistinguishable from none |
| DEP-20260914-apg | `1sb-integration-service` outbound + internal AD-verify | `external` | Apigee product onboarded: private spoke path, per-1SB-path proxies, per-env egress IPs, internal targets that do not hairpin Cloudflare/F5 | EXTERNAL | OPEN | Owner Shivanshi + bank API platform. `ADR-020` draws the hop; this dependency is the written product + IP list. Java may use a configurable adapter base URL before those answers land |
| DEP-20260824-cst | `GATE-S09` entry (cloud account structure and budget approved) | `blocked_by` | Cost envelope for the five 2026-08-24 layers (`RISK-012`, `NFR-OPEN-6`) | DECISION | OPEN | Owner Shivanshi + Kalpana; required by 2026-09-25 (first date set 2026-09-11). Three stateful services, a sixth account, an inspection VPC per environment and two circuits are now inside the S09 budget line, and none of it is priced |
| DEP-20260824-evd | `#16` Audit consumer (W3) | `requires` | MSK topics, per-topic IAM and the Glue Schema Registry (`ADR-012`) | TECHNICAL | OPEN | Owner Amit + Shivanshi. Writing the audit path against a direct outbox poll and moving it later is a rewrite of the one component that must not lose a record |
| DEP-20261006-igw | S09 edge / inbound Proxy 1 (`ADR-018`, `ADR-022`, `CR-017`) | `external` | Bank platform team **written** commitment: provision Amazon API Gateway + VPC Link + Internal ALB **or** formal refuse with CR-017 §5 control mapping for a Board-accepted substitute | EXTERNAL | OPEN | Owner Shivanshi + bank platform team; Architecture (Mahesh) + Security (Deepali) on any substitute. Distinct from `DEP-20260914-apg` (Apigee **outbound**). Required by 2026-10-20 (initial chase date set 2026-10-06) |

## 2. External dependencies

Every `EXTERNAL` edge needs an owner and a follow-up date, or it is not tracked — it is hoped
for (Rule DEP-3).

| ID | Dependency | Owner | Required by | Age | State | Impact if late |
|----|------------|-------|-------------|-----|-------|----------------|
| → [DEP-002](#1-edges) | Bank app team UAT integration slot | Rajal / Product | **2026-09-18** | **OVERDUE — reviewed 2026-09-30, still OPEN.** AIGEM suggested ESCALATE. No new date fabricated. | OPEN | Phase 4 gate criterion 4.3 cannot close. What is owed by the date is a named slot or a stated refusal — R12 cannot shorten the bank app team's calendar, but silence is not an outcome |
| → [DEP-010](#1-edges) | Bank AD technology confirmation | Mahesh / Architecture | **2026-09-18** | **OVERDUE — reviewed 2026-09-30, still OPEN.** AIGEM suggested ESCALATE. No new date fabricated. | OPEN | WS-2 Phase 2 design cannot start. WS-2 has had no movement for a month; this is one reason why |
| → [DEP-20260824-dx1](#1-edges) | Bank-side VPN termination, prefixes, firewall change, DX order | Shivanshi / SRE + bank network | **2026-09-18** | **OVERDUE — reviewed 2026-09-30, still OPEN.** AIGEM suggested ESCALATE. Bank-side contact still unnamed. | OPEN | `uat` and `prod` keep running against CBS/AD stubs, so `#4` Customer and WS-2 Phase 2 cannot be evidenced. **The one item on the programme that working harder cannot accelerate** — which is exactly why a lapsed follow-up on it is the most expensive one in this table |
| → [DEP-20260824-eip](#1-edges) | 1SB and AU Bank PG allowlist the **correct** egress IPs (working belief: Apigee, `ASM-015`, not spoke NAT EIPs) | Shivanshi / SRE | **2026-09-18** | **OVERDUE — reviewed 2026-09-30, still OPEN.** AIGEM suggested ESCALATE. Written confirmations from 1SB / AU Bank PG have not arrived. | OPEN | W2 quotes and W3 payments fail in UAT regardless of code readiness |
| → [DEP-20260824-cst](#1-edges) | Cost envelope for the five 2026-08-24 layers (`RISK-012`, `NFR-OPEN-6`) | Shivanshi / SRE + Kalpana / Delivery | **2026-09-25** | **OVERDUE 5d at the 2026-09-30 review.** AIGEM suggested RE-DATE. No owner-confirmed new date is on file, so the row is not re-dated here. | OPEN | `GATE-S09` entry needs an approved cost envelope. Three stateful services, a sixth account, an inspection VPC per environment and two circuits are inside the S09 budget line and none of it is priced. WS-3 cannot enter S09 on an unpriced plan, and S09 is the next stage |
| → [DEP-20261006-igw](#1-edges) | Platform written inbound product: Amazon API Gateway per `ADR-018` **or** Board-accepted substitute with Deepali mapping (`CR-017`) | Shivanshi / SRE + bank platform team | **2026-10-20** | New 2026-10-06 | OPEN | S09 P4 edge cannot be provisioned without either violating ADRs or stalling. Apigee outbound (`DEP-20260914-apg`) does not close this row |

> Every external dependency has an accountable chase owner and date. The dependency remains
> external; assignment makes the chase schedulable and does not pretend the answer is available.
>
> The two rows added on 2026-08-24 come from `ADR-009` and `ADR-010`. Deciding the connectivity
> *pattern* removed an open architecture decision — it did **not** remove the bank's own work, and
> recording that distinction is the point of both rows.

## 3. Resolved cycles

| Date | Cycle | Technique | Outcome |
|------|-------|-----------|---------|
| — | — | *none detected* | — |

Recurring cycles between the same components are an architecture signal, not a planning one —
escalate to the Architecture board ([07 §6](../07-DEPENDENCY_MODEL.md#6-cycles)).

## 4. Current execution view — WS-1 Phase 4

Eligible READY work is ordered first per [07 §5](../07-DEPENDENCY_MODEL.md#5-execution-ordering).
Blocked criteria remain visible below it but are not selection candidates. Recompute on every
completion or blocker change; do not reuse a stale view.

| # | Item | P | Enables | Effort | State |
|---|------|---|---------|--------|-------|
| 1 | Gate 4.4 — compliance review of audit schema | P1 | 2 | M | READY |
| 2 | Gate 4.2 — publish OpenAPI + consumer collection | P2 | 1 | S | READY |
| 3 | Gate 4.7 — close or waive QA-001 coverage gate | P2 | 0 | M | READY — 🟡 Partial as of 2026-09-11: the coverage gate now executes on every PR, so what remains is a QA decision (raise the floor, or waive QA-001 with an expiry and a compensating control) |
| 4 | Gate 4.5 — operations runbook | P3 | 0 | S | READY |
| 5 | Gate 4.1 — sandbox E2E suite in CI (absorbs TD-014) | P1 | 3 | L | BLOCKED by GATE-4.1-SANDBOX-E2E |
| 6 | Gate 4.6 — performance smoke | P2 | 0 | M | BLOCKED by DEP-003 (soft) |
| 7 | Gate 4.3 — bank consumer UAT | P2 | 0 | M | BLOCKED by DEP-001, DEP-002 |

> This ordering is **derived, not a commitment.** It follows from the ratified gate criteria in
> [04](../04-STAGE_GATES.md) and the state file. The PO and Tech Lead own the actual sequence
> and re-order it at the weekly Governance Sync.
