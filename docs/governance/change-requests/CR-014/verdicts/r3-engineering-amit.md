# `R3` — Engineering · Position on CR-014

**Change request:** [CR-014](../../CR-014-gitlab-estate-migration.md) · **Plan:** [`GLM-001`](../../../../platform/gitlab-migration/GLM-001-migration-plan.md)
**Board:** 2 — Technical · **Role:** `R3`
**Persona:** Amit — Technical Head / Principal Engineering
**Reviewer type:** `AGENT` · **Self-review:** false · **Change tier:** `T4` · **Date:** 2026-08-29

> **`signature_status: AI-DRAFTED`** — Engineering is not one of the three mandatory human T4
> signatures, but this position does not substitute for one either.

> ## Drafted verdict: `APPROVE-WITH-CONDITIONS`
> The split itself is low-risk and I can say so from the build files rather than from optimism. The
> risk is concentrated in the CI re-implementation, and specifically in three mechanisms that are
> currently load-bearing for `GATE-S08` and are easy to lose in translation.

---

## 1. What I reviewed

`settings.gradle.kts` · `build.gradle.kts` (root) · `apps/rm-workspace-app/pubspec.yaml` ·
`.github/workflows/application-ci.yml` in full, including its trigger-design and branch-protection
comments · the coverage-gate configuration · the ArchUnit and static-analysis position for
`S08-G4` · GLM-001 phase M7.

---

## 2. Findings

### `ENG-F01` · the split seam is clean, and that is a measured statement

`settings.gradle.kts` includes exactly ten modules: five under `libs:` and five under `services:`.
`apps/rm-workspace-app` is a Flutter application with its own `pubspec.yaml` and **no Gradle
participation at all** — it is not in `settings.gradle.kts` and nothing in the Gradle build
references it.

That means `frontend` and `backend` have no build coupling to sever. The split is a file move, and
the backend build after the split is byte-for-byte the build before it. This is the best case and we
happen to have it. I want it stated plainly so nobody budgets a week for a dependency untangle that
does not exist.

### `ENG-F02` · `rootProject.name` is a rename with a tail

IMP-12 is right that `rootProject.name = "1sb-insurance-platform"` should change during the split.
It is also not a one-line edit. The value propagates into artifact coordinates, the Gradle project
path used in CI invocations, container image names and SBOM component identifiers.

Doing it during the split is correct — every one of those consumers is being rewritten anyway. Doing
it later is a nine-repository sweep. **Condition `C-ENG-1`:** one labelled commit, and the SBOM
component-name assertion in the pipeline is updated in the same commit, or the SBOM job fails on
first run and it looks like a migration bug.

### `ENG-F03` · the three mechanisms that must survive translation

`GATE-S08` currently rests on three things implemented in CI, and each has a specific way of being
silently lost:

| Mechanism | Criterion | How it gets lost |
|---|---|---|
| `jacocoTestCoverageVerification` — libs line ≥ 80% / branch ≥ 70%, services on the interim floor | `S08-G3` | A `java-test.yml` component that runs `test` and publishes a coverage *report* without running the *verification* task. Green pipeline, no gate |
| ArchUnit rules — 1SB specifics confined to `adapter.onesb.*`, no Flyway/JPA in the integration service | `S08-G4` | ArchUnit tests live in the normal test source set. A component that filters test tasks by name or module drops them without an error |
| No-PII-in-logs automated test | `S08-G7` | Same failure mode as ArchUnit |

All three fail **silently and green**, which is the only failure mode worth writing a condition
about. `C-ENG-2` requires each to be asserted present in the new pipeline by a test that fails when
the mechanism is absent — not by inspection.

### `ENG-F04` · affected-component detection is the IMP-4 failure mode wearing a different hat

I wrote the comment in `application-ci.yml` explaining why the `paths:` filter had to be removed:
*a required status check that is skipped by a path filter never reports a conclusion*, and the gate
either blocks forever or passes vacuously. Both defeat `S08-G2`.

GLM-001 M7.9 introduces affected-component detection for the backend. That is the right long-term
design — 20-40 services must not all rebuild for a one-service change — and it is **the same
mechanism I just removed**, reintroduced deliberately. GitLab's `rules:` and `needs:` behave the same
way: a job that does not run does not report.

The resolution is not to abandon affected-component execution. It is to keep the **gating** jobs
outside the affected-set logic. Concretely: gating jobs (build-all-signal, coverage verification,
ArchUnit, secret/SAST/SCA) always run; the *matrix* of per-service builds and tests inside them is
what narrows. The gate is a fixed set of always-reporting jobs; the work underneath is dynamic.

`C-ENG-3` makes that binding, and `C-ENG-4` adds the nightly full build, because affected-component
detection has a real blind spot: a change in `libs/bank-common-*` affects consumers the diff does not
name, and detection tuned to be fast will eventually get that wrong.

### `ENG-F05` · I support the M0.3 decision going to Delivery, and I have a position

The gate-evidence question (IMP-7) is put to Kalpana and me. My engineering position, which is an
input and not the decision:

Four criteria re-open on cutover regardless of what we do. Closing `S08-G1`/`G2` on GitHub first
therefore buys a *closed gate on a platform we are leaving* — it is real evidence, but it is evidence
about infrastructure the bank does not own, and we would rebuild all of it within weeks. I would
rather spend that effort once, on GitLab. That argues for **Option B — re-evidence on GitLab** and
for stopping GitHub Actions investment now beyond keeping the build green.

The cost of Option B is honest and should be stated: `GATE-S08` stays open across the migration, and
nobody should be surprised by that in six weeks.

---

## 3. Conditions

| ID | Condition | Must be true before |
|---|---|---|
| `C-ENG-1` | `rootProject.name` change in one labelled commit, with SBOM component-name assertions updated in the same commit | GLM-001 M5.3 |
| `C-ENG-2` | Coverage verification, ArchUnit and the no-PII-in-logs test each asserted present in the new pipeline by a check that fails when absent | GLM-001 M7.11 |
| `C-ENG-3` | Gating jobs always run and always report; affected-component logic narrows the matrix *inside* a gating job, never whether the job runs | GLM-001 M7.9 |
| `C-ENG-4` | Nightly full-platform build in place before affected-component detection becomes the default path | GLM-001 M7.9 |
| `C-ENG-5` | Backend build verified green from the split clone before the freeze lifts — `./gradlew test` from a fresh checkout, not from the rehearsal working tree | GLM-001 M5.10 |

---

## 4. What I am not deciding

- **Not** the M0.3 gate-evidence decision — `ENG-F05` is my input; Kalpana convenes and the boards decide.
- **Not** whether the resulting evidence is sufficient — Swapnali's, and I do not weaken it.
- **Not** operational readiness on the new platform — Shivanshi's.
- **Not** Architecture boundaries. `C-ARC-1` binds my implementation and I am not negotiating it.
- **Not** security controls. If a control is difficult to implement in GitLab CI that is my problem
  to solve, not a reason to remove it.
