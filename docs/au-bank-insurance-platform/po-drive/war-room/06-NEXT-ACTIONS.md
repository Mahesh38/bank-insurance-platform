# 06 — Next Actions

**Status:** 🔵 **Suggestions, not assignments.** Nothing here has been agreed by the people named.
Each item says who is best placed to pick it up and why it matters now, so the accountable person
knows something is waiting for them.
**Date:** 2026-08-12
**Basis:** the pack in this folder, plus what is already open in the repository

---

## 1. What was actually committed

| | |
|---|---|
| **Files** | 13 changed, ~1,530 lines added |
| **Code changed** | **None.** No service, library, test, build file or configuration was touched |
| **Nature** | Six war-room documents, one persona, one triage record, one change request (`CR-014`, **PENDING**), two risks (`RISK-016`, `RISK-017`), two ID-counter bumps |
| **Reversible?** | Entirely. Nothing is enforced until a human approves `CR-014` and edits the governance state |

**Read that first line as the reassurance it is:** the realignment has cost the codebase nothing
so far. Everything expensive is still a decision, not a commitment.

---

## 2. The one thing that matters this week

> ### 🔴 Book the war room. — **PO (Rajal)**
>
> Six of the nine decisions in [04 §4](./04-WAR-ROOM-RUNSHEET.md#4-decisions-the-room-must-take)
> gate everything else in this pack. Until there is a date in calendars, the proposal is a
> document rather than a plan, and the build keeps running on the old sequence in the meantime.
>
> Two small prerequisites, both PO: **fill in the blank attendee names** in
> [04 §1](./04-WAR-ROOM-RUNSHEET.md#1-attendees) (Delivery Lead, Tech Lead, QA Lead, Security
> Architect, BA are unnamed), and **circulate the pack 48 hours ahead**.
>
> **Size:** 30 minutes. **Blocked by:** nothing.

---

## 3. Start now — these do not need the war room

Four tasks are unblocked today, useful regardless of what the room decides, and each answers a
question the room will ask.

### 3.1 Compliance review — **Vaishnavi** · highest value of anything on this page

Review the code and documents end to end and answer **C6**: *is anything already built something
you consider non-compliant today, requiring it to be stopped rather than scheduled?*

Scope, file paths and the eight known gaps are pre-assembled in
[her persona §5](../../../context/roles/vaishnavi-compliance-officer.md#5-her-review-scope-in-this-repository) —
she should not have to discover them. The three that most need her ruling:

| | Finding | The call she needs to make |
|---|---------|---------------------------|
| 1 | `consentRef` is **optional** today (`COMP-005` deferred to Phase 5.5) | Acceptable for UAT with non-live customers, or mandatory before any proposal anywhere? |
| 2 | `PiiMasker` exists only in `1sb-integration-service` — the three identity services have no masking | Required in auth logs too, and at what urgency? |
| 3 | Raw payload capture unwired for status/master-data (`TD-023`) | Is partial capture a finding, or is quote/proposal/payment sufficient? |

**Why now:** a "yes" on C6 is a P1 that changes the delivery plan the same evening. Finding that
out *before* the war room makes the room far more useful. This also closes WS-1 gate criterion
**4.4**, which is open regardless of the realignment.
**Size:** 1–2 days. **Blocked by:** nothing — she can start on a shared read of the repo.

### 3.2 The A4 verdict and its cleanup — **Solution Architect (Mahesh)**

The PO's read is that 1SB exposure into the domain is minor and removable. The task is to make
that a recorded decision rather than a shared impression:

- Confirm whether `com.bank.insurance.onesb.domain` and the `OneSb*` result types are a **naming
  artefact** or whether 1SB's shape actually drove the aggregate boundaries.
- Decide the target: rename, relocate behind the adapter boundary, or move to a separate module.
- Log it as a debt item with a target stage, so it is not rediscovered in three months.

**Why now:** cleanup is cheap while there is no bank-wide canonical model sitting on top of it.
It gets expensive the moment PZ.5 defines one.
**Size:** 1 hour to decide, small to execute. **Blocked by:** nothing.

### 3.3 RTM skeleton — **Business Analyst** · the genuine critical path

List the outward behaviours of the five built services — from the published OpenAPI spec and the
`FUNC-`/`COMP-`/`TECH-` backlog rows, **not** from reading the code — and put each in a row of a
Requirements Traceability Matrix with its requirement column empty.

**Why now:** it is useful whatever the war room decides; it is the artefact Compliance will ask
for; and it answers the two questions the room cannot currently answer — *how many behaviours are
we mapping* (question L5) and *how long will the retro-fit take* (question B3). Right now nobody
can size the work being proposed.
**Size:** 1–2 days for the skeleton. **Blocked by:** nothing. **Note:** no BA is named anywhere in
the repository — if there isn't one, this is the first thing the PO needs to resource.

### 3.4 Track B gate work — **Tech Lead + QA Lead**

Already sanctioned, on-stage, and needing no decision from anybody:

| Item | Owner | Note |
|------|-------|------|
| **4.1** Sandbox E2E for the Term path in CI (or gated nightly) | Tech Lead | Also closes `TD-014` |
| **4.5** Ops runbook — secrets rotation, IP whitelist, 1SB 401/5xx | Tech Lead + Ops | Pure documentation of existing behaviour |
| **4.6** Performance smoke — p95 quote under nominal concurrency | Tech Lead | Needs a target from the NFR sheet (`GAP-017`); measure first, set the target after |
| **4.7 / QA-001** Coverage gate — close it, or issue a dated waiver | QA Lead | Waiver needs TL + QA Lead co-approval |
| `CR-001` counter-signature | QA Lead | Outstanding since 2026-08-10 |

**Why now:** this is the "keep delivering while we realign" work. If the realignment produces two
quiet sprints, the model has already failed. **Size:** 1–2 sprints. **Blocked by:** nothing.

---

## 4. Long-lead items — start chasing today, not after the war room

Both have been open for weeks and both have external turnaround times that no amount of internal
planning can compress.

| Item | Owner | Open since | Why it cannot wait |
|------|-------|-----------|-------------------|
| **Name the Executive Sponsor** (`GAP-010`) | PO | 2026-07-31 | PZ.1 cannot close without a named sponsor, and Wave 0 exit needs their signature. If the role is known but the person is not appointed, that is an escalation, not a task |
| **Find the bank caller for gate 4.3** (`DEP-002` / `RISK-002`) | PO + Delivery Lead | — | Has **no owner and no date**. It is the one Phase 4 criterion the team cannot close by working harder. Start the conversation now so it is not the thing holding the gate in three weeks |

---

## 5. Waits for the war room

Do **not** start these before `CR-014` is decided — they change governed state, and starting them
early repeats the exact mistake this pack exists to correct.

| Item | Owner | Waits for |
|------|-------|-----------|
| Add workstream `WS-0` to `CURRENT-STATE.yaml` and `01-CURRENT_STATE.md` | Delivery Lead | D-WR-03 |
| Add WS-0 stage map and gates | Architect | D-WR-03 |
| Adopt Rules S-1…S-4; amend Definition of Ready / Done | Architect + PO | D-WR-04, D-WR-05 |
| Add WS-0 routing entries | Delivery Lead | D-WR-03 |
| Reorganise the board around flows F0–F11 | Delivery Lead | D-WR-06 |
| Confirm the 40/60 capacity split against real names | Technical Head | D-WR-07 |
| Write BR requirements with acceptance criteria for F1 | BA + PO | D-WR-02 |

**One exception worth taking early:** the **Technical Head** can map the 40/60 split to actual
names *before* the room, so D-WR-07 is a confirmation rather than a live negotiation. Questions
T1 and T2 in [05 §2](./05-STAKEHOLDER-REVIEW-SHEET.md#2-technical-head--amit) — particularly
whether "nobody is on both tracks" survives contact with the real headcount.

---

## 6. Who has something waiting for them

| Owner | Next action | When | Blocked? |
|-------|-------------|------|:--------:|
| **PO — Rajal** | Book the war room; fill in attendee names; circulate the pack | This week | No |
| **PO — Rajal** | Chase sponsor naming (`GAP-010`) and the 4.3 bank caller (`DEP-002`) | This week | External |
| **Compliance — Vaishnavi** | End-to-end review; answer **C6**; closes gate 4.4 | Before the war room if possible | No |
| **Architect — Mahesh** | A4 verdict on the `onesb` domain namespace; log the cleanup | 1 hour | No |
| **Technical Head — Amit** | Map 40/60 to real names; answer T1/T2 | Before the war room | No |
| **Tech Lead** | Gate 4.1 E2E in CI · 4.5 runbook · 4.6 perf smoke | Now, continuous | No |
| **QA Lead** | Close or waive 4.7 / QA-001; counter-sign `CR-001` | This sprint | No |
| **BA** *(unnamed — resource first)* | RTM skeleton for the five built services | This week | No |
| **Delivery Lead** *(unnamed)* | Prepare the eight governance edits; decide how Rule S-1 is detected | After the war room | Yes — `CR-014` |
| **Security Architect** *(unnamed)* | Review the auth design that shipped in the same commit as its spec (question S1) | Before the war room | No |

**Three of the ten rows have no name against them** — BA, Delivery Lead, Security Architect. That
is worth noticing on its own: the roles carrying the corrective work are the ones the programme
has not yet staffed.
