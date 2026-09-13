# Board 1 — Architecture · Draft verdict on CR-016

**Board:** 1 — Architecture · **AIGEM role:** R2  
**Reviewer:** Mahesh — Principal Insurance Platform Architect  
**Reviewer type:** AGENT (AI simulation)  
**Self-review:** false  
**Change request:** [CR-016](../../CR-016-north-star-and-service-workstream-strategy.md)  
**ADR:** [ADR-020](../../../../platform/architecture-review/ADR-020-north-star-and-service-workstream-strategy.md)  
**Date:** 2026-09-13

> ## Draft: `APPROVE_WITH_CONDITIONS`
> **Architecture severity:** `A2` — operating-model change; no new bounded context, no new deployable, no trust-boundary move.
>
> **`signature_status: HUMAN_ALREADY_RECORDED_ON_CR`** for the Architecture approval dated 2026-09-13.  
> This AI pack **restates** Board 1 reasoning and conditions; it does not invent a second signature.

---

## 1. Decision requested of Architecture

| # | Decision | Draft |
|---|---|---|
| D-1 | Two-tier model: programme `WS-NS` + service lanes `SWS-*` | APPROVE |
| D-2 | Service lanes are **not** independent lifecycle stages | APPROVE — mandatory |
| D-3 | Amend ADR-002 *operating posture* without deleting WS-1/2/3 history | APPROVE |
| D-4 | Agents may edit `current_phase` / `stage_status` for `SWS-*` | **REJECT** |
| D-5 | Microservice = capability = bounded context | **REJECT** — standing Mahesh rule |

---

## 2. What I reviewed

CR-016 · ADR-020 · WORKSTREAM-STRATEGY.md · service-progress boards · ADR-002 · Mahesh card · AUTHORITY-QUICK-CARD · 11-REVIEW_GATES §3 / §15 · CURRENT-STATE.yaml (unchanged topology).

---

## 3. Findings

The structural problem ADR-002 solved (platform work needing a programme home) remains valid. CR-016 does **not** re-open “grow WS-1 into the platform” or “fourth programme workstream for Flutter.” It adds **execution/ownership lanes** under one North Star chase.

The main Architecture risk is **category error**: agents and humans calling an `SWS-*` a “workstream” in the AIGEM lifecycle sense. ADR-020 § Decision 2–3 and WORKSTREAM-STRATEGY rules WS-NS-1 / WS-NS-2 are the control — they must stay binding.

Second risk: **service catalogue ≠ domain model**. Boards are keyed to Gradle modules. That is correct for agent ownership of deployables; it must not silently redefine bounded contexts or merge Hub + adapter responsibilities.

---

## 4. Conditions (`must_fix` if human adopts)

1. Keep the sentence: *service workstreams never carry independent `stage_status`.*
2. Any proposal to triage stage-fit against an `SWS-*` alone is **SF4 / REJECT** unless a new CR supersedes ADR-020.
3. Standing constraints unchanged: no bank→1SB/DB direct; Hub mediation; Flutter tokens stay off-device.
4. Product (Rajal) and Delivery (Kalpana) countersigns remain required before `CURRENT-STATE.yaml` topology transcription — Architecture approval of the operating model does not replace them.

---

## 5. Concerns to understand (not blockers to the Architecture draft)

| Concern | Why it matters | Owner to close |
|---|---|---|
| Dual labels WS-3 vs WS-NS confuse agents | Wrong stage-fit target | R12 transcription + BOOT regen |
| 21 boards without critical-path roll-up | Parallelism without integration | Kalpana |
| Progress boards become a second product backlog | Scope drift | Rajal + Amit hygiene |

## 6. Signature status

Architecture human approval: **recorded on CR-016 (2026-09-13)**. AI draft aligns with that approval and adds clarifying conditions only.
