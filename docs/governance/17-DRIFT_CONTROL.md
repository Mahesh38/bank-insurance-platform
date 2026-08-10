# 17 — Drift Control: Detection and Return

**Layer:** L1 — generic
**Owner:** Every agent, continuously

---

## 1. What drift is

**Drift** is the gap between the approved plan and what is actually being built. It is rarely a
single decision to go off-task; it is a sequence of individually reasonable steps:

```text
Fix the payment status mapping
  → notice the mapper is duplicated
    → extract a shared mapper
      → the shared mapper needs a common enum
        → the enum belongs in bank-common-error
          → while in that lib, tidy the error codes
            → 400 lines changed in a module nobody reviewed for this item
```

Every arrow is defensible. The destination is not. Drift control exists because agents are
*especially* prone to this pattern: each step is locally optimal, and nothing in the local
context says stop.

---

## 2. Drift signals

Checked continuously during implementation. Each is a **stop-and-classify** trigger, not
necessarily an error.

| # | Signal | Severity |
|---|--------|----------|
| D1 | Editing a file not in `files_expected` | Low — variance or drift |
| D2 | Editing a component not in `affected_components` | **High** |
| D3 | Adding a library, service, or infrastructure dependency | **High** |
| D4 | Touching anything named in the plan's `out_of_scope` | **Critical** |
| D5 | Changing a public contract, schema, or error code not in the plan | **Critical** |
| D6 | Diff size materially exceeding the estimate (> 2×) | Medium |
| D7 | Creating an abstraction with one implementation | Medium — fails X2 |
| D8 | Implementing something raised as a suggestion this session | **High** |
| D9 | Refactoring code you are only reading | Medium |
| D10 | Changes with no traceable acceptance criterion | **High** |
| D11 | Fixing a second, unrelated defect found en route | Medium |
| D12 | "While I'm here…" appearing in your own reasoning | **Treat as a hard stop** |
| D13 | Adding config, flags, or env vars not in the plan | Medium |
| D14 | Changing tests to make them pass rather than to reflect new behaviour | **Critical** |

D12 is not a joke. That phrase — or the thought behind it — reliably precedes the worst drift,
and it is easy to detect in your own output.

---

## 3. Checkpoints

| When | Check |
|------|-------|
| **Before each edit** | Is this file in `files_expected`? Which AC does this edit serve? |
| **Before adding a dependency** | Is it in the plan? If not: stop, this needs Architecture |
| **Every ~10 edits or 30 minutes** | Re-read the objective and `out_of_scope`. Still on it? |
| **Before each commit** | Does the diff map to the plan and to specific AC? |
| **Before opening a PR** | Full drift check (§5) |
| **On finishing** | Variance log complete; suggestions registered |

---

## 4. Classification and response

When a signal fires:

```text
STOP. Do not continue the edit.

Classify:
 ├─ NECESSARY-INSIDE — required to satisfy an AC of the current item, inside the
 │   approved components
 │      → Log a variance in the plan (10 §6). Continue.
 │
 ├─ NECESSARY-OUTSIDE — the current item cannot be completed without it, but it
 │   crosses a component, contract, or out_of_scope boundary
 │      → Re-review by the affected boards (14 §4) BEFORE continuing.
 │        If review is not available now: revert the excursion, mark the item
 │        BLOCKED, name the blocker. Do not "proceed and ask later".
 │
 ├─ ADJACENT-VALUE — genuinely useful, not required for this item
 │      → REVERT it. Write SUG-####. Continue the original item.
 │
 └─ INCIDENTAL — formatting, unrelated tidy-up, opportunistic rename
        → REVERT. No register entry needed unless it recurs.
```

> **Rule DC-1 — When in doubt, revert and register.** Reverting costs minutes. An unreviewed
> cross-boundary change costs a review cycle, a merge conflict, or a production incident.

> **Rule DC-2 — Drift is never resolved by widening the plan after the fact.** Editing
> `files_expected` to match what you already changed is not a variance log; it is a cover-up,
> and it destroys the only measurement of plan accuracy the model has.

---

## 5. Pre-PR drift check

```text
[ ] Every changed file is in files_expected, or has a variance log entry
[ ] No component outside affected_components is touched
[ ] Nothing in out_of_scope is touched
[ ] No new dependency that is not in the plan
[ ] Every change traces to an acceptance criterion
[ ] Diff size is within ~2× the estimate, or the excess is explained
[ ] Suggestions raised during implementation are in the register, not in the diff
[ ] No TODO without a work item ID
[ ] Tests changed only to reflect intended behaviour changes
```

Mechanical support:

```bash
# files changed vs the plan
git diff --name-only origin/main...HEAD

# unreferenced TODOs (should return nothing)
git diff origin/main...HEAD | grep -nE '^\+.*(TODO|FIXME|HACK)' | grep -vE '(TD|FUNC|NFR|QA|SUG|COMP|TECH|SEC|OPS)-[0-9]+'

# new dependencies
git diff origin/main...HEAD -- '*build.gradle.kts' 'gradle/libs.versions.toml'
```

---

## 6. Return protocol

When drift has already happened and you notice late:

```text
1. STOP. Do not add "one more fix" to make the excursion coherent.
2. SNAPSHOT. Note what has changed and why, honestly.
3. SEPARATE. Split the change:
     - part that serves the original item's AC  → keep
     - everything else                          → extract or revert
   `git stash`, a scratch branch, or a patch file all work. Losing the work is
   not required — removing it from *this* item is.
4. REGISTER. Each extracted piece becomes a SUG-#### and goes through triage.
5. RE-ANCHOR. Re-read the objective and acceptance criteria out loud (in your output).
6. RESUME from the last plan step that was actually completed.
7. REPORT. One line to the user: what drifted, what was reverted, what was registered.
   Never hide drift — the user is the one who can tell you whether the excursion
   mattered more than the item.
```

Reporting is mandatory and is not punished. An agent that reports drift is behaving correctly;
an agent that quietly ships a 400-line diff for a 40-line story is not.

---

## 7. Prevention

| Habit | Effect |
|-------|--------|
| Write `out_of_scope` *before* starting, listing the three most tempting adjacent changes | Names the traps in advance, when judgement is uncontaminated by momentum |
| Keep `files_expected` open while working | Makes D1 immediate rather than retrospective |
| Batch suggestions into one register write at the end | Removes the urge to fix now to avoid forgetting |
| Commit per acceptance criterion | Makes untraceable changes visible in the history |
| Say the objective back before each work session | Cheap re-anchoring |
| Treat "while I'm here" as a stop word | Catches D12 at the thought, not the diff |

---

## 8. Scope-recovery phrases

Ready-made responses for the moments where drift begins. Use them verbatim:

| Situation | Say |
|-----------|-----|
| You spot an improvement mid-task | "Noted as SUG-0044 (parked, Phase 5). Continuing FUNC-011." |
| The user suggests something adjacent mid-task | "That's a valid change — triaging it as SUG-0045. It's SF2 for the current stage, so I'd finish FUNC-011 first unless you want to switch." |
| You've already drifted | "I drifted: I changed `bank-common-error` while fixing the status mapper. Reverting that, registering it as SUG-0046, and finishing the mapper fix." |
| A P1 genuinely interrupts | "Interrupting FUNC-011 for a P1 (O2: reachable vulnerability in the payment path). Snapshot recorded; returning after." |
| An item turns out to be bigger than planned | "This is larger than the plan: it needs a second component. Stopping to re-review rather than expanding scope." |
| You are asked to skip the process | "Understood — doing it directly. Recording the bypass and the one risk it carries: no Security board on an auth-path change." |

---

## 9. Drift metrics

| Metric | Definition | Target |
|--------|------------|--------|
| Drift incidents / PR | Signals that reached NECESSARY-OUTSIDE or worse | < 0.5 |
| Plan accuracy | Files changed ∩ `files_expected` ÷ files changed | > 0.85 |
| Variance rate | Variance log entries per plan | < 2 |
| Reverted excursions | Count per stage | Falling |
| Suggestions registered per implementation session | Count | > 0 is **healthy** — it means ideas are captured, not acted on |

That last metric is worth reading twice: **zero registered suggestions is a warning sign**,
not a success. Agents that never notice anything are either not looking, or are quietly acting
on what they see.
