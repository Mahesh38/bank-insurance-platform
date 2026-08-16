# Epic — S<xx>-E<nn> <Title>

```yaml
epic:
  id: S08-E01
  title: "Continuous integration pipeline"
  stage: S08
  aigem_stage: "L4 — Foundation"
  workstream: WS-3
  owner_persona: "Amit / Engineering"
  supporting_personas: ["Shivanshi / SRE", "Swapnali / QA"]
  priority: P1
  status: NOT_STARTED     # NOT_STARTED | IN_PROGRESS | BLOCKED | DONE | WITHDRAWN
```

## 1. Outcome

One paragraph: what is true when this epic is done that is not true now. Written as an outcome,
not a task list — "every pull request is built and tested automatically, and a failing change
cannot be merged", not "set up GitHub Actions".

## 2. Why now

Which stage does this serve, and why is it on-stage rather than premature? If it belongs to a
later stage, it should be parked, not written up as an epic.

## 3. Business or regulatory justification

- Requirement, obligation, gap or debt IDs this traces to
- If this is foundation work with no direct business output, say so plainly and name what it
  unblocks. Foundation epics that dress themselves as features get cut first; ones that state
  their dependency chain survive.

## 4. Stories

| ID | Story | Acceptance criteria | Size | Status |
|---|---|---|---|---|
| S08-E01-S01 | | | M | |

## 5. Definition of Done for this epic

Beyond every story being done:

- [ ] The stage's related validation tests pass
- [ ] Evidence produced at the level the gate criterion requires
- [ ] Documentation updated where behaviour or process changed
- [ ] No new tech debt introduced without a ledger entry

## 6. Dependencies

| Depends on | Type | Status | Owner |
|---|---|---|---|
| | prerequisite / external / decision | | |

## 7. Out of scope

What a reasonable person might assume is included and is not — with where it goes instead.
This section prevents the epic absorbing adjacent work, which is the most common way a
foundation epic becomes a quarter.

## 8. Review tier and impact

```yaml
review_tier: T3                 # T1 | T2 | T3 | T4 — see 11-REVIEW_GATES §3
security_impact: medium         # none | low | medium | high
compliance_impact: none
operational_impact: high
t4_triggers_considered: []      # if T3 on an ambiguous call, name the triggers and why they did not fire
```

## 9. Evidence this epic must produce

| Gate criterion served | Evidence level | Artefact |
|---|---|---|
| S08-G1 | E4 | Pipeline definition + run history |
