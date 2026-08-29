# Dependency Register

Edges of the dependency graph. Edges outlive the items that discovered them, so they are
recorded once and reused by every later ordering computation.

**Owner:** Tech Lead · Architect (architectural and decision edges)
**Model:** [07-DEPENDENCY_MODEL.md](../07-DEPENDENCY_MODEL.md)

---

## 1. Edges

| ID | From | Relation | To | Type | State | Notes |
|----|------|----------|----|------|-------|-------|
| DEP-001 | Gate 4.3 (bank consumer UAT) | `blocked_by` | Gate 4.2 (OpenAPI published) | HARD | OPEN | A consumer cannot integrate against an unpublished contract |
| DEP-002 | Gate 4.3 | `external` | Bank app team UAT slot | EXTERNAL | OPEN | Owner Rajal / Product; follow-up 2026-08-21 |
| DEP-003 | Gate 4.6 (performance smoke) | `blocked_by` | Gate 4.1 (sandbox E2E in CI) | SOFT | OPEN | Smoke reuses the E2E harness; could be built standalone at higher cost |
| DEP-004 | TD-014 (integration ↔ persistence E2E) | `enables` | Gate 4.1 | TECHNICAL | OPEN | Parked item whose trigger has fired |
| DEP-005 | Phase 5 (Expand LOBs) | `blocked_by` | Phase 4 gate | HARD | OPEN | "Do not start Health/Motor until Phase 3/4 exit is met" |
| DEP-006 | TD-010 (Redis idempotency) | `blocked_by` | Horizontal scale-out decision | DECISION | OPEN | Needs an ADR before implementation, not just capacity |
| DEP-007 | TD-006 (AWS Secrets Manager) | `blocked_by` | AWS deployment target confirmed | ENVIRONMENT | OPEN | Prod profile fails fast until then |
| DEP-008 | Gate 4.4 (compliance review) | `enables` | TD-023 scope (raw payload capture breadth) | COMPLIANCE | OPEN | The review decides how far capture must extend |
| DEP-009 | WS-2 Phase 2 (production IdP) | `blocked_by` | WS-2 Phase 1 gate | HARD | OPEN | Deliberate deferral behind the adapter |
| DEP-010 | WS-2 Phase 2 (AD federation) | `external` | Bank confirms AD technology | EXTERNAL | OPEN | Owner Mahesh / Architecture; follow-up 2026-08-21 |
| DEP-011 | TD-007 (tighten ArchUnit) | `requires` | Packages populated by LOB expansion | TECHNICAL | OPEN | Cannot tighten rules against empty packages |
| DEP-20260824-dx1 | R0 bank connectivity (`ADR-009`) | `external` | Bank terminates the VPN, publishes prefixes, opens its firewall, accepts the DX order | EXTERNAL | OPEN | Owner Shivanshi / SRE with the bank network team; follow-up 2026-08-28. The **pattern** is decided; the bank's own work is not. `uat` cannot leave stubs behind until the VPN half exists |
| DEP-20260824-eip | 1SB and AU Bank PG allowlists | `blocked_by` | Publication of the **inspection-VPC** Elastic IPs (`ADR-010`, LLD §2.3) | EXTERNAL | OPEN | Owner Shivanshi; follow-up 2026-08-28. The addresses moved out of the workload VPCs. Any allowlist conversation already started must be re-based — from 1SB's side, a stale allowlist is indistinguishable from none |
| DEP-20260824-cst | `GATE-S09` entry (cloud account structure and budget approved) | `blocked_by` | Cost envelope for the five 2026-08-24 layers (`RISK-012`, `NFR-OPEN-6`) | DECISION | OPEN | Owner Shivanshi + Kalpana. Three stateful services, a sixth account, an inspection VPC per environment and two circuits are now inside the S09 budget line, and none of it is priced |
| DEP-20260824-evd | `#16` Audit consumer (W3) | `requires` | MSK topics, per-topic IAM and the Glue Schema Registry (`ADR-012`) | TECHNICAL | OPEN | Owner Amit + Shivanshi. Writing the audit path against a direct outbox poll and moving it later is a rewrite of the one component that must not lose a record |
| DEP-20260829-cr14 | GLM-001 M3 (bootstrap IaC) | `blocked_by` | CR-014 board approval | DECISION | **DONE** | `APPROVED_WITH_CONDITIONS` 2026-08-29 with 29 board conditions plus `AC-1`…`AC-5`. M3 is unblocked |
| DEP-20260829-m1 | All GLM-001 technical phases | `external` | Eleven bank enterprise inputs (`ASM-012`…`ASM-022`) | EXTERNAL | OPEN | Owner Shivanshi / SRE → bank GitLab platform team. Follow-up 2026-09-19. Kalpana has recorded the programme `DECISION-BLOCKED` on this and will not average it into green |
| DEP-20260829-res | GLM-001 M5.2 (first push to the bank estate) | `blocked_by` | Data residency confirmed permissible (`C-CMP-1`, `ASM-022`) | COMPLIANCE | OPEN | Owner Shailja / Board 6. **Can invalidate the destination, not merely the schedule.** Raised as `CMP-F01` in the CR-014 board round |
| DEP-20260829-sec | GLM-001 M5.2 (first push) | `blocked_by` | Clean blocking full-history secret scan (`C-SEC-1`) | SECURITY | OPEN | Owner Deepali / Board 4. On a finding: rotate → scrub → re-scan (`C-SEC-2`). No workaround exists |
| DEP-20260829-gov | GLM-001 M4.3 (create `governance/platform-governance`) | `blocked_by` | Bank Appendix C exception accepted in writing (`AC-2`, `C-ARC-2`, `ASM-021`) | EXTERNAL | OPEN | Owner: bank GitLab platform / architecture authority. **Approved conditionally on 2026-08-29** — until the bank accepts, M4.3 creates **eight** projects, not nine. Board-approved fallback in `DEC-20260829-01` §3.2 |
| DEP-20260829-jtk | GLM-001 M7.9 (backend affected-component pipeline) | `blocked_by` | Job-token allowlists applied (M6.8, `C-OPS-5`) | TECHNICAL | OPEN | Owner Shivanshi / SRE. The usual monorepo-split CI failure, and it fails **after** cutover when GitHub is already read-only |
| DEP-20260829-cr15 | CR-015 verdict | `blocked_by` | Joint Mahesh + Aarti review | ARCHITECTURAL | **DONE** | Option B approved 2026-08-29 (`ADR-019`) — target model only. Q4 moves to `DEP-20260829-q4` and stays open |
| DEP-20260829-alloc | S09 persistence allocation migration (`ADR-019`) | `blocked_by` | `CR-014` cutover complete — GitLab authoritative, freeze released | HARD | OPEN | Owner Aarti + Mahesh. `AC-5`: repository migration is never combined with persistence restructuring |
| DEP-20260829-q4 | S09 persistence allocation migration | `blocked_by` | Aarti's integrity and recovery review (`CR-015` Q4) incl. restore test vs RPO 5 min / RTO 30 min | DATA | OPEN | Owner Aarti — **not substitutable**. Approving the target model did not answer it |

## 2. External dependencies

Every `EXTERNAL` edge needs an owner and a follow-up date, or it is not tracked — it is hoped
for (Rule DEP-3).

| ID | Dependency | Owner | Follow-up | State | Impact if late |
|----|------------|-------|-----------|-------|----------------|
| → [DEP-002](#1-edges) | Bank app team UAT integration slot | Rajal / Product | 2026-08-21 | OPEN | Phase 4 gate criterion 4.3 cannot close |
| → [DEP-010](#1-edges) | Bank AD technology confirmation | Mahesh / Architecture | 2026-08-21 | OPEN | WS-2 Phase 2 design cannot start |
| → [DEP-20260824-dx1](#1-edges) | Bank-side VPN termination, prefixes, firewall change, DX order | Shivanshi / SRE + bank network | 2026-08-28 | OPEN | `uat` and `prod` keep running against CBS/AD stubs, so `#4` Customer and WS-2 Phase 2 cannot be evidenced. **The one item on the programme that working harder cannot accelerate** |
| → [DEP-20260824-eip](#1-edges) | 1SB and AU Bank PG allowlist the inspection-VPC Elastic IPs | Shivanshi / SRE | 2026-08-28 | OPEN | W2 quotes and W3 payments fail in UAT regardless of code readiness |

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
| 3 | Gate 4.7 — close or waive QA-001 coverage gate | P2 | 0 | M | READY (criterion ratified by CR-001) |
| 4 | Gate 4.5 — operations runbook | P3 | 0 | S | READY |
| 5 | Gate 4.1 — sandbox E2E suite in CI (absorbs TD-014) | P1 | 3 | L | BLOCKED by GATE-4.1-SANDBOX-E2E |
| 6 | Gate 4.6 — performance smoke | P2 | 0 | M | BLOCKED by DEP-003 (soft) |
| 7 | Gate 4.3 — bank consumer UAT | P2 | 0 | M | BLOCKED by DEP-001, DEP-002 |

> This ordering is **derived, not a commitment.** It follows from the ratified gate criteria in
> [04](../04-STAGE_GATES.md) and the state file. The PO and Tech Lead own the actual sequence
> and re-order it at the weekly Governance Sync.
