# Backlog — generated artefacts

## What is here

| File | Content |
|---|---|
| `BACKLOG.yaml` | Machine-readable stages, epics, stories, validation tests and gate criteria |
| `jira-import.csv` | Jira CSV import covering the same set |

**Both are generated. Do not edit them by hand** — the next regeneration overwrites your change.

## Source of truth

The stage markdown under [`../stages/`](../stages/) is authoritative for **structure**: which
stages, epics, stories, tests and gate criteria exist, and what their acceptance criteria say.

Jira is authoritative for **state**: what is in progress, who owns it, what is blocked, what is
done.

> Do not attempt to sync state back into the YAML. Two-way sync between a document and a tracker
> produces two wrong answers and an argument about which is right.

## Regenerating

```bash
python3 scripts/lifecycle/generate-backlog.py
```

Requires Python 3.10+ and no third-party packages.

Run it after any structural change to a stage file — a new epic, a new story, a changed acceptance
criterion, a new gate criterion — and commit the regenerated files alongside the stage edit.

## Current contents

| Item | Count |
|---|---|
| Stages | 16 |
| Epics | 93 |
| Stories | 447 |
| Validation tests | 141 |
| Gate criteria | 143 |
| CSV rows | 824 |

## Importing into Jira

1. **Settings → System → External System Import → CSV**
2. Upload `jira-import.csv`
3. Map columns:

| CSV column | Jira field |
|---|---|
| `Issue Key` | *(do not map — reference only; Jira mints its own keys)* |
| `Summary` | Summary |
| `Issue Type` | Issue Type |
| `Fix Version` | Fix Version/s |
| `Epic Link` | Epic Link |
| `Epic Name` | Epic Name |
| `Priority` | Priority |
| `Assignee Persona` | custom field (text) |
| `Labels` | Labels |
| `Acceptance Criteria` | custom field (text) |
| `Evidence Required` | custom field (text) |
| `Description` | Description |

4. **Import Epics before Stories** so `Epic Link` resolves. The CSV is already ordered that way
   per stage, but if you import in slices, keep epics ahead of their stories.

### A note on keys

`Issue Key` in the CSV is the **lifecycle ID** (`S08-E01-S02`), not a Jira key. Keep it in the
summary — that is what makes traceability queries work, and what lets a regulator's question
about a specific control be answered years later.

## Priorities

Priorities in the CSV are **stage-level defaults** derived from the realignment sequencing:
foundation and compliance stages (S02, S08, S09, S11) default to P1, the rest to P2 or P3.

They are a starting point. Product owns priority, and per-item priority is set in Jira after
AIGEM triage — never inherited unexamined from this file.

## Before importing

This framework is **proposed, not ratified**. Adoption requires CR-010 with verdicts from Boards
1–7 ([`../README.md`](../README.md)). Importing 824 issues into a live Jira project before that
ratification would create a parallel backlog competing with the existing one.

Recommended order: ratify CR-010 → register WS-3 → import the stages in scope for the Foundation
Recovery Increment (S08 and S09) → import the rest as those stages are entered.
