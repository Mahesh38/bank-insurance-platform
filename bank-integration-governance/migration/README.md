# Migration plan — AIGEM 1.5 → FLOW

Six weeks, pilot first, reversible at every step, and no historical evidence destroyed.

## 1. Shape

```text
 Wk 0      Wk 1          Wk 2–3                 Wk 4–5                 Wk 6
 DECIDE ─► CARRY OVER ─► PILOT on integration ─► EXTEND to all lanes ─► DECOMMISSION AIGEM
            (state)       hub + platform lanes    switch agent entry      archive read-only
                          (AIGEM still binding    points in product repo
                           elsewhere)
     ▲                         │
     └──── rollback at any point: repoint AGENTS.md / CLAUDE.md; nothing was deleted ────┘
```

## 2. Decisions only humans can make

Nothing in this repository takes effect until these are made. An AI agent prepared them; it cannot
make them.

| # | Decision | Owner | Recorded as |
|---|---|---|---|
| H1 | Adopt FLOW via this plan, including a pilot | Sponsor, with Architecture (framework custodian under AIGEM) | `DEC-MIG-01` |
| H2 | Confirm each role holder in `personas/roster.yaml`, name deputies, **name a second human able to approve R2 changes**, and name the sponsor | Each holder | `roster.yaml` `confirmed: true` |
| H3 | Accept the guardrail list as complete for now | Architecture, Security, Compliance | commit approving `state/guardrails.yaml` |
| H4 | Confirm the initial outcome order | Product | `state/outcomes.yaml` `status: confirmed` |
| H5 | Treat engineering foundation (GATE-S08) as complete | Architecture (+ Product) | `DEC-MIG-02` |
| H6 | Where the repository lives (new GitHub repo vs folder in the product repo) | Architecture | README |

## 3. Phases and exit signals

### M0 — Decide (week 0)
- M0.1 Walk the sponsor and role holders through [`../docs/00-diagnosis.md`](../docs/00-diagnosis.md) and this plan (45 min).
- M0.2 Take H1 and H6. Freeze AIGEM **framework** edits (no new CRs to `docs/governance/**` L1 files); AIGEM remains the operating process until M3.
- **Exit:** `DEC-MIG-01` decided.

### M1 — Carry over (week 1)
- M1.1 Guardrails: review `state/guardrails.yaml` against `CURRENT-STATE.yaml` standing constraints and `never` lists — nothing dropped, nothing new invented (H3).
- M1.2 Work: import every open work item into the tracker (GitHub Projects or Jira) with lane, horizon and class — see [`carry-over.md`](./carry-over.md). Registers are **not** migrated row by row.
- M1.3 Roster: H2.
- M1.4 Decisions: first decision clinic sets real `needed_by` dates on `state/decisions.yaml`; `DEC-MIG-02` and the first ADR batch taken.
- M1.5 Dependencies: owners confirm chase dates in `state/dependencies.yaml`.
- **Exit:** tracker has every open item; decisions and dependencies have real dates; roster confirmed.

### M2 — Pilot (weeks 2–3) — `integration-hub` and `platform` lanes
Chosen because they have the most blocked-but-unblockable work (GATE-P4 4.2/4.4/4.5 are independent;
platform IaC has waited on a stage signature) and the clearest guardrails.
- M2.1 Product confirms outcome order (H4). Weekly replenishment and twice-weekly decision clinic start.
- M2.2 Agents in the pilot lanes use this repo's `AGENTS.md` and skills; PRs carry R0/R1/R2 labels; advisors run in parallel.
- M2.3 Flow steward digest runs daily (manual AI run is fine; automation later).
- M2.4 Other lanes stay on AIGEM. A pilot-lane change that touches a non-pilot lane follows the stricter of the two.
- **Exit — go/no-go at end of week 3**, against the week-1 baseline:

| Signal | Go if |
|---|---|
| Lead time p85 (pilot lanes) | improved |
| Review wait p85 | ≤ 2 working days |
| Decisions past SLA | ≤ 20% |
| Guardrail or R2 escapes | **zero** |
| Bypass rate | < 10% |
| Pilot lane leads' verdict | "keep" |

### M3 — Extend (weeks 4–5)
- M3.1 Remaining lanes switch. Replace the product repo's `AGENTS.md` / `CLAUDE.md` with a short pointer to this repository's `AGENTS.md` (or vendor it in — H6).
- M3.2 Replace `.claude/skills/aigem-triage` and `context-load` in the product repo with `flow-intake`, `load-context`, `advise`.
- M3.3 Wire guardrail automation candidates: GR-ARC-05 contract lint, GR-GOV-01 PR check on approval fields.
- M3.4 Replace `FreshnessCheck` exit-2 halting with `validate.py` warnings in the product repo's governance workflow.
- **Exit:** all lanes on FLOW for one full week with the pilot signals holding.

### M4 — Decommission (week 6)
- M4.1 Move `docs/governance/`, `docs/context/` (except content migrated into packs) and `docs/application-lifecycle-bible/` to `docs/archive/aigem-1.5/` in the product repo with a tombstone README: *"Superseded by FLOW on <date>, DEC-MIG-01. Read-only historical evidence."*
- M4.2 Keep every register, CR, verdict and evidence file byte-identical in the archive — they are audit evidence of decisions already taken.
- M4.3 Remove AIGEM CI jobs that enforce process (BOOT drift, capsule budgets, freshness halt). Keep jobs that enforce guardrails.
- M4.4 First monthly governance retro under FLOW.
- **Exit:** product repo boots agents from FLOW only; archive intact; retro held.

## 4. Rollback

At any phase: repoint the product repo's `AGENTS.md` / `CLAUDE.md` back to AIGEM and restore the
two skills. Nothing in M0–M3 deletes or rewrites AIGEM files, and M4 only moves them. Decisions
taken under FLOW remain valid decisions — they were taken by the accountable humans either way.

## 5. What to do on day one, whichever way H1 goes

These unblock flow under **either** model — CR-016 already permits most of them:

1. Start `platform` IaC (O-PL-1) now; GATE-S08 criteria are all met.
2. Start GATE-P4 4.2 (publish OpenAPI), 4.4 (compliance review of audit schema) and 4.5 (runbook) in parallel — none depends on another.
3. Start WS-2 A.1–A.6 — none depends on the AD technology decision.
4. Hold a first 15-minute decision clinic on the five oldest open decisions.
5. Chase the four external dependencies whose 2026-09-18 follow-up dates have passed.

## 6. Files in this folder

| File | Purpose |
|---|---|
| [`concept-map.md`](./concept-map.md) | Every AIGEM concept → keep / replace / retire, and where it went |
| [`artefact-disposition.md`](./artefact-disposition.md) | Every AIGEM folder/file group → migrate / archive / retire |
| [`carry-over.md`](./carry-over.md) | Open gates, criteria, parked items, debt and risks → their new homes |
