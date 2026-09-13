# CR-016 — North Star continuity + per-microservice service workstreams

**Change request:** CR-016  
**Date raised:** 2026-09-13  
**Status:** **APPROVED (Architecture / governance operating model)** 2026-09-13 by **Mahesh** — Product (Rajal) and Delivery (Kalpana) counter-signatures outstanding for `CURRENT-STATE.yaml` topology transcription  
**Change type:** `GOV` — workstream operating-model recalibration  
**Origin:** `SUG-20260913-wss`  
**Runtime impact:** None. No application code, API contract or production configuration changes.  
**Deferred gate capacity (BR-4 / GC-1):** temporary attention from `S08-G10` (new-engineer build/test/ship path) while the operating model is recorded — recovered by enabling parallel service ownership.

---

## 1. Request

Replace the current *three program workstreams* agent-operating posture with:

1. **One continuous North Star program workstream** (`WS-NS`) that the team keeps working until the North Star outcome is reached.
2. **One service workstream per microservice** (`SWS-*`) where agents take independent ownership and keep a living progress markdown board (active · completed · waiting on another service).
3. **Governance sync checks** between dependent services before claiming cross-service work Done.

## 2. Current position

Today AIGEM registers three lifecycle workstreams in
[`state/CURRENT-STATE.yaml`](../state/CURRENT-STATE.yaml):

| ID | Role today |
|---|---|
| **WS-3** | Primary platform programme (S08, GATE-S08) |
| **WS-1** | Supplier — 1SB integration |
| **WS-2** | Enabler — workforce IAM |

That topology is recorded in
[`ADR-002`](../../platform/architecture-review/08-architecture-decision-log.md#adr-002--workstream-topology-ws-3-is-the-platform-ws-1-is-a-supplier-ws-2-is-an-enabler)
and chartered under [`workstreams/README.md`](../workstreams/README.md).

**Problem for multi-agent delivery:** agents lack a first-class ownership lane per deployable
service, so progress, blockers and cross-service sync are invisible except inside the three
programme gates. Parallel ownership toward the North Star is under-specified.

## 3. Proposed change

| # | Change | Artefact |
|---|---|---|
| A1 | Adopt two-tier workstream model: **program** (`WS-NS`) + **service** (`SWS-*`) | [`WORKSTREAM-STRATEGY.md`](../workstreams/WORKSTREAM-STRATEGY.md) · [`ADR-020`](../../platform/architecture-review/ADR-020-north-star-and-service-workstream-strategy.md) |
| A2 | Stage-fit / gates continue against the **program** North Star workstream only — service workstreams do **not** mint independent lifecycle stages | `WORKSTREAM-STRATEGY.md` §2–§3 · amends ADR-002 constraint on “fourth workstream” for *execution lanes* |
| A3 | Map every catalogue microservice to one `SWS-*` progress board | [`workstreams/service-progress/`](../workstreams/service-progress/README.md) |
| A4 | Dependent services must pass a **sync check** (contract / shared AC / DEP edge) before either side marks cross-service work Done | `WORKSTREAM-STRATEGY.md` §5 |
| A5 | Preserve WS-1 / WS-2 / WS-3 as **domain cluster labels** during transition; do not delete history | `WORKSTREAM-STRATEGY.md` §4 |
| A6 | `CURRENT-STATE.yaml` topology transcription is a **separate human R12 act** after Product + Delivery countersign | Not performed by this CR’s agent turn (Rule: agents do not edit stage state) |

## 4. What this CR does NOT change

- Product North Star *outcome* wording (still Rajal’s) — only the *operating lanes* change.
- Standing constraints, seven boards, nine personas, T4 human sign-offs.
- WS-1 / WS-2 / WS-3 historical gates and evidence already recorded.
- Service boundaries, ports, or bounded-context ownership (still Architecture / existing ADRs).
- Permission for agents to edit `current_phase` / `stage_status` in `CURRENT-STATE.yaml`.

## 5. Driver

**Business priority change + delivery operating evidence:** Mahesh (Architecture / governance
custodian) directed on 2026-09-13 that the team needs one continuous North Star lane and
independent per-microservice agent ownership with progress boards and dependency sync, so
multiple agents can advance in parallel without losing governance.

## 6. Impact

| Dimension | Effect |
|---|---|
| Scope | Operating model only — no product scope expansion |
| Stage | No stage transition; GATE-S08 remains the open program gate until transcribed otherwise |
| Dependencies | Makes cross-service DEP edges visible on SWS boards; does not invent new runtime deps |
| Parked items | None auto-unparked |
| Effort | S–M (docs + boards) |
| Risk if rejected | Parallel multi-agent delivery stays ungoverned; ownership and unblock waits stay tribal |

## 7. Alternatives considered

| Option | Why not |
|---|---|
| Do nothing | Leaves multi-agent ownership without a governed lane per service |
| Mint a full AIGEM lifecycle workstream per microservice | Explodes stage-fit (Rule LC-1), gate hygiene and FreshnessCheck cost — rejected |
| Keep only WS-1/2/3 and add informal notes | Fails the requirement for durable per-service progress + sync checks |

## 8. Approvals

| Authority | Verdict | Date |
|---|---|---|
| **Mahesh — Architecture / governance owner (R2)** | **APPROVED** — operating model, ADR-020, progress-board standard | **2026-09-13** |
| Rajal — Product (R1) | Outstanding — North Star objective continuity countersign | — |
| Kalpana — Delivery (R12) | Outstanding — critical-path / parallelization countersign + `CURRENT-STATE.yaml` transcription | — |

> **Rule CC-1:** this record captures the named human Architecture approval stated by Mahesh.
> It is not an agent-manufactured signature. Product and Delivery countersignatures remain human.

## 9. Decision

```yaml
change_request:
  id: CR-016
  raised_by: "human:mahesh"
  date: "2026-09-13"
  type: GOV
  decision: APPROVED_ARCHITECTURE
  decided_on: "2026-09-13"
  approvers:
    - "Mahesh (R2) — APPROVED 2026-09-13"
  conditions:
    - "Rajal R1 countersign before North Star objective text is altered in CURRENT-STATE.yaml"
    - "Kalpana R12 countersign + human transcription before workstreams: topology in CURRENT-STATE.yaml changes"
    - "Service workstreams never carry independent lifecycle stage_status"
```

---

## 10. Board review pack

Draft AIGEM board simulations (2026-09-13): [`CR-016/verdicts/`](./CR-016/verdicts/README.md).

## 11. Conditions closure (mandatory before “moving ahead”)

Board HOLDs and `must_fix` items are real. Solutions are recorded in
[`CR-016/CONDITIONS-CLOSURE.md`](./CR-016/CONDITIONS-CLOSURE.md).

| Residual human act | Brief |
|---|---|
| H1 Rajal Product countersign | [`countersign-rajal-north-star-continuity.md`](./CR-016/countersign-rajal-north-star-continuity.md) |
| H2 Kalpana Delivery countersign + transcription | [`countersign-kalpana-transcription-checklist.md`](./CR-016/countersign-kalpana-transcription-checklist.md) |

Until H1 and H2 complete: agents may use `SWS-*` boards under interim Rule WS-NS-3, but must not
rewrite `CURRENT-STATE.yaml` topology or claim GATE progress from board counts.
