# Programme roll-up — North Star critical path over service boards

**Authority:** CR-016 conditions C8, C14, C17, C19, C20 · [WORKSTREAM-STRATEGY](./WORKSTREAM-STRATEGY.md)  
**Owners:** Kalpana (roll-up / forecast) · Swapnali (journey evidence sufficiency) · Rajal (outcome priority)  
**Date:** 2026-09-13

---

## 1. Problem this solves

Twenty-one `SWS-*` boards can all look healthy while the assisted Life journey is blocked.
**Service green ≠ programme green.** This roll-up is the only artefact agents and humans may
use to claim programme-level progress under CR-016.

---

## 2. Hard rules

| Rule | Statement |
|---|---|
| **PR-1** | Forecast “green” is declared only against **programme milestones** on `WS-NS` / WS-3 carrier — never by counting Completed rows across SWS boards. |
| **PR-2** | GATE-S08 / QA / Board 7 evidence packs are **not** satisfied by board hygiene. |
| **PR-3** | When a local SWS priority conflicts with the North Star slice Rajal ordered, **programme priority wins** (Product outcome > local ownership). |
| **PR-4** | Every **Waiting to unblock** row ages on Delivery DL windows (see §4). |

---

## 3. Roll-up table (keep current weekly)

Update in the same cadence Kalpana already uses for gate hygiene. Seed rows point at R0 assisted Life — amend only with Product.

| Milestone (programme) | North Star contribution | Critical SWS lanes | Status | Blockers (SWS / DEP) | Evidence pack |
|---|---|---|---|---|---|
| M1 Foundation CI & architecture gates | Enables all lanes | platform CI, shared libs | track in GATE-S08 | — | GATE-S08 criteria |
| M2 Identity workforce path | RM can authenticate | `SWS-identity-*`, `SWS-workforce-access-bff` | local boards | — | WS-2 gate evidence |
| M3 Lead → consent → suitability spine | Journey legality | lead, consent, suitability, journey-orchestration | local boards | — | journey AC / tests |
| M4 Quote → proposal → payment → policy | Sale completion | quotation, proposal, payment, policy-issuance, integration-hub, 1sb-integration | local boards | — | journey AC / tests |
| M5 Audit / notification minimum | Evidentiary close | audit-compliance, notification | local boards | — | audit evidence rules |

**Status vocabulary for this table only:** `NOT_STARTED` · `IN_PROGRESS` · `BLOCKED` · `EVIDENCED`  
`EVIDENCED` requires the Evidence pack column — not SWS Completed counts.

---

## 4. Waiting-row ageing (DL windows)

Align with Delivery decision-forcing windows (Kalpana card):

| Age of oldest open wait on critical-path SWS | DL class | Action |
|---|---|---|
| 0–1 working day | DL0 | Owner pair sync; record on both boards |
| 2 working days | DL1 | Kalpana publishes required-by for the owning decision |
| ≤5 working days | DL2 | Convene owners; escalate to Architecture if contract dispute |
| Beyond agreed date / next gate cadence | DL3 | Record `DECISION-BLOCKED` on forecast; do **not** invent contract content |

Sandbox / env contention waits use the same ageing — they are delivery waits, not “background noise.”

---

## 5. Agent fan-out / shared environments

- Claiming an `SWS-*` does not grant unbounded parallel agents against shared UAT/sandbox.
- Kalpana sequences READY claims when contention appears (Operations C18).
- Producer load against 1SB demo/sandbox still obeys existing rate and standing constraints.

---

## 6. How agents must report

When asked “are we on track?” answer with:

1. Programme milestone status from this roll-up, then
2. Critical-path waits (aged), then
3. Optional: local SWS highlights.

Never lead with “18 of 21 boards have Completed rows.”
