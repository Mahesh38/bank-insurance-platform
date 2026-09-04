# DEC-20260829-01 — Phase M0 decisions: gate evidence, governance-tree home, Render disposition

**Status:** **`APPROVED` 2026-08-29** — board outcome relayed by `human:Mahesh` and recorded. Supersedes the `AI-DRAFTED` state of this file.
**Recorded by:** `agent:claude`. The agent recorded a decision the owner gave; it did not supply one (Rule CC-1, Rule PA-1).
**Date:** 2026-08-29
**Workstream:** WS-3 — AU Bank Insurance Distribution Platform
**Stage:** S08 — Engineering Foundation (`GATE-S08` `OPEN`, 10 of 10 criteria open), S09 overlapped
**Origin:** human:Mahesh — *accept the improvements, activate every persona, start Phase M0, take the decisions with mutual discussion*
**Scope CR:** [`CR-014`](./change-requests/CR-014-gitlab-estate-migration.md) · **Parallel CR:** [`CR-015`](./change-requests/CR-015-shared-persistence-vs-bank-baseline.md)
**Plan:** [`GLM-001`](../platform/gitlab-migration/GLM-001-migration-plan.md) · **ADR:** `ADR-020`
**Board positions:** [`CR-014/verdicts/`](./change-requests/CR-014/verdicts/README.md)
**Freshness:** `state_as_of` is 19 days old — `FreshnessCheck` exit `1` (WARN). Review due 2026-09-09.

---

## 1. What this file is, and is not

**Is:** the record of the three Phase M0 decisions, the cross-persona discussion that produced them,
and the two findings that discussion surfaced which `GLM-001` did not contain.

**Is not:** a human T4 Architecture, Security or Risk & Compliance signature · an approval of
`CR-014` · a waiver of any `GATE-S08` criterion · the bank's Appendix C exception · authority to run
`terraform apply` against the bank control plane.

Every decision below carried a **status** when this file was drafted: two `RECOMMENDED`, one
`BLOCKED-EXTERNAL`, none `APPROVED`. **On 2026-08-29 the boards approved all three**, and the
statuses below are updated accordingly. The reasoning is left exactly as it was drafted *before* the
outcome was known — it is the record of how the decision was reached, not a reconstruction after it.

---

## 2. M0.3 — Gate-evidence strategy

> **Question.** `GATE-S08` has ten open criteria. Four — `S08-G1`, `G2`, `G5`, `G9` — are evidenced
> by the CI platform itself and are re-opened by the cutover, because GitHub Actions run history does
> not migrate. Do we close them on GitHub first, or re-evidence them on GitLab?
>
> **Owner.** Amit (`R3` Engineering) with the affected boards. Kalpana convenes; she may not decide.

### 2.1 The discussion

**Kalpana** opened it as `DL1` with a required-by date of 2026-09-02, and stated the cost of delay
plainly: every day this stays open is a day of GitHub Actions investment that may be discarded. She
declined to express a preference — Rule PA-1.

**Amit** gave the engineering input. Four criteria re-open regardless of which option is chosen, so
Option A does not avoid the rebuild — it buys a closed gate on a platform we are leaving, then
rebuilds all of it within weeks. He would rather spend the effort once. He also named Option B's
honest cost: `GATE-S08` stays open across the migration and nobody should be surprised by that in six
weeks.

**Swapnali** did not take a side on timing and constrained both options identically: neither may
record a criterion as satisfied by *"ported, therefore equivalent"*. She then made Option A less
attractive without arguing against it — closing `S08-G1` on GitHub produces evidence about
infrastructure the bank does not own, which she would have to re-collect anyway to satisfy the same
criterion in the target estate. Her four-run minimum for `S08-G1` (`QA-F02`) applies to whichever
platform ends up carrying the evidence.

**Deepali** noted that `S08-G5` cannot be closed on GitHub in a way that survives at all: the SAST
engine changes, so the control changes. Closing it twice is not duplicated effort, it is two
different controls.

**Shivanshi** observed that `S08-G9` will likely be *worse* on first measurement on bank runners
(`OPS-F05`), and that closing it on GitHub first would set a baseline we then visibly regress
against — which is a worse conversation than measuring it once, honestly, in the target estate.

**Mahesh** had no architecture position. Gate sequencing is not an architecture judgement and he
declined to supply one.

**Shailja** noted only that neither option changes any regulatory obligation, and that `CMP-F01`
(§5) can invalidate the destination entirely — in which case this decision is premature rather than
wrong.

### 2.2 Decision

> **Option B — re-evidence on GitLab.** Stop investing in GitHub Actions evidence beyond keeping the
> build green. `GATE-S08` stays open across the migration window and is reported open.
>
> **Status: `APPROVED` 2026-08-29.** Selected by the boards as `AC-1`, with one addition to the
> drafted wording: **GitHub Actions is kept green for rollback continuity only.** `GATE-S08` remains
> `OPEN` throughout the migration.

**Consequences, accepted deliberately:**

1. `GATE-S08` will be open for 6–9 weeks longer than it otherwise might have appeared to be.
   Kalpana reports it open and will not mark it `CANDIDATE` to improve a forecast.
2. `S08-G3`, `G4`, `G6`, `G7`, `G8` and `G10` are **unaffected** and their work continues now.
   Swapnali has recorded (`QA-F05`) that `S08-G3` / `QA-001` is not a migration casualty and its date
   does not move because of CR-014.
3. GitHub Actions stays green through the freeze — it is the rollback path, not a dead branch.

**Revisit if:** `CMP-F01` invalidates the target estate, or the migration slips past 2026-10-31, at
which point closing `S08` on the platform we are still on becomes the cheaper option again.

---

## 3. M0.4 — Governance-tree home

> **Question.** `docs/` (441 files, ~16 MB), `scripts/{governance,context,lifecycle}`, `AGENTS.md`
> and `CLAUDE.md` have no home in the bank's seven-project topology. Where do they go?
>
> **Owner.** Mahesh (Board 1) for the internal architecture position. The **bank** GitLab platform or
> architecture authority for the Appendix C exception.

### 3.1 The discussion

**Mahesh** ruled the internal question `yes`: the governance tree is a genuine ownership boundary —
different owners, different cadence, different review path, different consumers from either the
frontend or the backend. He rejected both alternatives on their merits rather than by preference:
placing it in `backend` runs the Java build on every governance change and makes the frontend team a
guest in the backend repository to read its own rules; splitting it across repositories destroys
`DOC-MAP.yaml`, `context-load.py` and `FreshnessCheck`, which every agent session depends on.

He then declined the second half. The seven-project topology is the **bank's** approved baseline, and
his jurisdiction covers architecture exceptions within the platform, not exceptions against an
external authority's document. The repository owner's acceptance of IMP-1 makes it our proposal, not
a granted exception.

**Amit** supported it on `S08-G10` grounds — a new engineer cannot build, test and ship within a week
if the operating model is unreachable or buried under an unrelated build.

**Shailja** attached a condition rather than an opinion: wherever it lands, the 16 MB carries
`C-CMP-2`, and the sweep runs before the push, not after.

**Shivanshi** noted the operational consequence: a separate repository means `governance.yml` gets
its own pipeline, which is simpler than the path-filtered arrangement in place today.

### 3.2 Decision

> **Internal position: `governance/platform-governance`, a ninth project.** Recorded as **`ADR-020`**.
>
> **Status: `APPROVED` 2026-08-29, conditionally** — `AC-2`. The ninth project is approved
> **subject to written acceptance of the Appendix C exception by the bank GitLab/architecture
> authority before M4.3**. The external gate is unchanged by the approval: until the bank accepts,
> M4.3 creates **eight** projects, not nine. On rejection, the fallback below applies — it is now a
> board-approved fallback rather than a contingency note.

**Fallback if the bank refuses the exception:** the tree goes to `product/backend` under a top-level
`governance/` directory, with the governance pipeline path-filtered — accepting the `S08-G10` cost
and the build-on-every-governance-change cost. This is written down now so it is a decision later
rather than a scramble.

---

## 4. M0.6 — Render disposition

> **Question.** `render.yaml` deploys one container running two JVMs. The bank baseline requires
> per-service immutable images promoted to production by digest on EKS. Does Render survive the
> migration?
>
> **Owner.** Shivanshi (Board 7) + Kalpana (`R12`).

### 4.1 The discussion

**Shivanshi** separated two questions that were being asked as one. *Is Render the target runtime?*
— no, and nobody proposed it. *Must it be switched off at cutover?* — a different question with a
different answer. Retiring it at cutover removes the only working deployment of the platform during
the single riskiest window of the programme, and buys nothing: the EKS work has not started.

**Kalpana** agreed on sequencing grounds and restated her position from `CR-014` §8: merging the
runtime re-platform into the migration puts the gate-evidence recovery and a runtime change on one
critical path, and she would not accept that sequencing.

**Deepali** did not object and attached her standing constraint explicitly rather than leaving it
implied (`SEC-F06`): Render is dev-preview only and never a data path for PII or production-like
data. Reachable from a GitLab pipeline is not a licence to send it anything real.

**Shailja** added `C-CMP-5` to the same effect from the regulatory side — no regulated customer data,
no real premium or quote values.

### 4.2 Decision

> **Render survives the migration as a dev-preview demo target, redeployed from GitLab CI.** It is
> retired when the EKS path can demonstrate an equivalent deployment — not on a date, on a
> capability.
>
> **Status: `APPROVED` 2026-08-29** — `AC-3`, with the data prohibition restated explicitly at
> approval: **no PII, no real premium or quote values, no production-like data**. Retired only after
> EKS demonstrates equivalent deployment capability. Binding: `C-SEC-8`, `C-CMP-5`.

**Consequence:** `render.yaml` migrates into `product/backend` unchanged, and one `ci-components`
template must be able to deploy it. This is roughly two hours inside `GLM-001` M7.6 and is not
separately scheduled.

---

## 5. Two findings the board round produced

Neither was in `GLM-001` when the plan was published. This is what convening the boards was for, and
both are now tracked.

### 5.1 `CMP-F01` → **IMP-14** · Data residency is unresolved — capable of `R0`

Raised by **Shailja**. The WS-3 standing constraint forbids regulated data, backups, logs **or
archives** outside AWS India regions. `GLM-001` M1.2 asks for the GitLab base URL, version and
edition, because those decide provider capability. **It never asks where the instance and its storage
physically are**, and the migration proceeds identically either way.

If the bank GitLab is self-managed in-region, the finding closes on a written confirmation. If it is
GitLab.com or a managed instance outside India, then repository content, CI job logs, artifacts, the
container registry and the Terraform state holding the estate's configuration all sit outside India,
and Board 6 cannot rule the estate permissible for material that will later carry regulated data.

**This can invalidate the destination, not merely the schedule.** Recorded as `IMP-14` in `GLM-001`,
`RISK-021` in the risk register, `ASM-022` in the assumption register, and a new task **M1.2a** with
`C-CMP-1` attached. No push to the bank estate precedes it.

### 5.2 `OPS-F04` · Cutover +24 h to archive GitHub is too tight

Raised by **Shivanshi against her own plan**. `GLM-001` M9.4 archived the origin at cutover +24
hours. That is shorter than the rollback-validation window it is supposed to follow, and far shorter
than any realistic dispute about what the archive holds — and archiving is easy to do and awkward to
reverse under a bank account model.

**Revised:** GitHub goes read-only at cutover (write access removed, nothing archived), stays
restorable for **14 days**, and is archived only once `C-CMP-4` names the disposition — owner,
retention period, access control, departure handling, deletion trigger and authority. `GLM-001` M9.4
is amended; `C-OPS-4` supersedes the original text.

---

## 6. Decision summary

| ID | Decision | Owner | Status | Blocking condition |
|---|---|---|---|---|
| **M0.1** | Triage and register the migration | Shivanshi | **DONE** | — |
| **M0.2** | `CR-014` | Boards | **`APPROVED_WITH_CONDITIONS` 2026-08-29** | 29 board conditions + `AC-1`…`AC-5` |
| **M0.3** | Re-evidence `S08-G1/G2/G5/G9` on GitLab; GitHub Actions green for **rollback continuity only** | Amit + boards | **`APPROVED`** (`AC-1`) | `GATE-S08` stays `OPEN` throughout |
| **M0.4** | `governance/platform-governance` as a ninth project (`ADR-020`) | Mahesh → bank | **`APPROVED`, conditional** (`AC-2`) | Bank Appendix C acceptance **before M4.3** |
| **M0.5** | `CR-015` — **Option B**: persistence ownership per bounded context, after migration (`ADR-019`) | Mahesh + Aarti | **`APPROVED` target model** | Aarti's S09 integrity/recovery review still owed |
| **M0.6** | Render survives as dev-preview demo target | Shivanshi + Kalpana | **`APPROVED`** (`AC-3`) | No PII / real values / production-like data |

**Phase M0 is CLOSED.** All six items are decided. `GLM-001` M3 (bootstrap IaC) is unblocked and may
start.

**What remains outstanding is no longer M0's.** Three things gate later phases and none of them
blocks M3:

| Outstanding | Gates | Owner |
|---|---|---|
| Bank Appendix C acceptance (`AC-2`, `ASM-021`) | **M4.3** — eight projects until it lands | bank authority |
| Twelve enterprise inputs (`ASM-014` … `ASM-024`) | M3.3, M3.4 detail · M5.2 · M8 | bank |
| Aarti's S09 integrity and recovery review (`CR-015` Q4) | The S09 allocation migration, **not** the GitLab migration | Aarti |

---

## 7. The twenty-nine conditions, consolidated

Attached by the boards across `CR-014`. Each names the phase it gates.

| Owner | Conditions | Gate the migration at |
|---|---|---|
| Deepali — Security | `C-SEC-1` … `C-SEC-8` | M5.2 (first push), M4.2 (first apply), M9.4 (cutover) |
| Shailja — Compliance | `C-CMP-1` … `C-CMP-5` | M5.2 (first push), M6.9, M9.4 |
| Mahesh — Architecture | `C-ARC-1` … `C-ARC-5` | M4.3, M5.2, M5.3, M5.8, M5.9 |
| Shivanshi — Operations | `C-OPS-1` … `C-OPS-6` | freeze start, freeze lift, M7.9, M9.4, M10.3 |
| Amit — Engineering | `C-ENG-1` … `C-ENG-5` | M5.3, M5.10, M7.9, M7.11 |
| Swapnali — QA | `C-QA-1` … `C-QA-5` | freeze lift, and each criterion claim |

Two are **hard blocks on the first push to the bank estate** and neither has a workaround:
`C-SEC-1` (clean full-history secret scan) and `C-CMP-1` (data residency confirmed permissible).

---

## 8. What was decided, and what still is not

Recorded on 2026-08-29 after the board outcome.

| Decided | By |
|---|---|
| `CR-014` — `APPROVED_WITH_CONDITIONS`, 29 board conditions plus `AC-1`…`AC-5` | Boards, relayed 2026-08-29 |
| `CR-015` — **Option B** as the target model, `ADR-019` | Boards, relayed 2026-08-29 |
| M0.3, M0.4 (conditionally), M0.6 | Boards, relayed 2026-08-29 |

| Still not decided | Why it stays open |
|---|---|
| The bank's Appendix C exception | External authority. `AC-2` makes its written acceptance a precondition of M4.3; approving M0.4 did not grant it |
| Whether the estate is permissible | `CMP-F01` / `ASM-022` is unanswered. `C-CMP-1` blocks the first push and the approval did not lift it |
| A security exception if the edition is Premium | `RISK-017`. Deepali declined to pre-approve a downgrade she cannot yet see, and the approval did not overrule that |
| The **S09 persistence allocation migration** | `CR-015` approved a *target*. Aarti's integrity and recovery guarantees (Q4) are unanswered, and a data migration is not approved by approving its destination |
| Any `GATE-S08` criterion as satisfied | `AC-1` keeps the gate `OPEN` throughout. Unexecuted is still not passed |

> **On provenance.** This file records a board outcome relayed by the repository owner. The seven
> files under [`CR-014/verdicts/`](./change-requests/CR-014/verdicts/README.md) remain **AI-drafted
> board inputs** and are not signature artefacts — they are retained because the twenty-nine
> approved conditions are defined in them. The agent recorded a decision the owner gave; it did not
> supply one.
