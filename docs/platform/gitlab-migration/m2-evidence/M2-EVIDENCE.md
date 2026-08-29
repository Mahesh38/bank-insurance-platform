# M2 — Pre-migration hygiene: evidence pack

**Executed:** 2026-08-29 · **Owner:** Shivanshi (SRE / `R10`)
**Authorised by:** [`CR-014`](../../../governance/change-requests/CR-014-gitlab-estate-migration.md) `APPROVED_WITH_CONDITIONS`
**Plan:** [`GLM-001` §3 Phase M2](../GLM-001-migration-plan.md)
**Status:** **M2.1 BLOCKED on a real finding.** M2.5, M2.6, M2.7 complete. M2.2 awaiting Board 4.

> ## Headline — dispositioned 2026-08-29
> **Finding B — `COMPROMISED / POTENTIALLY LIVE`.** *"A runnable Docker default cannot be declared
> dead without deployment evidence."* `C-SEC-2` applies in full and **`filter-repo` is barred** until
> B is rotated or formally retired, any affected `raw_payload` data is handled, and the
> `pre-gitlab-migration` anchor is verified on the remote.
>
> **Finding C — `TEST FIXTURE / NON-CREDENTIAL`.** No rotation, no scrub. Exact-path allowlist
> proposed for Board 4. **Packaging verified: the built jar contains no test resources** (§2.7).
>
> **Findings 3–5 — `FALSE_POSITIVE / NON-CREDENTIAL`.** Figma content identifiers.
>
> Nothing has been rotated, scrubbed or rewritten by this workstream.

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

**Push authorised 2026-08-29** as a narrow exception (tag only, exact target, no move/overwrite/delete).
Three guards were asserted and all passed: remote `main` still exactly `b802775`; the tag absent on
the remote; the local tag pointing at the authorised commit.

> **Push REFUSED by GitHub.** `HTTP 403` at send-pack, twice. The egress proxy recorded **no**
> failure (`recentRelayFailures: []`), so this is GitHub-side: the session credential permits branch
> pushes on `claude/*` and not tag refs. Not a policy denial to route around, and not something the
> operator's grant can widen. **The anchor exists locally and is verified; it must be pushed by a
> credential with tag-write scope before the freeze (`C-OPS-1`).**

---

## 2. M2.1 — full-history secret scan · **BLOCKED — 1 real credential**

`gitleaks` **8.21.2** (the version CI pins), `--config .gitleaks.toml`, `--log-opts=--all`,
`--redact`, `--exit-code 1`. 344 commits scanned across 82 refs.

### 2.1 The five findings

| # | Rule | Location | Introduced | Verdict *(SRE technical read — Board 4 owns the ruling)* |
|---|---|---|---|---|
| **B** | `generic-api-key` | `Dockerfile:61` | `d2a3d4e6` · 2026-08-04 · Claude | **`COMPROMISED / POTENTIALLY LIVE`** — operator ruling 2026-08-29. `C-SEC-2` in full. See §2.6 |
| **C** | `generic-api-key` | `…/application-test.yml:22` | `6ed3ad23` · 2026-07-30 · Cursor Agent | **`TEST FIXTURE / NON-CREDENTIAL`** — operator ruling 2026-08-29. Packaging verified §2.7 |
| 3 | `generic-api-key` | `docs/design/figma/…/tokens.json:26` | `72b96c0a` · 2026-08-28 | **`FALSE_POSITIVE / NON-CREDENTIAL`** — operator ruling 2026-08-29. Figma publishedStyle key |
| 4 | `generic-api-key` | `docs/design/figma/…/inventory.json:3` | `72b96c0a` · 2026-08-28 | **`FALSE_POSITIVE / NON-CREDENTIAL`** — operator ruling 2026-08-29. Figma `fileKey`, already in the directory path |
| 5 | `generic-api-key` | `docs/figma_file_structure.json:18170` | `9fea89bb` · 2026-08-25 · Mahesh38 | **`FALSE_POSITIVE / NON-CREDENTIAL`** — operator ruling 2026-08-29. 40-hex style key, `"name": "app-grid"`. **This is the finding the ruling describes** |

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

### 2.3 Operator ruling, 2026-08-29 — recorded as given

> *"Rotation: not required. The sole finding is a 40-character Figma remote-style identifier for
> app-grid in `docs/figma_file_structure.json`, not authentication material. Record it as
> FALSE_POSITIVE / NON-CREDENTIAL; M2.3 is not triggered (N/A). Do not rotate anything or run
> `git filter-repo`. Board 4 should approve a narrowly scoped allowlist in M2.2, followed by a
> clean rescan."*

**Actioned:** findings 3, 4 and 5 recorded `FALSE_POSITIVE / NON-CREDENTIAL`. No rotation performed.
No `filter-repo` run. `M2.3` **N/A** for those three. The narrow allowlist is prepared at §3 and
**not applied**, pending Board 4.

The ruling's description is precisely correct for the finding it names — `figma_file_structure.json`
line 18170 is a 40-hex Figma `publishedStyle` key with `"name": "app-grid"`, and it is the **only**
finding the working-tree scan returns.

### 2.6 Dispositions for B and C — operator ruling, 2026-08-29

#### Finding B — `COMPROMISED / POTENTIALLY LIVE`

> *"C-SEC-2 applies: rotate or formally retire it first, check whether any `raw_payload` rows decrypt
> with it, re-encrypt/dispose affected data if found, then scrub B from migration history and
> re-scan. A runnable Docker default cannot be declared dead without deployment evidence."*

The reasoning is the part worth preserving: **a default that ships in a runnable image is live until
proven otherwise.** Absence of deployment evidence is not evidence of absence, and the burden sits
the right way round.

Required sequence, none of it started, none of it an agent's to perform alone:

| # | Step | Owner |
|---|---|---|
| 1 | Rotate the key, **or** formally retire it with a recorded decision | Deepali + operator |
| 2 | Determine whether any stored `raw_payload` row decrypts with it | Aarti + Amit |
| 3 | Re-encrypt or dispose of any affected data | Aarti · Shailja rules on disposal |
| 4 | **Only then** scrub B from migration history | SRE |
| 5 | Re-scan clean, retain the report | SRE · Swapnali on sufficiency |

#### Finding C — `TEST FIXTURE / NON-CREDENTIAL`

> *"No rotation and no history scrub. Board 4 may approve an exact allowlist limited to
> `src/test/resources/application-test.yml`; confirm the packaged backend excludes test resources."*

**Proposed for Board 4, not applied** — exact path, not a pattern:

```toml
# Test fixture key. key-id: test-v1, used only by the H2 in-memory test profile.
# Verified 2026-08-29: not present in the packaged bootJar (M2-EVIDENCE section 2.7).
'''^services/bank-persistence-service/src/test/resources/application-test\.yml$'''
```

### 2.7 Packaging verification for the C disposition — **PASS**

`./gradlew :services:bank-persistence-service:bootJar`, then inspection of
`bank-persistence-service.jar`:

- **No `application-test.yml`.** No `test-classes`, no `/test/` entries of any kind.
- Packaged config is exactly: `application.yml`, `application-local.yml`, `application-uat.yml`,
  `application-prod.yml`.

The test fixture key **cannot reach a running deployment through the artefact**. Board 4's condition
is satisfied on the evidence.

### 2.8 `filter-repo` is barred until two conditions are met

Recorded so it cannot be forgotten under schedule pressure:

1. **B is rotated or formally retired**, and any affected `raw_payload` data is handled.
2. **`pre-gitlab-migration` is verified present on the remote** — see §1, currently blocked.

Neither is satisfied. **No history rewrite may run**, and none has.

### 2.4 Scan-scope comparison### 2.4 Scan-scope comparison (retained as method evidence)

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
| M2.1 blocking full-history scan | **All 5 dispositioned.** B `COMPROMISED/POTENTIALLY LIVE` — remediation not started. C and 3–5 closed |
| M2.2 allowlist re-review | **Awaiting Board 4.** Two exact-scope entries proposed: Figma (§3) and the C fixture (§2.6). Neither applied |
| M2.3 rotate → scrub → re-scan | **REQUIRED for B**, 5 steps at §2.6, not started, not an agent's to perform. N/A for C and 3–5. **Nothing rotated, scrubbed or rewritten** |
| M2.4 `docs/` PII / NDA sweep | **COMPLETE — reporting only.** 63 hits, **0 credentials, 0 real customer data**. 3 items for Board 6; 23 binary assets need human eyes. [`M2-4-docs-sweep.md`](./M2-4-docs-sweep.md) |
| M2.5 branch triage | **COMPLETE** |
| M2.6 rollback anchor | **Local: complete and verified. Remote: BLOCKED** — GitHub refuses tag pushes on this credential |
| M2.7 split rehearsal | **COMPLETE — 18/18** |
| M2.8 verify split builds | **Backend PASS** — 503 tests, 0 failures, 53 tasks, all 10 modules. **Flutter `BLOCKED_ENVIRONMENT`, not passed** (§7) |

**M2 cannot close yet.** Three findings are dispositioned and closed. Two await a disposition, and
the rollback anchor is not on the remote. Neither is a reason to rush: `C-SEC-1` gates the first push
to the bank estate, and that push is phases away.

---

## 7. M2.8 — build verification

### 7.1 Backend — **PASS**

`./gradlew test`, JDK 21, clean environment.

| | |
|---|---|
| Result | `BUILD SUCCESSFUL` in 2m 07s |
| Tasks | 53 executed |
| Modules | all 10 — 5 services, 5 shared libraries |
| **Tests** | **503 · 0 failures · 0 errors · 0 skipped** |
| Coverage | JaCoCo reports generated for every module |

This corroborates Amit's `ENG-F01` from the other direction: the backend builds and tests green as a
unit, and the split rehearsal (§5) showed the frontend carries no Gradle coupling to sever.

**Not yet done:** building from the *split* clone rather than the monorepo. `C-ENG-5` requires
`./gradlew test` from a fresh checkout of the split `backend`, and that is a separate run.

### 7.2 Flutter — `BLOCKED_ENVIRONMENT`

**Recorded as blocked, explicitly not as passed.** No Flutter or Dart toolchain exists in this
session — `which flutter dart` returns nothing, and installing one is not migration work.

`apps/rm-workspace-app` therefore has **no** verification evidence: not a pass, not a fail, no run.
Per `C-QA-5`, this may not later be recorded as satisfied by anything other than an executed run on
a host with the toolchain. The frontend split is 2 commits, so the run is cheap — it just cannot
happen here.
