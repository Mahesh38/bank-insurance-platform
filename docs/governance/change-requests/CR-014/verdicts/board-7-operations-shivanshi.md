# Board 7 — Operations · Position on CR-014

**Change request:** [CR-014](../../CR-014-gitlab-estate-migration.md) · **Plan:** [`GLM-001`](../../../../platform/gitlab-migration/GLM-001-migration-plan.md)
**Board:** 7 — Operations · **Role:** `R10`
**Persona:** Shivanshi — Principal SRE / Reliability Engineering Head
**Reviewer type:** `AGENT` · **Self-review:** **true — I authored GLM-001** · **Change tier:** `T4` · **Date:** 2026-08-29

> ### ⚠ Self-review, and it is disclosed rather than hidden
> I drafted the plan this board is reviewing. That is a weak review position, and the honest
> response is to state it and to spend the review looking for what the plan gets wrong rather than
> confirming what it gets right. One correction to my own plan is recorded at `OPS-F04`.
>
> **`signature_status: AI-DRAFTED`**

> ## Drafted verdict: `APPROVE-WITH-CONDITIONS`
> **Operational severity: `O1` — high risk.** One `O0` in the plan (secret hygiene, IMP-6) is
> contained by design and by Deepali's `C-SEC-1`/`C-SEC-2`, not by optimism.

---

## 1. The O1–O8 checks, applied to the migration itself

The migration is a change to be operated, so it answers the same checklist any release does.

| # | Check | Position |
|---|---|---|
| `O1` | **Deployability** — config, secrets, migrations, ordering | The ordering *is* the risk. Ten binding constraints are named in GLM-001 §4; three of them (`M2.1→M5.2`, `M4→M5`, `M5→M6.1`) are the ones that break the estate if inverted |
| `O2` | **Observability** | Weak, and acknowledged. Until M7.12 measures pipeline p95 and flake on bank runners, we cannot see the new platform. `S08-G9` is not evidenced by a plan |
| `O3` | **Alerting** | Not yet applicable. Drift detection (M10.1) is the first operational signal and it reports, it does not page |
| `O4` | **Failure modes and blast radius** | The largest blast radius in the programme is the bootstrap Terraform state — it can delete every repository including the governance tree. `C-SEC-6` and IMP-10 contain it |
| `O5` | **Rollback** | Adequate only if `M9.5` is executed. See `OPS-F02` |
| `O6` | **Capacity and cost** | Not material. 14.9 MiB of history; runner capacity is a bank input (M1.5), not a platform build |
| `O7` | **Runbooks** | M10.2 covers token rotation, state recovery, drift reconciliation and emergency manual change. Adequate as scoped |
| `O8` | **Backward compatibility during rollout** | The freeze window is what substitutes for rolling compatibility. See `OPS-F03` |

---

## 2. Findings

### `OPS-F01` · `O1` — there is no rollback anchor today

Zero tags across 273 commits. If cutover is reverted there is currently nothing to point at, and
"revert to `main` as it was" is a sentence with no referent once branches have moved.

`pre-gitlab-migration` (M2.6) is fifteen minutes of work and it is the difference between a rollback
and an argument. It must exist before the freeze, not during it.

### `OPS-F02` · `O1` — a documented rollback is not a rollback

M9.5 tests rollback against the tag before releasing the freeze. I am hardening it, because this is
the check that gets dropped when a cutover runs late: **rollback is tested by executing it**, not by
reading the procedure. Clone from the anchor, verify the tree, confirm the build. If that has not
been done, the freeze does not lift and GitLab is not declared authoritative.

### `OPS-F03` · `O1` — a freeze without an end time is a dual-write

IMP-13 asks for ≤48 hours with a named owner. I am adding the failure branch, which the plan lacks:
**what happens if the window expires with the migration incomplete.** Without a pre-agreed answer the
default is "keep both writable for a bit", which is precisely the state the baseline forbids and the
one that becomes permanent.

The answer is roll back to the anchor and re-schedule. It is cheaper than two sources of truth, and
it is only cheaper if it was decided in advance.

### `OPS-F04` · `O2` — **correcting my own plan: cutover +24 h is too tight**

GLM-001 M9.4 archives the GitHub origin read-only at cutover +24 hours. Reviewing it against
`OPS-F02` and Shailja's `CMP-F04`, I no longer think that is right, and I raised it.

Twenty-four hours is shorter than the rollback-validation window it is supposed to follow, and far
shorter than any realistic dispute about what the archive contains or who may reach it. Archiving is
also a step that is *easy to take and awkward to reverse* under a bank account model — exactly the
shape of action that should not be on a 24-hour timer during the busiest week of the programme.

**Revised:** GitHub goes read-only at cutover (write access removed, nothing archived), stays
restorable for **14 days**, and is archived only after `C-CMP-4` names the disposition. GLM-001 M9.4
is amended accordingly.

### `OPS-F05` · `O2` — `S08-G9` will get worse before it gets better

Pipeline feedback under 10 minutes at p95 is currently measured on GitHub-hosted runners with a
warm Gradle cache and a repository that contains everything. After the split, `backend` builds
without the frontend — faster — but on bank runners of unknown shape, with cold caches, and through
a component-resolution step that does not exist today.

I expect the first GitLab measurement to be **worse**, and I want that expected rather than treated
as a regression. M7.12 measures; it does not promise. If the number lands above 10 minutes the
response is runner and cache work, not a re-baselined criterion.

---

## 3. Conditions

| ID | Condition | Must be true before |
|---|---|---|
| `C-OPS-1` | `pre-gitlab-migration` tag exists on GitHub `main` and is verified reachable | The freeze starts |
| `C-OPS-2` | Rollback **executed** against the anchor — clone, verify tree, confirm build — not merely documented | Freeze lifts / GitLab declared authoritative |
| `C-OPS-3` | Freeze window carries a named owner, an announced start, an end time, and a pre-agreed expiry action (roll back and re-schedule) | GLM-001 M5.1 |
| `C-OPS-4` | GitHub read-only at cutover; restorable for 14 days; archived only after `C-CMP-4` | GLM-001 M9.4 — supersedes the +24 h in GLM-001 |
| `C-OPS-5` | Job-token allowlists applied before the first cross-project pipeline runs, not reactively | GLM-001 M7.9 |
| `C-OPS-6` | `S08-G9` re-measured on bank runners and reported honestly, including a regression | GLM-001 M10.3 |

---

## 4. What I am not deciding

- **Not** whether the re-measured `S08-G9` evidence is sufficient — Swapnali owns evidence.
- **Not** data residency or retention — Shailja's, and `CMP-F01` may change where this estate can be.
- **Not** the security exception if the edition is Premium — Deepali's alone.
- **Not** service boundaries. `C-ARC-1` is Mahesh's line and I operate inside it.
- **Not** the schedule. I have named what must be true; Kalpana sequences it.
