# GitLab CE repository structure — backend cutover

**Owner:** Mahesh — Principal Insurance Platform Architect (Board 1 · `R2`)  
**Origin:** `SUG-20260909-glc` · [`ADR-020`](../architecture-review/ADR-020-gitlab-ce-backend-monorepo.md) (Proposed)  
**Purpose:** How to place this Gradle tree on the bank GitLab Community Edition **without** a GitHub↔GitLab integration.  
**Status:** Proposed — human Architecture / SRE / Compliance sign-off outstanding  
**Horizon:** **H0 now** (S08/S09 cutover) · per-service GitLab projects are **parked** · first copy wave is **libs + 1SB path** (`SUG-20260910-w1s`)

> This file used to propose one GitLab project per Gradle module. That is the **wrong grain for GitLab CE at S08**. The three subgroups already created under `Insurance` are the right team/layer split. Do not explode `nip-backend` into ~20 projects during migration.

## 1. Recommendation in one screen

**Keep the backend as a Gradle monorepo** in the GitLab project you already created. Split **by team and stack**, not by microservice.

```text
Insurance/                          (existing top-level group — private)
├── platform/
│   └── nip-governance              # CI templates + group policy for DevOps — not a second AIGEM tree
├── frontend/
│   └── nip-app                  # Flutter NIP-APP — frontend team owns; out of this migration
└── backend/
    └── nip-backend              # THIS GitHub repository (libs + all services + AIGEM that CI enforces)
```

| Cut | Verdict | Why |
|---|---|---|
| Frontend vs backend | **Polyrepo (already done)** | Different team, stack, cadence. `ADR-015` / `nip-app`. |
| Governance vs backend code | **Do not split AIGEM out of `nip-backend` at H0** | `FreshnessCheck`, `context-load.py` and `./gradlew test` are one GATE-S08 pipeline. |
| One GitLab project per microservice | **Park** | Most modules are skeletons; libs are `project()` dependencies; GATE-S08-G1 needs one pipeline that builds every module. |
| Module-by-module *copy* into `nip-backend` | **Yes, if you cannot land one bundle** | Wave 1 = all six `libs/*` **in one go** plus `bank-persistence-service` + `1sb-integration-service`. Later services copy into **this same project** when mature (`SUG-20260910-w1s`). |

**Mahesh (draft, not T4):** severity `A2` if someone still creates 20 empty GitLab projects before S09 is green — recoverable, expensive. Severity `A0` if a GitHub↔GitLab integration is turned on against the compliance constraint.

## 2. What you already created — keep it

The GitLab CE group `Insurance` already has the only projects this increment needs:

| GitLab path | Owner | What belongs there at H0 |
|---|---|---|
| `insurance/platform/nip-governance` | Platform / DevOps | GitLab CI `include` templates, group README, contribution/branch policy. **Not** a copy of `docs/governance/`. |
| `insurance/frontend/nip-app` | Frontend team | Flutter client. Backend does not push here. |
| `insurance/backend/nip-backend` | Backend (this repo) | Entire current GitHub tree: `libs/`, `services/`, `gradle/`, `docs/`, `scripts/`, `templates/`. |

Do **not** create `platform-common`, `ws2-iam`, `ws3-domain`, … as GitLab **projects** now. Those names stay as **logical ownership folders** inside `nip-backend` (CODEOWNERS + catalogue `gitlab_group`). They become GitLab subgroups only if a later extract unparks.

## 3. Why a backend monorepo on GitLab CE — and why not 20 repos

This repository **is already** a Gradle monorepo (`settings.gradle.kts`: 6 shared libs + 20 services). GATE-S08 still requires:

- `S08-G1` — CI builds and tests **every module on every PR** ([`.github/workflows/application-ci.yml`](../../../.github/workflows/application-ci.yml) deliberately dropped path filters so a skipped check cannot fake a green merge).
- `S08-G2` — merge to `main` impossible without that green pipeline.
- `S08-G9` — pipeline feedback under 10 minutes at p95 (one Gradle invocation, not 20).
- `S08-G10` — a new engineer clones **one** repo and runs `./gradlew test`.

### GitLab CE (Community Edition) — what is easy vs painful

| Concern | One `nip-backend` | ~20 GitLab projects |
|---|---|---|
| **CI** | One `.gitlab-ci.yml`. `include: project:` from `nip-governance` works on CE. | 20 files, 20 protected-branch setups, or a brittle trigger mesh. CE has **no merge-request dependencies** and **no merge trains** (Premium). |
| **Shared libs** | `implementation(project(":libs:bank-common-error"))` as today. | Must publish Maven coordinates to GitLab Package Registry on every lib change, then bump 20 consumers. New pipeline + versioning **during foundation**. |
| **ArchUnit / coverage** | One JaCoCo + ArchUnit run, as GATE-S08 needs. | Cross-repo breakage is invisible until a coordinator job exists (you would be inventing a monorepo CI anyway). |
| **Approvals** | CE can require MR approvals and protect `main`. Path `CODEOWNERS` is **advisory** on CE; **required** CODEOWNERS / multiple approval rules are Premium. | Same CE gap, multiplied by 20 projects. You cannot encode “DBA + Engineering for persistence” as CE approval rules. |
| **Access** | Group `backend` for backend engineers; `frontend` cannot push here. | 20 membership matrices. |
| **Skeletons** | Harmless empty modules in one tree. | 16 near-empty projects to secure, scan and forget. |
| **Undo** | Extracting one service later is `git subtree` / `git filter-repo` when that service actually ships alone. | Unsplicing 20 repos back into a composite Gradle build is the expensive direction. |

Anti-over-engineering ([`16 §6`](../../governance/16-DECISION_MODEL.md#6-anti-over-engineering-tests)): **X3** (cheap later) and **X5** (stage) fail for a per-service split now. **X6** (smallest sufficient) is the three-project layout you already have.

`ADR-019` created skeletons so **engineers see the topology**. It did **not** require a GitLab project per skeleton. Catalogue `gitlab_group` remains the join key for ownership, not a project-creation script.

## 4. What goes in `nip-backend` (this GitHub repo)

Push **this repository as-is** (history included). There is no Flutter app in this tree; frontend is already `nip-app`.

| Path | Stays in `nip-backend` | Why |
|---|---|---|
| `libs/`, `services/`, `gradle/`, `gradlew*` | Yes | Gradle composite |
| `docs/`, `scripts/governance`, `scripts/context` | Yes | GATE-S08 governance CI is coupled to the code |
| `templates/microservice-skeleton/` | Yes | Scaffold for later modules |
| `.github/workflows/` | Copy in, then **stop using** once GitLab CI is equivalent | Do not leave GitHub Actions as the merge gate after cutover |

`workforce-access-bff` stays here. It is an edge BFF **module**, not a frontend repo. The Flutter team consumes its API; they do not own the Java.

## 5. What goes in `nip-governance` (and what does not)

Use the empty `nip-governance` project as the **CI/policy** repo DevOps can change without a Java clone:

```text
insurance/platform/nip-governance/
├── README.md                      # how to include templates; who owns runners
├── ci/
│   ├── gradle-java21.gitlab-ci.yml
│   ├── secret-scan.gitlab-ci.yml
│   └── governance-docs.gitlab-ci.yml
└── policy/
    └── protected-branch-baseline.md
```

`nip-backend` then starts with:

```yaml
include:
  - project: insurance/platform/nip-governance
    file: /ci/gradle-java21.gitlab-ci.yml
```

Do **not** move AIGEM, ADRs, or `CURRENT-STATE.yaml` into `nip-governance`. That would split the SSOT from the pipeline that proves it (`S08-G1` / `S08-G10`).

## 6. Manual cutover — no GitHub integration

**Forbidden** (compliance): GitLab GitHub importer, GitHub OAuth on GitLab, pull/push **mirroring** of GitHub, “CI/CD for GitHub”, GitHub Actions that push or trigger GitLab, webhooks between `github.com` and `gitlab-ce.au.bank.in`.

**Allowed:** a human (or a bank-controlled workstation) copies git objects that are **already on disk**, then pushes to GitLab over the bank network.

### Preferred — one bundle, full history

On a machine that already has this clone (no live GitHub API call required if the clone is current):

```bash
# 1. Produce an opaque git bundle (history + tags + branches)
git bundle create /secure-transfer/nip-backend.bundle --all
git bundle verify /secure-transfer/nip-backend.bundle
```

Move the bundle via the bank-approved channel (file share / SCP / encrypted USB — **not** a GitHub webhook).

On a machine that can reach GitLab CE:

```bash
git clone /secure-transfer/nip-backend.bundle nip-backend
cd nip-backend
git remote remove origin    # if the bundle encoded a GitHub URL, drop it
git remote add origin git@gitlab-ce.au.bank.in:insurance/backend/nip-backend.git
# or https://gitlab-ce.au.bank.in/insurance/backend/nip-backend.git

git push -u origin --all
git push origin --tags
```

Empty the GitLab project first if it already contains a README commit, or push to a `migration/*` branch and fast-forward `main` after review.

### If policy forbids importing GitHub history

Create a **new root commit** on GitLab with the same tree (clean-room copy). Record the reason in the MR. Audit then starts at cutover; you lose blame. Only do this if Compliance (`R9`) requires it in writing.

### Module-by-module — still one GitLab project (`SUG-20260910-w1s`)

If you cannot land the whole tree in one push, copy **into `nip-backend`**, not into new projects. The owner sequence is: **all common libs in one go, then the 1SB path, then one service at a time when it is mature.**

`1sb-integration-service` has **no datasource**. Its job store is HTTP to `bank-persistence-service`. Wave 1 therefore always includes persistence. Do not copy 1SB alone.

| Wave | Content | Maturity / why |
|---|---|---|
| 0 | `gradlew`, `settings.gradle.kts` (only the modules already copied), `build.gradle.kts`, version catalog, `.gitignore` | Build must exist before modules |
| 1 | All six `libs/*` **together** + `services/bank-persistence-service` + `services/1sb-integration-service` | First implemented 1SB path. Libs stay `project()` dependencies — do not invent a Maven registry for this wave |
| 2+ | **One** further service per GitLab MR, when it is mature | Mature = catalogue `status: implemented` (or that module has GATE evidence). Next implemented candidates today: `identity-provider-adapter-service`, `identity-authorization-service`, `workforce-access-bff` — still **one MR each**, not a new GitLab project |
| Last before GitHub archive | Remaining `services/*` skeletons + `docs/`, `scripts/`, `templates/` | Skeletons may wait on GitHub until mature **or** until GitHub is archived. Archive requires the rest of the tree in `nip-backend` so GATE-S08-G1/G10 still have one clone |

On GitLab, `settings.gradle.kts` `include(...)` lists **only modules that have been copied**. Do not leave `include("services:lead-service")` pointing at a missing directory.

Each wave is a normal GitLab MR on `nip-backend`. DevOps does **not** create `insurance/backend/<service>` projects for these waves. That extract stays parked (`SUG-20260909-glc` extract).

### After the first successful push

1. Developers drop the GitHub remote from local clones (`git remote remove origin` / retarget).
2. GitLab: **Settings → Repository → Protected branches** on `main`: no force-push, no direct push, MR required, ≥1 approval (CE can do this; it cannot do multi-rule CODEOWNERS).
3. GitLab: disable **Mirroring**, **GitHub integration**, and any “external repo” CI.
4. Recreate secrets in GitLab CI variables / the bank vault. **Do not copy GitHub Actions secrets.**
5. Shivanshi / DevOps replace `.github/workflows/*` with GitLab CI that still runs: `./gradlew build test jacocoTestReport jacocoTestCoverageVerification`, gitleaks, SAST, SCA, `python3 scripts/governance/ci-checks.py`. Path filters on the test job would re-open `S08-G2` — do not add them.
6. Keep GitHub **read-only archive** only if Compliance agrees; otherwise unhook it. Dual-write is a compliance leak.

## 7. Logical ownership inside `nip-backend` (not GitLab projects)

Catalogue [`backend-service-catalog.yaml`](./backend-service-catalog.yaml) `gitlab_group` values stay as **CODEOWNERS paths** / future extract names:

| Logical group | Persona | Gradle modules (today) |
|---|---|---|
| `platform-common` | Amit + Aarti | `libs/*`, `bank-persistence-service` |
| `ws2-iam` | Deepali + Amit | `identity-provider-adapter-service`, `identity-authorization-service`, `workforce-access-bff` |
| `ws3-domain` | Rajal + Mahesh | customer, lead, consent, suitability, catalogue, journey, quotation, proposal, payment, policy |
| `ws3-integration` | Mahesh + Amit | `integration-hub-service`, `1sb-integration-service`, `direct-insurer-adapter-service` |
| `ws3-platform` | Shivanshi + Shailja | audit, notification, reporting, admin-config |

Suggested `CODEOWNERS` (advisory on CE):

```text
/libs/                          @insurance/backend-platform
/services/bank-persistence-service/  @insurance/backend-platform
/services/identity-*/           @insurance/backend-iam
/services/1sb-integration-service/   @insurance/backend-integration
/docs/governance/               @insurance/platform-architects
```

CE will **not** enforce those as required reviewers. Use MR templates + a human checklist until Premium, or until DevOps adds a custom CI label check.

## 8. CI policy sketch (one project, path-aware *jobs*, not skipped gates)

| Policy | `nip-backend` (H0) |
|---|---|
| Merge to `main` | Protected branch + required pipeline success (maps `S08-G2`) |
| Required jobs | Java 21 Gradle `build`+coverage, secret scan, SAST, SCA, governance `ci-checks.py` |
| Path filters | **Forbidden on the Gradle test job** (same reason as GitHub: a skipped required check is not a gate) |
| Reviewers | Engineering for `services/`+`libs/`; Architect for `docs/platform/`+ADRs |
| Deploy to UAT | Not this increment (S09). No per-service deploy pipeline yet |
| ArchUnit | Still in `./gradlew test` |

Optional later: child pipelines that **also** run a module’s tests for faster feedback, **in addition to** the full Gradle job — never instead of it, while `S08-G1` is open.

## 9. When a per-service GitLab project becomes on-stage (parked)

Unpark only when **all** of these are true (re-triage; do not auto-split):

1. S09 platform foundation is green (runners, Maven registry, CI templates in `nip-governance`).
2. Shared libs are published to **GitLab Maven Package Registry** with a versioning policy Amit owns.
3. The extracted service has its **own release cadence** (not “it is a bounded context on a diagram”).
4. GATE-S08-G1 is either closed or explicitly re-specified for a coordinator pipeline.

Until then, creating those projects is SF3. Recorded as parked from `SUG-20260909-glc`.

## 10. Edge / frontend (out of this increment)

| Component | GitLab | Notes |
|---|---|---|
| NIP-APP (Flutter) | `insurance/frontend/nip-app` | Frontend team. Backend does not migrate it. |
| Customer BFF | not created | R1 (`out_of_scope_now`) |
| `workforce-access-bff` | **inside** `nip-backend` | Java module; not `nip-app` |

## 11. Ports (unchanged)

Ports `8080`–`8084` are reserved for implemented services. WS-3 skeletons use `8090`–`8105` per [`backend-service-catalog.yaml`](./backend-service-catalog.yaml).

## 12. Next steps after human approval of `ADR-020`

1. **Do not** create further GitLab projects for individual services.
2. Confirm `nip-backend` is empty (or only a README) and private.
3. DevOps: CI templates in `nip-governance`; protected `main`; no GitHub features enabled.
4. Backend owner: bundle-and-push this tree (section 6).
5. DevOps: GitLab CI equivalent of application + security + governance workflows; then archive GitHub.
6. Kalpana: treat GitHub as read-only after the first green GitLab pipeline on `main`.
