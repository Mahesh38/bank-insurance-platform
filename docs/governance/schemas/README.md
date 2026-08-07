# Schemas

JSON Schema (draft 2020-12) definitions for every AIGEM artefact. They exist so a record can be
**validated**, not just written — the conditional rules encode the framework's non-negotiables
directly, which is what stops the model degrading into prose nobody checks.

| Schema | Validates | Key enforced rules |
|--------|-----------|--------------------|
| [triage-record.schema.json](./triage-record.schema.json) | Pipeline output per input | SF3 requires `target_stage` + `unpark_trigger` · SC1 requires `serves` · NOT-NOW requires `future_necessity` + `target_stage` · MUST requires E1–E5 evidence · P1 overrides require evidence · EXTERNAL edges require owner + date · REJECT requires `reopen_if` |
| [work-item.schema.json](./work-item.schema.json) | Story / task / bug / spike / epic | BUG requires `violates` + a failing-first regression test · DEBT requires severity, owner, expiry · EPIC requires `completion_definition` + `not_included` + two triggers · PARKED requires target + trigger · READY requires owner + AC |
| [implementation-plan.schema.json](./implementation-plan.schema.json) | Plans submitted to the board | `files_expected` non-empty · `out_of_scope` non-empty · rollback, impact fields, and AC mandatory |
| [review-verdict.schema.json](./review-verdict.schema.json) | One board's verdict | Approval requires non-empty `evidence[]` (Rule RG-3) · REWORK requires `must_fix[]` · NOT_APPLICABLE requires a reason · rounds capped at 2 |
| [current-state.schema.json](./current-state.schema.json) | `../state/CURRENT-STATE.yaml` | Workstreams need lifecycle, objective, and scope; gates need criteria with states |

---

## Validating

The records are written as YAML for readability; JSON Schema validates them after a YAML load.

```bash
pip install check-jsonschema

# current state
check-jsonschema --schemafile docs/governance/schemas/current-state.schema.json \
  docs/governance/state/CURRENT-STATE.yaml

# a triage record extracted to YAML
check-jsonschema --schemafile docs/governance/schemas/triage-record.schema.json \
  /tmp/SUG-0001.yaml
```

`implementation-plan.schema.json` references `review-verdict.schema.json` by relative `$id`;
resolve both from this directory when validating plans with embedded verdicts.

## Extending

Adding a rule to the framework means adding it here too, or it is advice rather than a rule.
Schema changes are `GOV` work and follow [14-CHANGE_CONTROL.md](../14-CHANGE_CONTROL.md).
