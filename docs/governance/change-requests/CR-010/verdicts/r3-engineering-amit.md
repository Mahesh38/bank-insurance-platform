# R3 — Engineering · Verdict on CR-010

**Change request:** [CR-010 — Reusable Context Module and Safe Autopilot Foundation](../../CR-010-context-module-and-safe-autopilot.md)
**Plan:** [PLAN-001](../../../plans/PLAN-001-context-module-and-safe-autopilot.md)
**Role:** R3 — Engineering · **Board:** 2 — Technical
**Persona:** Amit — Technical Head (AU Bank Insurance Platform)
**Reviewer type:** `AGENT` · **Self-review:** false
**Change tier:** T4 (per the Board 1 assessment) · **Date:** 2026-08-16

> ### ⚠ This is an AI-drafted simulation of Amit's engineering reasoning
> Drafted by the Architecture agent (Mahesh) as an input to Board 2, using the persona package at
> [`amit-technical-head.md`](../../../../context/roles/amit-technical-head.md). It is **not** Amit's
> verdict and does not carry his authority. Amit owns application implementation, coding standards,
> build/CI implementation correctness and engineering quality; Shivanshi owns CI/CD platform
> mechanics and Board 7 posture, and her verdict is separate.

> ## Verdict: `APPROVE-WITH-MODIFICATION`
> **`signature_status: AI-DRAFTED — mandatory human signature outstanding`**

---

## 1. Decision requested

CR-010 §4 asks Engineering to conclude on **scripts, CI and maintainability**.

Scope of this review: `.github/workflows/application-ci.yml`, `.github/workflows/governance.yml`,
`scripts/governance/autopilot.py`, `scripts/governance/ci-checks.py`,
`scripts/governance/test_autopilot.py`, `scripts/governance/FreshnessCheck.java`,
`scripts/context/validate-context.py`, `scripts/context/test_context.py`,
`scripts/lifecycle/generate-backlog.py`, `build.gradle.kts` coverage configuration, and the
`.gitignore`/`.github` layout.

Out of scope: `services/` and `libs/` source, which CR-010 does not touch and this phase does not
modify.

---

## 2. What I reviewed

Read, not assumed:

- both workflow files in full, including triggers, path filters, permissions and step ordering;
- `autopilot.py` end to end — argument parsing, policy validation, selection, proposal emission and
  the output write path;
- `test_autopilot.py` — all five tests;
- `ci-checks.py` checks 2, 3, 5, 6 and 8 (schema, gate-evidence agreement, routing closure, link and
  anchor resolution, semantic state);
- `build.gradle.kts` lines 60–125 — the JaCoCo report and verification wiring and the per-module
  floors;
- `.gitignore` and the `.github/` directory listing;
- the module inventory under `services/` and `libs/`.

---

## 3. The finding that matters most

**The application now has a CI workflow. That is the single highest-leverage change in this CR and
I want it on the record as a genuine improvement.** Twenty thousand lines of Java in a regulated
financial application had never been built by an automated system. `application-ci.yml` changes
that, and it is correctly minimal: checkout, JDK 21, Gradle, `test jacocoTestReport
jacocoTestCoverageVerification`, upload reports. No cleverness. Good.

It is also, today, **a mechanism with no run history**, and S08-G1 requires evidence level E4. I am
not going to pretend a merged YAML file is a green build. The honest engineering position is: the
mechanism is right, the evidence does not exist yet, and the first green run is the milestone worth
tracking.

---

## 4. Findings

### T-F01 · Blocking · Path filters and required status checks are incompatible as configured

`application-ci.yml` triggers on `pull_request` with a `paths:` filter over `services/**`,
`libs/**`, the Gradle build files and the workflow itself. S08-G2 requires that merging to `main` is
impossible without a green pipeline, which means making this a **required status check**.

On GitHub, a required check that is skipped by a path filter never reports a conclusion. The PR
either sits blocked forever, or — depending on the ruleset configuration — merges without it.
Neither satisfies S08-G2.

**Fix:** the standard pattern is a lightweight always-running job that reports the same check name,
short-circuiting to success when no relevant path changed; or drop the `pull_request` path filter
and rely on Gradle's up-to-date checks and build caching for cost. I would take the second for a
repository this size — the build is small, the filter saves little, and one fewer moving part in a
gate mechanism is worth more than the compute.

### T-F02 · Blocking · Autopilot safety tests bind to live state

`test_autopilot.py` calls `autopilot.load_bundle()` with no arguments, which reads the real
`docs/governance/state/GATE-EVIDENCE.yaml`, then asserts `selected["id"] == "4.2"` and
`selected["id"] == "4.4"`.

Two problems, and the second is the one that will actually cost us:

1. The safety property is verified only against today's data shape.
2. A legitimate edit to gate evidence breaks a **safety** test. That trains people to edit the test
   alongside the state file, and a guard you routinely edit to make a change land is not a guard.

`load_bundle()` already takes `evidence_path` and `schema_path` parameters. The fix is committed
fixtures under `scripts/governance/fixtures/` and a separate, small smoke test that the live file
loads and validates. Concurs with Board 1 finding A-F02.

### T-F03 · Blocking · `--output` writes anywhere

`autopilot.py` `propose-transition --output <Path>` calls `args.output.write_text(...)` with no path
constraint. I am not going to restate Board 1's A-F01 argument; from an engineering standpoint the
fix is small and unambiguous: resolve the destination, assert it is inside a proposals directory,
reject `..` and symlinked escapes, and add a test asserting refusal on a protected path.

There is also no `CODEOWNERS` file — `.github/` contains only `workflows/`. Repository-level
protection of `docs/governance/state/**` and `docs/governance/change-requests/**` is cheap and would
provide a second, independent layer. I would add it in the same change.

### T-F04 · Non-blocking · The coverage gate is real for two modules and nominal for four

From `build.gradle.kts`:

| Module class | Line floor | Branch floor |
|---|---|---|
| `libs/*` | 80% | 70% |
| `services/1sb-integration-service` | 90% | 70% |
| **every other service** | **50%** | **none** |

So `bank-persistence-service` (45 main files, 7 test), `identity-authorization-service` (20:4),
`workforce-access-bff` (19:4) and `identity-provider-adapter-service` (8:2) are gated at 50% line
with **no branch floor at all**. `jacocoTestCoverageVerification` will pass on those modules while
proving very little.

This is honest interim policy — QA-002 with QA-003 package gates pending — and it is correctly
documented rather than hidden. It is not a defect in CR-010. But it means the claim "coverage gates
green" carries much less weight than it sounds like for four of six modules, and S08-G3 should not
be marked met on it. Swapnali owns the threshold; I am flagging the gap between the mechanism and
what it actually proves.

### T-F05 · Non-blocking · The pipeline is one serial job with a 30-minute ceiling

`timeout-minutes: 30`, one job, `--no-daemon`, no module parallelism, no `concurrency` group.
NFR-ENG-01 targets p95 PR feedback under 10 minutes. Today's build almost certainly fits, so this is
not urgent — but the ceiling is set three times the target, and S08-E01-S07 exists because a
pipeline slower than 10 minutes gets bypassed in practice.

**Fix, in order of value:** add a `concurrency` group with `cancel-in-progress: true` so superseded
pushes stop burning runners; measure actual p95 over the first 20 runs before optimising anything
else. Do not parallelise on speculation.

### T-F06 · Non-blocking · Nothing beyond unit tests runs

`application-ci.yml` runs `test`. There is no Testcontainers stage, no WireMock harness, no contract
test, no E2E, no performance smoke. All of those are S08-E03 deliverables and CR-010 does not claim
them — PLAN-001's `out_of_scope` is honest about it. I record it so that nobody reads "application
CI exists" as "the application is tested". The test-to-main file ratios are 29% for the 1SB service
and 16% for persistence.

### T-F07 · Non-blocking · `chmod +x gradlew` in the workflow

`gradlew` is committed with the executable bit set. The `chmod` is harmless and is a common defensive
idiom, but it is a line that will outlive its reason. Drop it or comment why it is there.

### T-F08 · Non-blocking · No image build, no SBOM, no scanning

No container image is built, scanned or published by CI, despite a `Dockerfile` at the root and
S08-E01-S04 requiring versioned artefacts tagged with the commit SHA. S08-E04 (secret, SAST, SCA,
image scanning, SBOM) is entirely absent. Both are S08 scope and correctly out of CR-010's; Deepali
owns the security half.

### T-F09 · Non-blocking · Governance tooling quality is good

Worth saying, because I am usually the one complaining about scripts. `ci-checks.py` is well
structured — numbered independent checks, a single `fail()`/`ok()` reporting convention, and check 5
asserts routing closure over the work-type enum plus destination existence, which is a genuinely
non-trivial invariant to enforce mechanically. `FreshnessCheck.java` runs as a single-file source
program on the documented JDK baseline with no build step and no dependencies, which is the right
call for something an agent runs at session start. `validate_policy()` in `autopilot.py` refuses
rather than warns. This is competent work and I would not want the findings above read as a negative
assessment of it.

---

## 5. Board 2 checklist

| # | Check | Result |
|---|---|---|
| T1 | Feasible with the current stack and skills? | **Yes.** Python 3.12 + PyYAML + jsonschema for governance; JDK 21 for freshness and the application build. No new runtime |
| T2 | Error paths defined, not just the happy path? | **Yes** for the controller — `AutopilotRefusal` and `jsonschema.ValidationError` are caught and return exit 1 with `REFUSED:`. **No** for the `--output` write path, which has no failure handling at all (T-F03) |
| T3 | Transaction and concurrency boundaries explicit? | **N/A** — no runtime data path is touched |
| T4 | Backward compatibility stated and correct? | **Yes.** Compatibility documents become redirects and stable entry paths are retained; the repository-wide link check backs the claim |
| T5 | Complexity proportional? | **Yes.** No framework, no service, no new runtime dependency for a governance control plane |
| T6 | Duplicates something that exists? | **No.** It removes duplication — persona compatibility files collapse to redirects |
| T7 | `files_expected` plausible and complete? | **Mostly.** PLAN-001 carries two honest variance-log entries covering CR/register/test additions and semantic reconciliation. That is the variance process working, not failing |
| T8 | Is the rollback real? | **Yes.** Revert the branch; no runtime data, no external tracker state, no production configuration is changed. This is one of the few changes in this repository where "revert the commit" is genuinely sufficient |

---

## 6. Conditions

| # | Condition | Blocking | Owner | Required by |
|---|---|---|---|---|
| **E-01** | Constrain `autopilot.py --output` to a proposals directory with `..`/symlink rejection, and add a refusal test for `docs/governance/state/**` and `docs/governance/change-requests/**`. Add a `CODEOWNERS` file covering the same paths. | ✅ | Amit | Before CR-010 binds |
| **E-02** | Move autopilot safety tests onto committed fixtures; keep one smoke test that the live evidence file loads and validates. | ✅ | Amit + Swapnali | Before CR-010 binds |
| **E-03** | Make `application-ci.yml` usable as a required status check (T-F01). My recommendation: drop the `pull_request` path filter. | ✅ | Amit + Shivanshi | Before S08-G2 is claimed |
| **E-04** | Add a `concurrency` group with `cancel-in-progress: true` to both workflows. | — | Amit | With CR-010 |
| **E-05** | Record the actual p95 PR feedback time over the first 20 runs before any pipeline optimisation. Measure, then decide. | — | Amit + Shivanshi | S08 |
| **E-06** | Do not mark S08-G3 met on the current floors. Four of six modules are gated at 50% line with no branch floor (T-F04). Threshold is Swapnali's call. | — | Swapnali | Before GATE-S08 |
| **E-07** | Track image build, SBOM and scanning (T-F08) as S08-E01-S04 and S08-E04 work items rather than leaving them implicit in the stage file. | — | Amit + Deepali | S08 |

---

## 7. Record

```yaml
review:
  board: TECHNICAL
  role: R3
  reviewer: "Amit / Technical Head"
  reviewer_type: AGENT
  ai_simulated: true
  drafted_by: "Architecture agent (Mahesh persona)"
  self_review: false
  change_request: CR-010
  plan: PLAN-001
  change_tier: T4
  decision: APPROVED_WITH_CONDITIONS
  must_fix:
    - "autopilot --output must be constrained to a proposals directory, with a refusal test (T-F03)"
    - "autopilot safety tests must run on fixtures, not live gate evidence (T-F02)"
    - "application-ci.yml must be usable as a required status check (T-F01)"
  conditions: [E-01, E-02, E-03, E-04, E-05, E-06, E-07]
  should_fix:
    - "drop the chmod +x gradlew line or comment why it is there"
    - "add CODEOWNERS in the same change as E-01"
  evidence:
    - "read both workflow files in full, including triggers, path filters and permissions"
    - "read autopilot.py end to end including the --output write path at the CLI layer"
    - "read all five tests in test_autopilot.py; confirmed load_bundle() reads live state"
    - "read ci-checks.py checks 2, 3, 5, 6, 8"
    - "read build.gradle.kts lines 60-125; recorded per-module coverage floors"
    - "confirmed .github/ contains only workflows/ — no CODEOWNERS"
    - "confirmed .gitignore excludes **/build/"
  notes: >
    The application CI workflow is the highest-leverage change in this CR and it is correctly
    minimal. It is a mechanism, not evidence: S08-G1 needs a run history at E4. Governance tooling
    quality is good and the findings above should not be read as a negative assessment of it.
  signature_status: "AI-DRAFTED — mandatory human signature outstanding"
  date: 2026-08-16
```

---

**Persona:** Amit — Technical Head · R3 / Board 2
**Drafted by:** Architecture agent under the Mahesh persona, as an input to Board 2
**signature_status:** `AI-DRAFTED — mandatory human signature outstanding`
