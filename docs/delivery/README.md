# Delivery — R0 Timeline, Sprint Plan and Go-Live

**Owner:** Kalpana — Principal Insurance Platform Delivery Head (R12)
**Workstream:** WS-3 — AU Bank Insurance Distribution Platform
**Objective:** `R0-ASSISTED-TERM-SALE`
**Baseline:** 2026-08-17 · **Production deploy:** 2026-12-16 · **Go-live:** 2027-01-01

---

## The answer in one page

| Question asked | Answer |
|---|---|
| **How many sprints?** | **8 sprints of 2 weeks**, plus a 1-week Sprint 0 mobilisation and a 3-day cutover window |
| **How many stories per sprint?** | **249 in-scope stories** of the 447 in the backlog. 38 · 48 · 38 · 19 · 23 · 22 · 35 · 26 |
| **How many epics?** | **63 of the 99** backlog epics fall inside the R0 window. S13 and S15 are out by design |
| **What runs in parallel?** | **Eight lanes** — six delivery squads and two standing tracks. Peak concurrency is Sprint 3 |
| **What is genuinely dependent?** | **15 hard technical edges + 7 governance edges.** Everything else was broken with a contract, a mock or a flag |
| **When is production deployed?** | **16 December 2026**, after `GATE-S12` and `GATE-S14` pass on 11 December |
| **What happens in the 15 days?** | **Four-phase operational rehearsal**: synthetic → controlled live pilot → destructive (DR, restore, rollback, incident) → freeze and certify |
| **Will it make 1 January?** | **72% for R0-Core.** **Below 20% if the AWS landing zone slips past 5 October** — that single external dependency has no workaround |

---

## Documents

| # | Document | Read it for |
|---|---|---|
| **00** | [Stakeholder brainstorming session](./00-STAKEHOLDER-BRAINSTORMING-SESSION.md) | Every stakeholder in the repository, their position, their immovable constraint, the four conflicts and how each was resolved |
| **01** | [Delivery timeline and sprint plan](./01-DELIVERY-TIMELINE-AND-SPRINT-PLAN.md) | The calendar, holiday impacts, squad model, sprint-by-sprint goals, gate schedule, cadence |
| **02** | [Sprint backlog allocation](./02-SPRINT-BACKLOG-ALLOCATION.md) | Every epic and story placed in a sprint with an owning squad and an effort label |
| **03** | [Dependency and parallelisation map](./03-DEPENDENCY-AND-PARALLELISATION-MAP.md) | What runs in parallel, what truly cannot, the critical path, the three items that can move the date today |
| **04** | [Dry run and go-live plan](./04-DRY-RUN-AND-GO-LIVE-PLAN.md) | Cutover 14–16 Dec, the 15 days day by day, go/no-go, withdrawal criteria, hypercare |
| **05** | [Forecast, confidence and descope levers](./05-FORECAST-CONFIDENCE-AND-DESCOPE-LEVERS.md) | Confidence model and sensitivity, 9 new risks, 8 assumptions, 6 descope levers in pull order, what is *not* a lever |

---

## The critical dates

| Date | Milestone | Owner |
|---|---|---|
| **2026-08-21** | AWS landing-zone request submitted · pentest RFQ issued · **T4 human sign-off slots booked** | Shivanshi · Deepali · Kalpana |
| **2026-08-28** | **Consent + suitability E2 signatures** (`S02-G3`/`S02-G4`) — gates 50 stories · sponsor named (`GAP-010`) | **Shailja** · Rajal |
| **2026-09-06** | **API contracts frozen** — the move the whole parallel plan rests on | Mahesh + SQ-3 |
| **2026-09-20** | **`GATE-S08` PASS** · **AWS landing zone delivered** | Amit + Shivanshi |
| **2026-10-04** | **`GATE-S09` PASS** · **pentest vendor slot confirmed** | Shivanshi · Deepali |
| **2026-10-05** | **AWS hard checkpoint — past this, the date is lost** | Rajal + sponsor |
| **2026-10-18** | **C1–C4 implemented at 100% coverage** — lawfully shippable in principle | Deepali + Swapnali |
| **2026-11-15** | **`GATE-S11` PASS — first complete R0 sale in UAT.** The stage never before attempted | Rajal + Amit |
| **2026-11-29** | Independent penetration test executed | Deepali |
| **2026-12-11** | **`GATE-S12` + `GATE-S14` PASS · human T4 sign-offs · go/no-go** | All authorities |
| **2026-12-16** | **Production deployment — M7** | Shivanshi |
| **2026-12-17 → 31** | **15-day dry run**, four phases | Shivanshi + Swapnali |
| **2027-01-01** | **Go-live. Hypercare. S15 begins** | Rajal + Shivanshi |

---

## The three items most capable of moving the date today

1. **AWS landing zone** — Shivanshi + bank infrastructure — required by **2026-09-20** — `DL0` —
   **no workaround exists.**
2. **Consent + suitability signature** — Shailja — required by **2026-08-28** — `DL0` — gates the
   entire business case.
3. **Pentest vendor slot** — Deepali + procurement — required by **2026-10-04** — `DL0` — Deepali
   blocks go-live without it, correctly.

---

## Authority boundary of these documents

Kalpana owns the **integrated delivery plan, milestones, critical path, sequencing, safe
parallelisation and the truthful forecast**. Kalpana owns **none** of the following, and nothing in
these documents grants them:

- **Scope, priority, business rules, R0 outcome** → Rajal
- **Architecture, build order, service boundaries** → Mahesh
- **Application implementation** → Amit
- **Security outcome and Board 4 verdict** → Deepali
- **Persistence integrity, recovery, retention** → Aarti
- **Test strategy and evidence sufficiency** → Swapnali
- **Regulatory permissibility and Board 6 verdict** → Shailja
- **Operational readiness and Board 7 verdict** → Shivanshi
- **Stage transitions and gate approvals** → the listed approvers
- **Mandatory T4 sign-offs and material risk acceptance** → **named humans only**

> **Rule PA-1** — R12 may compel a decision to happen; R12 may never supply its content.
> **Rule 9** — `CANDIDATE` is readiness for decision, not stage-transition approval.

---

## Relationship to existing governance

These documents **derive from** and do not replace:

- [`state/CURRENT-STATE.yaml`](../governance/state/CURRENT-STATE.yaml) — WS-3 scope, standing constraints, the `never` list
- [`state/GATE-EVIDENCE.yaml`](../governance/state/GATE-EVIDENCE.yaml) — live gate criteria state
- [`BACKLOG.yaml`](../application-lifecycle-bible/backlog/BACKLOG.yaml) — the 447-story source of truth
- [`01-POSITION-ASSESSMENT.md`](../application-lifecycle-bible/01-POSITION-ASSESSMENT.md) — where the platform actually is
- [`03-REALIGNMENT-PLAN.md`](../application-lifecycle-bible/03-REALIGNMENT-PLAN.md) — underpin, do not demolish
- [`PERSONA-AUTHORITY-MATRIX.md`](../governance/PERSONA-AUTHORITY-MATRIX.md) — who decides what

Adopting this plan as binding requires a **CR** under
[14-CHANGE_CONTROL](../governance/14-CHANGE_CONTROL.md), carrying verdicts from Architecture,
Security, Risk & Compliance, QA, Operations, Product and Delivery. **It is a proposal until then.**
