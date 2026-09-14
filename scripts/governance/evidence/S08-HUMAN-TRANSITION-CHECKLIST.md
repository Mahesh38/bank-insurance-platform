# S08 → S09 human transition checklist

> Agent-assembled handoff. **Does not** mark `GATE-S08` PASSED, edit `stage_status` /
> `current_phase`, or record board / T4 approvals. Those steps are human-only
> ([`04-STAGE_GATES.md` §5](../../../docs/governance/04-STAGE_GATES.md)).

Generated: 2026-09-14  
Autopilot: `proposal_type=STAGE_TRANSITION_CANDIDATE` · `may_mark_passed=false`  
Target: **S09 — Platform & Environment Foundation**  
Evidence PR (G2+G10): https://github.com/Mahesh38/bank-insurance-platform/pull/106 (merged)

## Status now

| Item | State |
|---|---|
| Exit criteria S08-G1…G10 | **10/10 MET** |
| `GATE-S08` | still **OPEN** |
| Stage fields (`current_phase` / `stage_status`) | unchanged (human only) |
| Board / T4 signatures | **missing** — Amit / Engineering, Swapnali / QA, Mahesh / Architecture, Deepali / Security, Shivanshi / SRE |

## What you (humans) do, in order

1. **Review evidence** in `scripts/governance/evidence/S08-STAGE_TRANSITION_CANDIDATE.yaml` and the criterion artefacts under `scripts/governance/evidence/S08-*`.
2. **Each required approver** records a named HUMAN verdict on the draft sign-off (`docs/application-lifecycle-bible/stages/signoffs/S08-GATE-SIGNOFF-DRAFT.md`):
   - Amit / Engineering
   - Swapnali / QA
   - Mahesh / Architecture
   - Deepali / Security
   - Shivanshi / SRE (Operations)
3. **Architect + PO jointly** mark the gate **PASSED** and update `docs/governance/state/CURRENT-STATE.yaml`:
   - `GATE-S08` → `PASSED`
   - `current_phase` / `stage_status` for the S08 → S09 advance
   - `next_stage` as appropriate for post-S09 work
4. Rename the draft sign-off to a dated file under `docs/application-lifecycle-bible/stages/signoffs/` and fill `outcome.decision: PASSED` with real `passed_by` names.
5. Record the transition in the decision register.
6. **Run the unpark sweep** (re-triage items whose `unpark_trigger` matches S08 PASS / entry to S09 — do not auto-admit).
7. Start S09 work (IaC, environments, secrets management) under the Foundation Recovery Increment already named in CURRENT-STATE.

## Explicitly out of agent authority

- Marking `GATE-S08` PASSED
- Editing `stage_status` / `current_phase`
- Fabricating T4 / board sign-off (`reviewer_type: HUMAN` cannot be satisfied by an agent)

## After humans close S08

Safe next agent work (once state shows S09 / GATE-S08 PASSED): pick S09 exit criteria via `python3 scripts/governance/autopilot.py next` and implement platform/environment foundation items only.

## Note on WS-1 Phase 5

Rajal condition C6 still applies: Phase 5 LOB expansion does **not** start until **GATE-S08 and GATE-S11** are both PASSED. Closing S08 alone does not authorise Phase 5.
