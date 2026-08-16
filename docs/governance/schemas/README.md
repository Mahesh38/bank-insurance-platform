# Schemas

JSON Schema (draft 2020-12) definitions for every AIGEM artefact. They exist so a record can be
**validated**, not just written — the conditional rules encode the framework's non-negotiables
directly, which is what stops the model degrading into prose nobody checks.

| Schema | Validates | Key enforced rules |
|--------|-----------|--------------------|
| [triage-record.schema.json](./triage-record.schema.json) | Pipeline output per input | SF3 requires a non-empty `target_stage` + `unpark_trigger` · SC1 requires `serves` · NOT-NOW requires `future_necessity` + `target_stage` · MUST requires E1–E5 evidence · P1 overrides require evidence · EXTERNAL edges require owner + date · REJECT requires `reopen_if` |
| [work-item.schema.json](./work-item.schema.json) | Story / task / bug / spike / epic | BUG requires `violates` + a failing-first regression test · DEBT requires severity, owner, expiry · EPIC requires `completion_definition` + `not_included` + two triggers · PARKED requires target + trigger · READY requires owner + AC. **Every conditionally-required field is also constrained non-null and non-empty in that branch** |
| [implementation-plan.schema.json](./implementation-plan.schema.json) | Plans submitted to the board | `files_expected` non-empty · `out_of_scope` non-empty · rollback, impact fields and AC mandatory · embedded `reviews[]` validated against the inlined verdict definition |
| [review-verdict.schema.json](./review-verdict.schema.json) | One board's verdict | Approval requires non-empty `evidence[]` (Rule RG-3) · REWORK requires `must_fix[]` · NOT_APPLICABLE requires a reason · rounds capped at 2 |
| [approval-gate.schema.json](./approval-gate.schema.json) | The **aggregated** board outcome | Mandatory boards per risk tier · APPROVED forbids any REWORK/REJECTED verdict, so a Security veto cannot be out-voted · T4 requires **human** Security and Risk & Compliance verdicts plus a recorded sign-off · T3 requires at least one human board · board conditions must be folded into acceptance criteria before the gate closes |
| [current-state.schema.json](./current-state.schema.json) | `../state/CURRENT-STATE.yaml` | Workstreams need lifecycle, objective and scope; gates need criteria with states; routing is workstream-aware and maps every canonical type to non-empty destinations |
| [gate-evidence.schema.json](./gate-evidence.schema.json) | `../state/GATE-EVIDENCE.yaml` | Criteria expose owner, execution mode, priority, effort, evidence, verifier and blockers; policy is proposal-only; silence and automatic waivers are forbidden; approvals remain separate from evidence |

---

## Validating

Records are written as YAML for readability and validated after a YAML load.

```bash
# everything at once — schemas, state, tagged template blocks, links, calibration
python3 scripts/governance/ci-checks.py

# a single record
pip install check-jsonschema
check-jsonschema --schemafile docs/governance/schemas/current-state.schema.json \
  docs/governance/state/CURRENT-STATE.yaml
```

`ci-checks.py` runs in CI ([.github/workflows/governance.yml](../../../.github/workflows/governance.yml)).
It is **not** on an agent's critical path — the mandatory agent check is
`java scripts/governance/FreshnessCheck.java`, which needs nothing beyond the documented
JDK 21 + Git baseline.

## The extractor contract

A fenced YAML block in any governance document whose **first line** is `# schema: <name>` is
extracted and validated against `schemas/<name>.schema.json`:

````markdown
```yaml
# schema: triage-record
id: SUG-20260812-a1b
...
```
````

This is what keeps the templates and the schemas from drifting apart. Blocks without the marker
— short forms, checklist stubs, illustrative fragments — are ignored by design. Records are
written at the **document root**, with no wrapper key, so a copied template validates as-is.

## Cross-schema references

`implementation-plan.schema.json` inlines the verdict definition under
`$defs.reviewVerdict` rather than `$ref`-ing the standalone file: the absolute `$id` makes a
relative `$ref` unresolvable offline, which silently disabled validation of embedded reviews.
Check 4 in `ci-checks.py` asserts the inlined copy stays identical to
`review-verdict.schema.json`, so the duplication cannot drift.

## Extending

Adding a rule to the framework means adding it here too, or it is advice rather than a rule.
Schema changes are `GOV` work and follow [14-CHANGE_CONTROL.md](../14-CHANGE_CONTROL.md).
