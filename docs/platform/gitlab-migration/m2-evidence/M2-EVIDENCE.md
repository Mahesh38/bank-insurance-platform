# M2 — Pre-migration hygiene: evidence pack

**Executed:** 2026-08-29 · **Owner:** Shivanshi (SRE / `R10`)
**Authorised by:** [`CR-014`](../../../governance/change-requests/CR-014-gitlab-estate-migration.md) `APPROVED_WITH_CONDITIONS`
**Plan:** [`GLM-001` §3 Phase M2](../GLM-001-migration-plan.md)
**Status:** **M2.1 BLOCKED on a real finding.** M2.5, M2.6, M2.7 complete. M2.2 awaiting Board 4.

> ## Headline
> **`C-SEC-1` has fired.** One real credential — a base64 AES-256 `RAW_PAYLOAD_ENCRYPTION_KEY` —
> is in `main`'s history, reachable from 60 branches. Per `C-SEC-2` the order is **rotate, then
> scrub, then re-scan**, and rotation is a question only the operator can answer. **No push to the
> bank estate may proceed until this closes.**

---

## 0. A correction that invalidates a number used throughout the programme

**The session's clone of the repository was shallow.** `git rev-parse --is-shallow-repository`
returned `true`, with 12 shallow boundary commits.

Everything measured before 2026-08-29 07:39 used that clone. The corrected figures:

| Measure | Recorded in GLM-001 / CR-014 | **Actual** |
|---|---|---|
| Commits on `main` | 273 | **358** |
| Commits across all refs | 407 | **437** |
| Root commits | 12 (apparent) | **1** |
| Remote branches | 81 | 81 — unchanged |
| Tags | 0 | 0 — unchanged |

**Why it mattered more than a wrong number.** Four of the shallow boundary commits presented as
*root* commits, so gitleaks treated their entire tree as newly added and re-flagged the same
Dockerfile secret five times. The first scan reported **9 findings**; the complete history reports
**5**. A shallow clone does not merely under-report — here it *over*-reported, and either direction
would have produced a wrong verdict on `C-SEC-1`.

**This is the single most important lesson from M2 for the real migration:** the migration host must
verify `--is-shallow-repository` is `false` before any scan, split or push. A shallow clone that
scans clean proves nothing. Added as condition `C-OPS-7` (proposed) and to the M2 procedure.

---

## 1. M2.6 — rollback anchor · **COMPLETE (local), push not yet authorised**

```
tag     pre-gitlab-migration
points  b8027751738b04d00dbe071a77b2aba56828a2cd
        = origin/main @ 2026-08-28 18:45:49 +0530
```

Annotated tag created **before** any history rewrite, so it anchors the pre-scrub state (`C-OPS-1`).

> **Awaiting authorisation.** Pushing the tag writes to the shared origin outside the designated
> working branch. It is created and verified locally; say the word and it goes up.

---

## 2. M2.1 — full-history secret scan · **BLOCKED — 1 real credential**

`gitleaks` **8.21.2** (the version CI pins), `--config .gitleaks.toml`, `--log-opts=--all`,
`--redact`, `--exit-code 1`. 344 commits scanned across 82 refs.

### 2.1 The five findings

| # | Rule | Location | Introduced | Verdict *(SRE technical read — Board 4 owns the ruling)* |
|---|---|---|---|---|
| **1** | `generic-api-key` | `Dockerfile:61` | `d2a3d4e6` · 2026-08-04 · Claude | **REAL CREDENTIAL.** See §2.2 |
| 2 | `generic-api-key` | `services/bank-persistence-service/src/test/resources/application-test.yml:22` | `6ed3ad23` · 2026-07-30 · Cursor Agent | Test fixture — `key-id: test-v1`. Distinct value from #1. Board 4 rules whether a test key may enter the bank estate |
| 3 | `generic-api-key` | `docs/design/figma/…/tokens.json:26` | `72b96c0a` · 2026-08-28 | **False positive** — a Figma *publishedStyle key* (40-hex content identifier) |
| 4 | `generic-api-key` | `docs/design/figma/…/inventory.json:3` | `72b96c0a` · 2026-08-28 | **False positive** — the Figma `fileKey`, which is already in the directory path |
| 5 | `generic-api-key` | `docs/figma_file_structure.json:18170` | `9fea89bb` · 2026-08-25 · Mahesh38 | **False positive** — same family |

Values are not reproduced here. Where identity mattered, values were compared by `sha256[:12]`.

### 2.2 Finding 1 — the one that blocks

```
ENV RAW_PAYLOAD_ENCRYPTION_KEY=<44-char base64, sha256[:12]=0651f16a1318>
```

| | |
|---|---|
| **What it protects** | The `raw_payload` store — **PII, 7-year retention** |
| **Exposure window** | 2026-08-04 → 2026-08-14, three commits carrying the literal value |
| **Reachable from `main`** | **YES** — `d2a3d4e6` and `f3b96f39` are ancestors of `origin/main` |
| **Reachable from** | **60 branches**, plus `0201bcb8` on `claude/cr-001-completion-8e1v5z` |
| **Status in HEAD** | **Removed.** The current `Dockerfile` documents the removal and why |
| **Distinct from** | Both test keys — this is its own value |

Two things follow, and they point in opposite directions.

**It cannot be avoided by branch selection.** It is on `main`. Migrating a reduced branch allowlist
does not exclude it. Remediation is a history rewrite of `main`, or a Board 4 ruling that the
rotated value may be imported.

**The repository already caught itself.** HEAD's `Dockerfile` says, in its own words, that a default
key committed to the repository means any deployment that forgets to override it encrypts regulated
data with a public key, and that it contradicted the platform's own rule. The control worked — but
only forward. History was never revisited, which is exactly the gap `IMP-6` was raised about.

### 2.3 The question only the operator can answer

`C-SEC-2` orders remediation **rotate → scrub → re-scan**. Rotation needs one fact this repository
cannot supply:

> **Is the exposed value currently set as `RAW_PAYLOAD_ENCRYPTION_KEY` in any running environment —
> in particular the Render dev-preview, where it is a `sync: false` dashboard variable — and is any
> stored `raw_payload` data encrypted under it?**

If yes: rotate, and re-encrypt or dispose of the affected data, **before** any history rewrite.
Scrubbing first destroys the evidence of what to rotate and leaves the live key live.

If no — the value never left the Dockerfile default and no environment used it — the finding
reduces to a history-hygiene problem and `C-SEC-2` is satisfied by scrub-and-rescan alone.

`C-CMP-5` and `C-SEC-8` restrict Render to non-PII dev-preview data, which if honoured bounds the
blast radius. **Honoured is not the same as verified**, and Board 4 owns that distinction.

### 2.4 Scan-scope comparison (retained as method evidence)

| Scope | Commits | Findings |
|---|---|---|
| Working tree only | — | **1** |
| `--log-opts=HEAD` | 219 | 6 |
| CI default (`gitleaks git .`) | 344 | **5** |
| `--log-opts=--all` | 344 | **5** |

**The CI job's scope is adequate.** `gitleaks git` already walks all refs, so the scheduled
history scan is not under-scoped — a concern raised and now closed by measurement rather than
assumption.

### 2.5 A current CI finding

The **working-tree** scan returns **1 finding**: `docs/figma_file_structure.json:18170`, the Figma
identifier. `security-scanning.yml` runs the tree scan with `--exit-code 1` and the step is marked
*"BLOCKS the build"*.

**So the repository's own blocking secret-detection job should be failing today**, on a false
positive, since that file was committed on 2026-08-25. That is `S08-G5`'s secret-detection leg red
on a non-secret — which is how a real finding gets ignored later. It is fixed by the `M2.2`
allowlist decision, not by a waiver.

---

## 3. M2.2 — allowlist decision sheet · **awaiting Board 4**

`.gitleaks.toml` states its own rule: *"Every allowlist entry below must state WHY the match is not
a secret. An unexplained allowlist entry is indistinguishable from a suppressed finding."* The
existing entries comply. `C-SEC-4` requires each to be re-justified against **bank** rules before
`security-policies` inherits it — that re-justification is Deepali's, not the agent's.

**One new entry is proposed, not applied**, to close §2.5:

| Proposed | Justification | Risk if granted |
|---|---|---|
| Path `^docs/(design/)?figma.*\.json$` for rule `generic-api-key` | Figma style keys and file keys are public content identifiers, high-entropy by construction. The file key is already in the directory path | Narrow to Figma JSON and one rule. A real credential pasted into a Figma export would be missed — judged low, as these files are machine-generated by `scripts/figma/figma-extract.py` |

The alternative — suppressing `generic-api-key` repository-wide — would be a control failure, and
`.gitleaks.toml`'s own header already warns against exactly that.

---

## 4. M2.5 — branch triage · **COMPLETE**

Full detail: [`M2-5-branch-triage.md`](./M2-5-branch-triage.md).

| | Count |
|---|---|
| Remote branches excluding `main` | 81 |
| Merged into `main` by ancestry | 52 |
| Not merged, but **zero unapplied patches** (squash-merged) | 4 |
| **Carrying genuinely unapplied work** | **25** |

The ancestry check alone would have flagged 29 branches as unmerged and overstated the risk;
`git cherry` patch-id comparison shows 4 of those are already in `main` by content. **56 of 81
branches are archive-only.** The 25 with real unapplied work need an owner decision before they are
bundled — including `claude/cr-001-completion-8e1v5z`, which carries the third key-bearing commit.

---

## 5. M2.7 — split rehearsal · **COMPLETE, 18/18 checks pass**

`git filter-repo` into three throwaway clones from `origin/main` at `b802775` (358 commits).

| Split | Commits | Kept paths |
|---|---|---|
| `frontend` | **2** | `apps/rm-workspace-app/` |
| `backend` | 47 | `services/`, `libs/`, `config/`, Gradle, Docker, `render.yaml` |
| `platform-governance` | 337 | `docs/`, `scripts/`, `AGENTS.md`, `CLAUDE.md`, `.claude/` |

**Verification — a pass/fail artefact, per `C-QA-1`:**

- **6/6 tree-hash equivalences.** Every kept subtree hashes identically to the same subtree of
  `origin/main`. This is the check that proves content, not inspection.
- **3/3 authorship preserved.** No author appears in a split that is not in the source.
- **6/6 author-date sequences match** over the kept paths — 2, 30 and 277 commits respectively.
- **2/2 exclusions hold.** No `Dockerfile` history reaches `frontend` or `platform-governance`.

**A correction made during verification.** The first date check compared each split's HEAD date
against the newest source commit *touching* the kept paths, and failed 2 of 14. The split was
correct; the check was wrong — a split's HEAD is the rewritten merge commit, whose date is later
than the last commit that changed those paths. Re-expressed as "newest commit touching kept paths,
both sides", it passes. Recorded because a verification script that reports false failures gets
disabled, and then it protects nothing.

**Two findings from the rehearsal:**

1. **`frontend` has 2 commits.** The Flutter app arrived late and carries almost no history.
   "Preserve history" is near-moot for that repository — worth knowing before anyone budgets for it.
2. **The split is clean.** No path required manual resolution, and `filter-repo` reported no
   conflicts, consistent with Amit's `ENG-F01` finding that the seam has no build coupling.

---

## 6. State of M2

| Task | Status |
|---|---|
| M2.1 blocking full-history scan | **BLOCKED — 1 real credential.** `C-SEC-1` fired |
| M2.2 allowlist re-review | **Awaiting Board 4.** Decision sheet at §3 |
| M2.3 rotate → scrub → re-scan | **Not started** — gated on the §2.3 operator answer |
| M2.4 `docs/` PII / NDA sweep | **Not started** |
| M2.5 branch triage | **COMPLETE** |
| M2.6 rollback anchor | **COMPLETE (local)** — push not yet authorised |
| M2.7 split rehearsal | **COMPLETE — 18/18** |
| M2.8 verify split builds | **Not started** — no Flutter toolchain in this environment; backend build pending |

**M2 cannot close, and should not.** `C-SEC-1` is a hard block on the first push and it has fired
on a real credential. That is the gate working.
