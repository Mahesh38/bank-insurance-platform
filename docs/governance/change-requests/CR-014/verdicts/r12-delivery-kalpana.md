# `R12` — Delivery · Position on CR-014

**Change request:** [CR-014](../../CR-014-gitlab-estate-migration.md) · **Plan:** [`GLM-001`](../../../../platform/gitlab-migration/GLM-001-migration-plan.md)
**Role:** `R12` — Delivery Lead. **One persona, not an eighth board.**
**Persona:** Kalpana — Principal Delivery Head
**Reviewer type:** `AGENT` · **Change tier:** `T4` · **Date:** 2026-08-29

> ### Rule PA-1
> **R12 may compel a decision to happen. R12 may never supply its content.**
> Everything below sets windows, names owners and records blockage. Nothing below decides anything.

> ## Drafted position: `CANDIDATE` · delivery severity `DL1`
> **`CANDIDATE` is not approval**, and I cannot convert it into one. `CR-014` §10 records
> `decision: PENDING` and `approvers: []`. That is the state of this change request, and it does not
> improve by being planned around.

---

## 1. Decisions I am compelling, with windows

Four decisions gate every technical phase. I am publishing required-by dates against named owners.
I am not choosing any of them.

| Decision | Owner | Severity | Required by | Status |
|---|---|---|---|---|
| **M0.3** gate-evidence strategy — close `S08` on GitHub, or re-evidence on GitLab | Kalpana **convenes** · Amit + the boards decide | `DL1` | **2026-09-02** | `DECISION-REQUIRED` |
| **M0.4** governance-tree home | Mahesh (internal) → **bank authority** (Appendix C) | `DL1` | **2026-09-02** internal · bank date not ours to set | `DECISION-REQUIRED` |
| **M0.6** Render disposition | Shivanshi + Kalpana | `DL2` | **2026-09-05** | `DECISION-REQUIRED` |
| **CR-014** T4 signatures — Architecture, Security, Risk & Compliance | Three named humans | `DL1` | **2026-09-05** | `SIGNATURE-OUTSTANDING` |

M0.3 is the one with a real cost of delay. Every day it stays open is a day of GitHub Actions
investment that may be discarded. Amit has given an engineering input at `ENG-F05`; that is an
input, and I am not treating it as the answer.

---

## 2. `DECISION-BLOCKED` — recorded, not averaged

Eleven bank enterprise inputs (GLM-001 M1.2–M1.9, plus residency per `CMP-F01`) are outside this
team's control. They are recorded as `ASM-012` … `ASM-022`, expiry **2026-09-19**.

I am recording the programme as **`DECISION-BLOCKED` on M1**, and I will not convert that into a
green forecast, an assumed answer, or a provisional default. Two of them are load-bearing:

- **M1.2 edition** — if Premium rather than Ultimate, `S08-G5`'s mechanism changes and only Deepali
  may grant that exception. She has explicitly declined to pre-approve it.
- **`CMP-F01` residency** — if the instance is outside India, Shailja cannot rule the estate
  permissible and the migration does not proceed on its current target. This one can invalidate the
  destination, not merely the schedule, and it was not in the plan until Board 6 raised it today.

Neither has a workaround I am permitted to invent.

---

## 3. Critical path

```text
CR-014 T4 signatures ─┐
M0.3 / M0.4 decisions ─┼─► M3 bootstrap IaC ─► M4 ─► M5 ─► M6 ─► M7 ─► M9 cutover
M1 enterprise inputs ──┘                         ▲
CMP-F01 residency ───────────────────────────────┘  (can invalidate the destination)
```

The critical path is **not engineering effort**. ≈175 agent-hours is four to five weeks of lane time
and it is not what decides the date. Enterprise inputs, three human signatures and protected applies
are. I want that in front of anyone reading a six-to-nine-week forecast, because the instinct will be
to add engineering capacity, and adding capacity to a decision queue does nothing.

**Forecast: 6–9 weeks, confidence LOW**, and low is the honest word while four decisions are open,
eleven inputs are unanswered and one of them can move the destination.

---

## 4. Sequencing positions

| Question | Position |
|---|---|
| Parallelism | M3 modules, the M5.4–M5.8 greenfield seeds and the M7 component templates are genuinely independent. Everything else is ordered by GLM-001 §4 and I am not compressing it |
| Freeze window | ≤48 h, named owner, announced start, **and a pre-agreed expiry action** (`C-OPS-3`). A freeze without a failure branch is a dual-write with a date on it |
| Render → EKS | Correctly separated (IMP-5). Merging them puts a runtime re-platform and the gate-evidence recovery on one critical path, and I would not accept that sequencing |
| `GATE-S08` during migration | Whatever M0.3 decides, the gate stays open across the cutover window. I will report it open. I will not mark it `CANDIDATE` to make a forecast look better |
| CR-015 | Runs in parallel, blocks nothing in CR-014, and must not be pulled onto the migration's critical path to "get it done while we're in there" |

---

## 5. What I may not do, and am not doing

- **Not** deciding M0.3, M0.4 or M0.6. I convene, date and escalate them. Their content is not mine.
- **Not** substituting a default for an unanswered enterprise input.
- **Not** converting `DECISION-BLOCKED` into approval or averaging it into green.
- **Not** fabricating the three human T4 signatures, and **not** escalating past their named owners
  to a more agreeable authority.
- **Not** waiving Shivanshi's, Deepali's or Shailja's conclusions because a date is at risk.
  Delivery urgency does not alter specialist authority.
- **Not** declaring a stage transition. `CANDIDATE` is not approval, here or anywhere.
