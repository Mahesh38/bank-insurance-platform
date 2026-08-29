# Board 5 — QA · Position on CR-014

**Change request:** [CR-014](../../CR-014-gitlab-estate-migration.md) · **Plan:** [`GLM-001`](../../../../platform/gitlab-migration/GLM-001-migration-plan.md)
**Board:** 5 — QA (**quality hold authority** `B`) · **Role:** `R7`
**Persona:** Swapnali — Principal Quality Engineering / QA Lead
**Reviewer type:** `AGENT` · **Self-review:** false · **Change tier:** `T4` · **Date:** 2026-08-29

> **`signature_status: AI-DRAFTED`**

> ## Drafted verdict: `APPROVE-WITH-CONDITIONS`, with a `Q1` evidence hold
> **Not `Q0`.** A `Q0` hold would stop the migration, and the migration is not a quality risk — the
> *claim that evidence survived it* is. My conditions attach to the evidence, not to the move.

---

## 1. The one sentence this review exists to enforce

> **Unexecuted is not passed.**

The cutover converts four executed criteria into unexecuted ones. `S08-G1` requires evidence tier
`E4` — a run history — and a run history is not a portable artefact. `S08-G2`, `S08-G5` and `S08-G9`
are in the same position for the reasons Amit, Deepali and Shivanshi each set out.

I am flagging in advance the argument I expect to be made, because it will be made by people acting
in good faith under schedule pressure: *"the pipeline is the same, the configuration is ported,
therefore the evidence carries over."* It does not. A ported configuration is a **hypothesis** that
the control still works. Evidence is the execution that confirms it. On IMP-3 the configuration is
not even ported — the SAST engine is different — so the hypothesis is weaker still.

---

## 2. Findings

### `QA-F01` · configuration review is not evidence, and `S08-G2` is where it will be attempted

`S08-G2` is *merge to main impossible without a green pipeline*. After IMP-4 the mechanism changes
shape entirely: named required checks become one required pipeline with `allow_failure: false`
gating jobs.

You cannot evidence that by reading settings. The only evidence that a merge is impossible is a
merge that was impossible. **`C-QA-2`:** a deliberately failing gating job on a throwaway merge
request, demonstrated to block the merge, with the attempt recorded. Then the same job fixed, and
the merge proceeding.

That is one hour of work and it is the difference between a gate and a belief about a gate.

### `QA-F02` · `S08-G1` needs a run history, and I will name the minimum

"Run history" invites an argument about how much. My minimum, and I will accept it as sufficient:

- at least one full green pipeline on `main` in the new estate, building and testing **every**
  module;
- at least one pipeline triggered by a merge request, showing the same;
- at least one **red** pipeline demonstrating a real failure is caught, not merely reported;
- the affected-component path exercised at least once and shown to still run every gating job
  (`C-ENG-3`).

Fewer than four runs is not a history. I am not asking for weeks of it.

### `QA-F03` · the SAST re-baseline needs a QA position, not only a security one

Deepali's `C-SEC-3` requires a differential CodeQL/GitLab-SAST run and I support it. My addition is
about what happens to the delta afterwards.

A CodeQL-only finding that is named and owned is handled. A CodeQL-only finding that is named and
then *absorbed into a backlog with no criterion attached* is a control that quietly stopped
existing. **`C-QA-3`:** every CodeQL-only finding either gets a corresponding GitLab-SAST rule, or a
work item with an acceptance criterion, or an explicit Deepali-signed acceptance. Not a list.

### `QA-F04` · the split is verifiable, so verify it rather than eyeballing it

GLM-001 M2.7 and M5.10 verify the split. "History preserved" is not checkable by looking at a commit
graph. It is checkable exactly: commit count, author and committer dates, tree hash of the final
tree against the corresponding subtree of the origin, and tag/branch presence.

**`C-QA-1`:** the split verification is a script producing a pass/fail artefact, run against the real
push and retained. If it is a human confirming it looks right, it is not evidence and I will not
accept it as such.

### `QA-F05` · `QA-001` and `S08-G3` must not become migration casualties

`S08-G3` (coverage thresholds enforced; `QA-001` closed) is already open and already mine. It is
**not** one of the four criteria this migration re-opens — coverage verification is a Gradle task,
not a platform feature — and I want that recorded now, because in six weeks "the migration" will be
an available explanation for every criterion still open.

Amit's `C-ENG-2` protects the mechanism. The remaining work on `QA-001` is unaffected by CR-014 and
its date does not move because of it.

---

## 3. Conditions

| ID | Condition | Must be true before |
|---|---|---|
| `C-QA-1` | Split verification is a script emitting a pass/fail artefact — commit counts, authorship, dates, tree hashes, branch and tag presence — run against the real push and retained | Freeze lifts |
| `C-QA-2` | `S08-G2` demonstrated negatively: a failing gating job shown to block a real merge request, recorded, then fixed and the merge shown to proceed | `S08-G2` claimed |
| `C-QA-3` | Every CodeQL-only finding from `C-SEC-3` resolved into a GitLab-SAST rule, a work item with an acceptance criterion, or a Deepali-signed acceptance | `S08-G5` claimed |
| `C-QA-4` | `S08-G1` evidenced by the four runs at `QA-F02`, not by configuration equivalence | `S08-G1` claimed |
| `C-QA-5` | No `GATE-S08` criterion is recorded as satisfied by an argument of the form "ported, therefore equivalent" | Continuous |

---

## 4. What I am not deciding

- **Not** whether to migrate, or when — Delivery convenes, boards decide.
- **Not** the security conclusion. I verify controls; Deepali concludes on them.
- **Not** the operational conclusion. I verify behaviour; Shivanshi concludes on it.
- **Not** a coverage waiver. `QA-001` stands and CR-014 does not touch it.
- I will **not** record any criterion as passed on unexecuted evidence, under any schedule.
