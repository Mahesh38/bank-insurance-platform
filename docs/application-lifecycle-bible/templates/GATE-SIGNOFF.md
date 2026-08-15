# Gate Sign-off — S<xx> <Stage Name>

> Copy to `docs/application-lifecycle-bible/stages/signoffs/S<xx>-GATE-SIGNOFF-<YYYY-MM-DD>.md`.
> One per gate transition. This is the artefact a regulator or an auditor reads.

```yaml
gate_signoff:
  stage: S08
  gate_id: GATE-S08
  transition: "S08 → PASSED"          # or "→ OPEN (rework)" / "→ BLOCKED"
  date: 2026-10-15

  marked_candidate_by: "Kalpana / Delivery"
  candidate_date: 2026-10-08
  freeze_in_effect: true               # only SF0 and P1-override admitted since candidate_date
```

## 1. Evidence table

Every criterion. Every one gets an artefact someone else can open — Rule GS-1.

| # | Criterion | Required level | Evidence (link) | State | Verified by |
|---|---|---|---|---|---|
| S08-G1 | | E4 | | MET / NOT MET / WAIVED | |
| S08-G2 | | E4 | | | |

**States:** `MET` · `NOT MET` (names the blocking item) · `WAIVED` (requires §4)

## 2. Board verdicts

One block per approving persona. `APPROVED` with an empty `evidence[]` is recorded as `NOT_RUN`
— Rule RG-3.

```yaml
approvals:
  - persona: "Mahesh / Architecture"
    board: ARCHITECTURE
    reviewer_type: HUMAN            # HUMAN | AGENT
    self_review: false
    decision: APPROVED              # APPROVED | APPROVED_WITH_CONDITIONS | REWORK | REJECTED | NOT_APPLICABLE
    must_fix: []
    conditions: []
    evidence:
      - "checked A1–A10 against the pipeline definition and the module graph"
    date: 2026-10-14

  - persona: "Deepali / Security"
    board: SECURITY
    reviewer_type: HUMAN            # mandatory HUMAN at T4 — no AI substitution
    decision: APPROVED_WITH_CONDITIONS
    conditions:
      - "SAST baseline findings triaged into the risk register by 2026-10-31"
    evidence:
      - "reviewed S1–S12 against the scanning stages and their failure thresholds"
    date: 2026-10-14
```

Personas required for this stage are listed in the stage file's *Approvers* line and in
[`../04-GATE-AND-SIGNOFF-MODEL.md §5`](../04-GATE-AND-SIGNOFF-MODEL.md).

Record `NO_RESPONSE` against any board that did not reply within its window. `NO_RESPONSE` never
counts toward the gate and never satisfies a mandatory sign-off — it puts a name and a date
against the stop.

## 3. Conditions carried forward

Conditions become acceptance criteria and are tracked to closure — Rule GS-4.

| Condition | From board | Owner | Backlog ID | Due |
|---|---|---|---|---|
| | | | | |

## 4. Waivers

Only if a criterion is `WAIVED`. All six fields are required, and the non-waivable list in
[`../07-SECURITY-COMPLIANCE-CANON.md`](../07-SECURITY-COMPLIANCE-CANON.md) cannot be waived by
any authority.

| Criterion | What is not met | Risk accepted | Compensating control | Risk owner (named human) | Expiry | Remediation ID |
|---|---|---|---|---|---|---|
| | | | | | | |

## 5. Outcome

```yaml
outcome:
  decision: PASSED                  # PASSED | OPEN | BLOCKED
  blocking_items: []                # if OPEN or BLOCKED — with IDs and priorities
  passed_by: ["Mahesh / Architecture", "Rajal / Product"]   # joint, human only

  post_transition_actions:
    current_state_updated: false    # docs/governance/state/CURRENT-STATE.yaml — human only
    unpark_sweep_run: false         # parked items RE-TRIAGED, never auto-admitted
    decision_register_entry: ""     # registers/DECISION-REGISTER.md reference
    position_banner_updated: false  # docs/application-lifecycle-bible/README.md

  parked_items_released: []
  next_stage: S09
```

## 6. Notes

Anything a future reader needs that the tables above do not carry: why a criterion was
interpreted a particular way, what nearly blocked the gate, what the boards disagreed about.

A gate that passed unanimously with no discussion is worth a sentence explaining why it was
straightforward — an empty notes section on a significant transition usually means the review was
thin, not that the work was clean.
