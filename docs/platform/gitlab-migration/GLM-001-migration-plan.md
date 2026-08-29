# GLM-001 — GitHub → GitLab estate migration plan

**Workstream:** WS-3 · **Stage:** S08 Engineering Foundation with S09 Platform Foundation overlapped
**Origin:** [`SUG-20260829-glm`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20260829-glm--github-to-gitlab-estate-migration)
**Persona:** Shivanshi — SRE (Board 7 · `R10`). Cross-persona calls are named per item; none are taken here.
**Status:** `AI-DRAFTED`. Plan only — nothing in this document has been executed.

> **Freshness disclosure.** `java scripts/governance/FreshnessCheck.java` exits `1` (WARNINGS):
> `state_as_of` is 19 days old (2026-08-10), refresh due 2026-09-09. Work may be admitted; the
> warning is disclosed here because every stage-fit and priority judgement below rests on that state.

---

## 1. What is actually being migrated

### 1.1 The origin, measured

| Fact | Value | Why it matters |
|---|---|---|
| Repository | `Mahesh38/bank-insurance-platform` (personal GitHub) | Single origin, single owner, no org controls |
| Commits · tags · remote branches | 273 · **0** · **81** | No tag means **no rollback anchor exists today** |
| Pack size | ~14.9 MiB (`.git` 16 MB) | Small. Mirroring is minutes, not hours — size is not the risk |
| `docs/` | ~16 MB, 441 routed files | Larger than all source combined, and **has no home in the target topology** |
| Source | `apps/rm-workspace-app` (Flutter), `services/` ×5, `libs/` ×5 | Frontend and backend share one history and one Gradle build |
| CI | 3 GitHub Actions workflows | `application-ci.yml`, `governance.yml`, `security-scanning.yml` |
| Deploy target today | `render.yaml` — Render.com, one container, two JVMs | Not AWS, not EKS, not per-service images |
| Terraform / GitOps / contracts | **none present** | Five of the eight target projects are greenfield |

### 1.2 Origin → target project mapping

| Target project (spec §2.2) | Source | Nature |
|---|---|---|
| `product/frontend` | `apps/rm-workspace-app` | **History split** (`git filter-repo --path`) |
| `product/backend` | `services/`, `libs/`, root Gradle, `Dockerfile`, `config/` | **History split** (complement) |
| `product/contracts` | — | **Greenfield seed** — no OpenAPI/AsyncAPI exists yet |
| `delivery/infrastructure` | — | **Greenfield seed** — no `*.tf` exists |
| `delivery/gitops` | — | **Greenfield seed** |
| `engineering/ci-components` | 3 GH Actions workflows, ported | **Greenfield seed, translated** |
| `governance/security-policies` | `.gitleaks.toml`, `.trivyignore` | **Greenfield seed, translated** |
| `governance/gitlab-bootstrap` (spec §4.1) | — | **Greenfield** |
| **`governance/platform-governance`** ← *proposed 8th/9th project* | `docs/`, `scripts/{governance,context,lifecycle}`, `AGENTS.md`, `CLAUDE.md` | **History split** — see **IMP-1** |

> The requirements document names seven projects and recommends `gitlab-bootstrap` as an eighth.
> This plan proposes a **ninth**, `platform-governance`, for reasons set out in **IMP-1**.

---

## 2. Improvements and corrections — raise before executing

These are SRE findings against the approved baseline. Per [BOOT.md §1](../../context/BOOT.md) none is
implemented in the turn it is raised; each is routed to its deciding authority. Severity is Board 7
operational severity `O0`–`O3`, which is **not** AIGEM `P1`–`P5`.

### IMP-1 · `docs/` and the governance tooling have no home — `O1`
**Finding.** The seven-project topology has nowhere to put 16 MB / 441 files of `docs/`, the AIGEM
registers, `scripts/context/`, `scripts/governance/`, `AGENTS.md` and `CLAUDE.md`. Three placements
are possible and two are wrong:

* *In `backend`* — wrong. It governs all nine repositories, and every governance MR would then run
  the Java build. It also makes the frontend team a guest in the backend repo to read its own rules.
* *Split across repos* — wrong. `DOC-MAP.yaml`, `context-load.py` and `FreshnessCheck.java` assume
  one tree; splitting them destroys the resolver every agent session depends on.
* *A dedicated `governance/platform-governance` project* — **recommended.**

**Why it is not gold-plating.** `S08-G8` (standards published and adopted) and `S08-G10` (a new
engineer ships within a week) both fail if the operating model is unreachable or buried. Every agent
session begins with `FreshnessCheck` + `BOOT.md` + `context-load.py`; if that tree is not a
first-class repository with its own CI, the governance model stops being enforceable on day one.
**Route:** Mahesh (Architecture) — a spec exception under Appendix C. It *adds* a boundary, weakens none.

### IMP-2 · `bank-persistence-service` contradicts spec §3.3 — `O1`, not SRE's to resolve
**Finding.** The baseline states each bounded context owns its write model and that a generic shared
persistence service for all domains is to be avoided. `AGENTS.md` describes `bank-persistence-service`
as *"platform-common persistence (Flyway + JPA + `/internal/v1`); **owns the DB for all consumers**"*.
That is the exact pattern the spec prohibits, and the migration will publish it into the bank estate
as an approved-looking day-1 asset.
**Route:** Mahesh (boundaries) + Aarti (persistence) as a `CR-###`. **Explicitly not** to be
"corrected" inside the migration — a topology change smuggled into a repository move is untraceable.
Migrate it as-is; land the CR in parallel.

### IMP-3 · CodeQL does not port — `O1`, blocks `S08-G5`
**Finding.** `security-scanning.yml` gates on CodeQL (`SAST (CodeQL, Java)`) with fail-on-critical/high
logic. CodeQL is GitHub-only. GitLab SAST is a different engine (Semgrep + SpotBugs for Java) with
different rules, different severities and a different report format. This is a **re-implementation with
re-baselined findings**, not a port, and the finding set will differ on day one.
**Compounding unknown:** MR-blocking security policies (Scan Result Policies / Security Approval rules)
require **GitLab Ultimate**. Edition is an open enterprise input (spec §5) and is the single most
consequential unknown in the programme. If the bank is on Premium, `S08-G5` needs a different mechanism
(a scanner run as an ordinary blocking job, findings triaged manually) and Deepali must accept that.
**Route:** Deepali (Security) owns the control outcome; SRE owns the mechanism. Resolve in Phase M1.

### IMP-4 · "Required status checks by name" has no GitLab analogue — `O1`, blocks `S08-G2`
**Finding.** `application-ci.yml` documents the S08-G2 mechanism as four **named** GitHub required
checks. GitLab has no equivalent: it offers *"Pipelines must succeed"* over the whole pipeline, plus
`allow_failure`, plus MR approval rules. The gate must be redesigned as: one required pipeline, every
gating job `allow_failure: false`, and no `rules:` path filter that can skip a gating job — GitLab
skipping behaves the same way GitHub's path filter did, and the workflow's own comment already
identifies that as the failure mode.
**Route:** SRE designs, Amit (Engineering) confirms build correctness, Swapnali confirms evidence.

### IMP-5 · Do not conflate the SCM move with the Render → EKS move — `O2`
**Finding.** `render.yaml` deploys one combined container running two JVMs behind a single public
port. The baseline requires per-service immutable images (`.../backend/quotation-service:<sha>`,
production by digest) promoted without rebuild onto EKS. That is a **runtime re-platform**, materially
larger than the SCM/CI migration, and it drags in AWS accounts, EKS, GitOps and OIDC.
**Recommendation.** Migrate SCM + CI first and keep Render as a throwaway demo target reachable from
GitLab CI. Treat EKS as S09 work with its own gate. Attempting both at once puts the gate-evidence
recovery (**IMP-7**) and the runtime change on the same critical path.
**Route:** SRE recommends; Kalpana (Delivery) sequences.

### IMP-6 · Secret hygiene is a **pre**-migration blocking gate — `O0`
**Finding.** `security-scanning.yml` runs full-history gitleaks only on a schedule, non-blocking. Once
273 commits across 81 branches are mirrored into the bank estate, a credential in history stops being
a personal-repo problem and becomes a bank security incident with bank disclosure obligations. This is
the one point where "preserve history exactly" (spec §7.2) and "do not import a leak" genuinely
conflict, and history wins only if the scan is clean.
**Also:** `.gitleaks.toml` carries allowlists written for a personal repository. They must be
re-reviewed against bank rules before they are inherited, not after.
**Sequence, non-negotiable:** rotate any exposed credential **first**, then scrub with `git filter-repo`,
then migrate. Scrubbing before rotating leaves the live secret live and merely harder to find.
**Route:** Deepali (Security) owns the verdict; SRE executes.

### IMP-7 · Gate-evidence continuity is the largest schedule risk — `O1`
**Finding.** `GATE-S08` is open with 10/10 criteria open. Four are evidenced by the CI platform itself:
`S08-G1` (builds every module on every PR), `S08-G2` (no merge without green), `S08-G5` (scanning in
the pipeline), `S08-G9` (p95 feedback under 10 min, flake under 1%). `S08-G1` requires **E4 evidence —
a run history**. GitHub Actions run history does not migrate. Every one of those four is re-opened by
the cutover regardless of how green it is on GitHub today.
**The decision.** Either (a) close S08 on GitHub first and re-evidence afterwards on GitLab, or
(b) accept that S08 closes on GitLab and stop investing in GitHub Actions evidence now. Doing neither —
continuing to build GitHub evidence while planning to leave — is the expensive path, and it is the
default if nobody decides.
**Route:** Kalpana (Delivery) + Amit (Engineering). Required **before** Phase M3 starts.

### IMP-8 · 81 branches, 0 tags — `O2`
**Finding.** Mirroring all 81 branches imports 81 unreviewed histories into the bank estate. And with
zero tags there is no immutable rollback anchor: if cutover is reverted, there is nothing to point at.
**Recommendation.** Tag `pre-gitlab-migration` on GitHub `main` before the freeze. Migrate `main` plus a
named allowlist of demonstrably active branches; `git bundle` the remainder into retained artifact
storage with a documented restore procedure. Archived is not deleted.

### IMP-9 · `CI_JOB_TOKEN` allowlisting is a day-1 blocker, not a refinement — `O1`
**Finding.** Today one repository means no cross-project authentication. The moment `backend` consumes
`ci-components` and `contracts`, every cross-project pipeline call needs an explicit job-token
allowlist entry. This is the single most common way a monorepo split breaks CI, and it breaks it
*after* cutover, when GitHub is already read-only.
**Recommendation.** Model `gitlab_project_job_token_scope` in Terraform in Phase M3 and apply it in
Phase M6, **before** the first consuming pipeline runs — not reactively.

### IMP-10 · Bootstrap state must be isolated from application state — `O1`
**Finding.** Spec §4.5 says protect bootstrap state more strongly, without saying how. Concretely: the
bootstrap state can delete every repository in the estate, which makes it the highest-value target in
the programme.
**Recommendation.** A separate backend/bucket and a separate KMS key from `delivery/infrastructure`
state; a distinct automation identity from the app-deploy identity; `prevent_destroy` on all five
groups and all nine projects; and a documented, **tested** restore. Untested state recovery is a
belief, not a control.

### IMP-11 · Ordering traps in the Terraform itself — `O2`
Three concrete traps the baseline's sequencing rule implies but does not spell out:

1. `gitlab_project` must set `initialize_with_readme = false` **explicitly** for the two repositories
   receiving migrated history — a generated README commit forks the history before the mirror lands.
2. `default_branch` must not be set, and branch protection must not be created, before `main` exists.
   Terraform will create a protection rule for a non-existent branch and then drift on every plan.
3. Import the existing `insurance` parent group into state; never let a plan propose creating it.
   Nothing in the estate is safe if the parent group is under `create` in a plan.

### IMP-12 · Cheap wins to take while every file is being touched anyway — `O3`
* `rootProject.name = "1sb-insurance-platform"` does not match the platform or the target project name.
  It propagates into image names, Gradle paths and SBOM component names. Fix during the split, in one
  labelled commit — not "later", when it becomes a rename across nine repositories.
* Seed `service.yaml` (spec §10) per service during the split, from the service table already in
  `AGENTS.md`. Free now; a nine-repo sweep later.
* Sweep `docs/` for PII, customer data, insurer material under NDA and credentials in samples before
  16 MB of it enters a bank estate. Route findings to Shailja (Compliance) — not an SRE call.

### IMP-13 · Declare the freeze window, with a duration and an owner — `O2`
The baseline forbids operating both sides as writable sources indefinitely, but this repository is
actively worked (81 branches, agent lanes pushing). "We'll dual-write for a while" is how a migration
acquires two sources of truth permanently. Recommend a **≤48-hour hard freeze** with a named owner, an
announced start, and GitHub set read-only (archived) at cutover +24 h.

---

## 3. The task list, in execution order

Ownership: **SRE** = Shivanshi · **SEC** = Deepali · **ARCH** = Mahesh · **ENG** = Amit ·
**QA** = Swapnali · **COMP** = Shailja · **DEL** = Kalpana · **BANK** = enterprise GitLab / AWS teams.
"AI" is focused agent-hours for work an agent can complete unaided.

### Phase M0 — Governance and decisions · *nothing technical starts until these land*

| # | Task | Owner | AI |
|---|---|---|---|
| M0.1 | Triage and register the migration (`SUG-20260829-glm`) | SRE | done |
| M0.2 | Raise `CR-014` — T4 change request for the estate migration | SRE drafts · ARCH/SEC/COMP/DEL sign | 2 h |
| M0.3 | **Decide gate-evidence strategy** (**IMP-7**) — close S08 on GitHub, or re-evidence on GitLab | DEL + ENG | — |
| M0.4 | **Decide the governance-tree home** (**IMP-1**) — Appendix C exception for `platform-governance` | ARCH | 1 h |
| M0.5 | Raise a separate CR for `bank-persistence-service` vs spec §3.3 (**IMP-2**) — parallel, non-blocking | ARCH + Aarti | 1 h |
| M0.6 | **Decide Render disposition** (**IMP-5**) — keep as demo target, or retire at cutover | SRE + DEL | — |

**Exit:** `CR-014` approved; M0.3, M0.4, M0.6 decided and recorded.

### Phase M1 — Discovery · *the critical path, and it is not code*

| # | Task | Owner | AI |
|---|---|---|---|
| M1.1 | Issue the spec §5 enterprise-input questionnaire (11 inputs) and track it to closure | SRE → BANK | 2 h |
| M1.2 | GitLab base URL, **version and edition/licence** — gates **IMP-3** and every governance feature | BANK | — |
| M1.3 | Existing `insurance` group path and ID; subgroup/project creation rights | BANK | — |
| M1.4 | SSO/LDAP identity group names and IDs for the eleven logical teams (spec §5.2) | BANK | — |
| M1.5 | Runner operating model, tags, and whether production-capable runners exist | BANK | — |
| M1.6 | Terraform state standard (GitLab-managed vs enterprise S3/KMS) and the automation identity | BANK | — |
| M1.7 | Container/Package Registry availability | BANK | — |
| M1.8 | AWS account and role conventions for OIDC/STS | BANK | — |
| M1.9 | Retention and audit requirements for logs, artifacts and evidence | COMP + BANK | — |
| M1.10 | Record every unresolved input as an `ASM-###` assumption with an expiry | SRE | 1 h |

**Exit:** all 11 inputs answered or explicitly assumed with an owner and expiry. **Do not write final
Terraform before M1.2 and M1.6 land** — provider capability and backend shape both depend on them.

### Phase M2 — Pre-migration hygiene · *not in the requirements; origin-specific*

| # | Task | Owner | AI |
|---|---|---|---|
| M2.1 | **Blocking** full-history gitleaks scan across all 81 branches (**IMP-6**) | SRE | 2 h |
| M2.2 | Re-review `.gitleaks.toml` allowlists against bank rules | SEC | 1 h |
| M2.3 | If a finding is real: **rotate first**, then `git filter-repo` scrub, then re-scan | SEC + SRE | 4 h¹ |
| M2.4 | PII / customer-data / NDA sweep of the 16 MB `docs/` tree (**IMP-12**) | SRE runs · COMP rules | 3 h |
| M2.5 | Branch triage: allowlist to migrate; `git bundle` the rest with a restore procedure (**IMP-8**) | SRE + ENG | 2 h |
| M2.6 | Tag `pre-gitlab-migration` on GitHub `main` — the rollback anchor | SRE | 15 m |
| M2.7 | **Split rehearsal**: `git filter-repo` into throwaway clones ×3; verify commit counts, authorship, tree hashes | SRE | 4 h |
| M2.8 | Verify the split builds: frontend `flutter test`, backend `./gradlew test`, governance `FreshnessCheck` | SRE + ENG | 3 h |

¹ conditional on a finding. **Exit:** clean history, verified split rehearsal, rollback anchor tagged.

### Phase M3 — Bootstrap IaC · *spec Phase 1 — no apply*

| # | Task | Owner | AI |
|---|---|---|---|
| M3.1 | Create `gitlab-bootstrap` skeleton per spec §4.1 | SRE | 2 h |
| M3.2 | `versions.tf` / `provider.tf` — pin the provider to a tested range, commit the lock file | SRE | 1 h |
| M3.3 | `backend.tf` — remote state per M1.6; encryption, locking, versioning, isolation (**IMP-10**) | SRE | 2 h |
| M3.4 | Modules: `gitlab-group`, `gitlab-project`, `branch-governance`, `memberships`, `labels`, `variables`, `environments` | SRE | 8 h |
| M3.5 | Config YAML: `groups`, `projects`, `labels`, `permissions`, `environments` — data, not logic | SRE | 3 h |
| M3.6 | `job-token-scope` module (**IMP-9**) | SRE | 1 h |
| M3.7 | `prevent_destroy` lifecycle guards on 5 groups + 9 projects (spec §11.2) | SRE | 1 h |
| M3.8 | Bootstrap `.gitlab-ci.yml`: fmt → validate → IaC lint → plan → **protected manual apply** | SRE | 3 h |
| M3.9 | `scripts/`: `validate.sh`, `migrate-repositories.sh`, `seed-repositories.sh`, `verify.sh` | SRE | 4 h |
| M3.10 | Docs: `README.md` (10 required topics, spec §12.1), `operating-model.md`, `rollback.md`, `disaster-recovery.md` | SRE | 5 h |
| M3.11 | First `terraform plan` reviewed by a human — **no destructive changes** | SRE + BANK | 1 h |

**Exit:** plan is clean, reviewed, and proposes nothing destructive.

### Phase M4 — Namespace and projects · *spec Phases 2–3 · first apply*

| # | Task | Owner | AI |
|---|---|---|---|
| M4.1 | **Import** the existing `insurance` parent group into state (**IMP-11** #3) | SRE | 1 h |
| M4.2 | Create `bank-insurance` + `product` / `delivery` / `engineering` / `governance` subgroups | SRE apply · BANK approve | 1 h |
| M4.3 | Create the 7 baseline projects + `gitlab-bootstrap` + `platform-governance` — private, **empty** (**IMP-11** #1) | SRE apply | 1 h |
| M4.4 | Confirm no `default_branch` and no branch protection yet (spec §7 sequencing) | SRE | 30 m |
| M4.5 | Re-run plan: converged, zero diff | SRE | 30 m |

### Phase M5 — Migration and seed · *spec Phase 4 · the freeze window*

| # | Task | Owner | AI |
|---|---|---|---|
| M5.1 | Announce and start the **≤48 h freeze**; named owner (**IMP-13**) | DEL | — |
| M5.2 | Final `filter-repo` split → push `frontend`, `backend`, `platform-governance` with history, authorship, dates | SRE | 3 h |
| M5.3 | Fix `rootProject.name` and Gradle module paths in the backend split, one labelled commit (**IMP-12**) | SRE + ENG | 2 h |
| M5.4 | Seed `contracts`: `openapi/`, `asyncapi/`, `schemas/`, `compatibility-tests/`, `codegen/` skeleton | SRE + ENG | 2 h |
| M5.5 | Seed `infrastructure`: `terraform/environments/{dev,sit,uat,preprod,prod,dr}`, `modules/`, `policies/` | SRE | 2 h |
| M5.6 | Seed `gitops`: `applications/`, `environments/`, `clusters/{ap-south-1,ap-south-2}`, `deployment-windows/` | SRE | 2 h |
| M5.7 | Seed `security-policies` from `.gitleaks.toml` + `.trivyignore`, re-expressed as policy-as-code | SRE + SEC | 2 h |
| M5.8 | Seed `service.yaml` per backend service from the `AGENTS.md` table (**IMP-12**) | SRE | 2 h |
| M5.9 | `CODEOWNERS` in all nine repos — **group-based**, sensitive paths dual-owned (spec §6.4) | SRE + ARCH | 3 h |
| M5.10 | Verify: `main` present, history and authorship preserved, clone/push works, builds green | SRE + QA | 3 h |

**Exit:** all nine repositories populated and verified. Freeze still held.

### Phase M6 — Governance apply · *spec Phase 5 · only now does `main` get protected*

| # | Task | Owner | AI |
|---|---|---|---|
| M6.1 | Branch protection on `main`: no direct push, no force push, MR-only, no deletion | SRE | 1 h |
| M6.2 | MR settings: pipeline must succeed, discussions resolved, CODEOWNER approval where licensed | SRE | 2 h |
| M6.3 | Risk-based approval rules per spec §6.3 — **not** every governance role on every routine MR | SRE + SEC | 2 h |
| M6.4 | Memberships from enterprise identity groups (M1.4), least privilege, narrowest durable boundary | SRE + BANK | 3 h |
| M6.5 | The 24 standard group labels (spec §6.5) | SRE | 1 h |
| M6.6 | Protected environments DEV/SIT/UAT/PREPROD/PROD/DR with the approval model of spec §9.3 | SRE | 2 h |
| M6.7 | Container/Package Registry enablement per project | SRE | 1 h |
| M6.8 | Apply job-token allowlists (**IMP-9**) — **before** the first consuming pipeline runs | SRE | 1 h |
| M6.9 | Non-secret CI/CD variables; confirm no secret value lands in Terraform state (spec §4.5) | SRE + SEC | 2 h |
| M6.10 | Re-run plan: converged, zero unexpected diff | SRE | 1 h |

### Phase M7 — CI/CD enablement · *spec Phase 6 · the largest engineering block*

| # | Task | Owner | AI |
|---|---|---|---|
| M7.1 | Port `application-ci.yml` → `java-build.yml` + `java-test.yml` components (JDK 21, JaCoCo gates) | SRE + ENG | 4 h |
| M7.2 | Port gitleaks → `secret-detection.yml`; tree scan blocking, history scan scheduled | SRE + SEC | 2 h |
| M7.3 | **Re-implement** CodeQL as GitLab SAST and re-baseline findings (**IMP-3**) | SEC + SRE | 6 h |
| M7.4 | Port Trivy SCA → `dependency-scan.yml`; keep critical/high blocking | SRE + SEC | 2 h |
| M7.5 | `container-scan.yml`, `sbom.yml` (CycloneDX, preserving Java coverage assertions) | SRE | 3 h |
| M7.6 | `flutter-build.yml`, `node-build.yml`, `node-test.yml`, `docker-build.yml` | SRE | 4 h |
| M7.7 | `contract-test.yml`, `terraform-plan.yml`, `terraform-apply.yml`, `gitops-promotion.yml` | SRE | 4 h |
| M7.8 | Component versioning, `tests/`, and the consumer-facing usage docs | SRE | 3 h |
| M7.9 | Backend parent pipeline with **affected-component detection** (spec §8.1) + nightly full build | SRE + ENG | 8 h |
| M7.10 | Port `governance.yml` into `platform-governance` (FreshnessCheck, DOC-MAP, schema/link validation) | SRE | 2 h |
| M7.11 | **Redesign the S08-G2 gate mechanism** for GitLab semantics (**IMP-4**) | SRE + ENG + QA | 3 h |
| M7.12 | Measure `S08-G9`: p95 pipeline feedback < 10 min, flake < 1%, on the new runners | SRE | 3 h |

### Phase M8 — AWS OIDC · *spec Phase 7 · execution is bank-side*

| # | Task | Owner | AI |
|---|---|---|---|
| M8.1 | Design the OIDC trust: GitLab ID token claims → per-environment IAM roles | SRE | 3 h |
| M8.2 | Role segregation matrix: build / DEV / SIT / UAT / PROD / DR — prod reuses nothing lower | SRE + SEC | 2 h |
| M8.3 | Create the identity provider and roles in the bank AWS accounts | BANK | — |
| M8.4 | Prove STS temporary credentials from a pipeline; confirm **zero** static AWS keys in variables | SRE | 3 h |
| M8.5 | Document the exception path if the bank mandates static keys anywhere | SRE + SEC | 1 h |

### Phase M9 — Validation and cutover · *spec Phase 8*

| # | Task | Owner | AI |
|---|---|---|---|
| M9.1 | Execute the spec §14 acceptance checklist; record evidence per criterion | SRE + QA | 4 h |
| M9.2 | Final `terraform plan` — no unexpected drift; attach as evidence (spec §15 item 15) | SRE | 1 h |
| M9.3 | Final synchronisation from the frozen GitHub origin; verify parity | SRE | 2 h |
| M9.4 | **Declare GitLab authoritative.** Archive GitHub read-only at cutover +24 h | DEL + SRE | 1 h |
| M9.5 | Test the rollback procedure against the `pre-gitlab-migration` tag before releasing the freeze | SRE | 2 h |
| M9.6 | Board 7 operations verdict `O1`–`O8`; Board 4 security verdict | SRE + SEC | 3 h |

### Phase M10 — Post-cutover

| # | Task | Owner | AI |
|---|---|---|---|
| M10.1 | Scheduled drift-detection plan (daily or weekly per M1.9); report, do not auto-overwrite (spec §11.4) | SRE | 2 h |
| M10.2 | Runbooks: token rotation, state recovery, drift reconciliation, emergency manual change | SRE | 4 h |
| M10.3 | Re-evidence `S08-G1/G2/G5/G9` on GitLab per the M0.3 decision (**IMP-7**) | ENG + QA + SRE | 4 h |
| M10.4 | Prove the future-change test (spec §16.1): add `partner-portal` by config change → MR → plan → apply | SRE | 2 h |
| M10.5 | Handover: operating model, ownership, on-call path, contacts | SRE | 3 h |

---

## 4. Sequencing constraints — the ones that actually bind

```text
M0.3 (gate-evidence decision) ──► M3   nothing is built before the target of the evidence is known
M0.4 (governance home)        ──► M4.3 the project must exist before it can be created
M1.2 (GitLab edition)         ──► M3.4 provider capability decides what the modules can express
M1.6 (state standard)         ──► M3.3 backend.tf cannot be written on a guess
M2.1 (history clean)          ──► M5.2 HARD. Never push unscanned history into a bank estate
M4   (projects exist, empty)  ──► M5   never seed into a project that does not exist
M5   (main exists)            ──► M6.1 SPEC §7. Never protect a branch before it exists
M6.8 (job-token allowlist)    ──► M7.9 cross-project CI fails without it, and fails after cutover
M1.8 (AWS conventions)        ──► M8.1 the trust policy is written against real account IDs
M9.5 (rollback tested)        ──► M9.4 never declare authoritative before rollback is proven
```

Everything else can run in parallel. The three phases that admit the most parallelism are M3
(module development), M5.4–M5.8 (greenfield seeds) and M7 (component templates).

---

## 5. Effort — what the AI persona can and cannot complete

### 5.1 Focused agent-hours, by phase

| Phase | AI hours | What the agent cannot do |
|---|---:|---|
| M0 — Governance | 4 | The four decisions and the T4 signatures |
| M1 — Discovery | 3 | All eleven enterprise inputs |
| M2 — Hygiene | 15 | Credential rotation; the security and compliance verdicts |
| M3 — Bootstrap IaC | 31 | The human plan review |
| M4 — Namespace/projects | 4 | The protected `terraform apply` |
| M5 — Migration/seed | 21 | Declaring the freeze |
| M6 — Governance apply | 16 | The apply; identity-group assignment in the bank IdP |
| M7 — CI/CD | 44 | Runner provisioning; the security re-baseline verdict |
| M8 — AWS OIDC | 9 | IAM identity provider and role creation in bank AWS |
| M9 — Validation/cutover | 13 | The authoritative-source declaration; board verdicts |
| M10 — Post-cutover | 15 | Human handover and acceptance |
| **Total** | **≈175 h** | |

### 5.2 Reading that number honestly

**≈175 focused agent-hours ≈ 22 agent-days.** Under the one-work-item-in-flight rule
([BOOT.md §1](../../context/BOOT.md)), with review cycles and rework, that is **4–5 weeks of agent
lane time** — not 175 hours of wall clock.

**But agent hours are not the constraint.** The critical path is:

```text
M1 enterprise inputs (BANK)        5–15 business days   ← longest single dependency
M0 T4 sign-offs (4 humans)         3–10 business days   ← can overlap M1
M8.3 AWS IdP + role creation       5–10 business days   ← can overlap M3–M7
Protected applies (M4, M6)         1–3 days each        ← human approval latency
```

| Scenario | Calendar | Assumption |
|---|---|---|
| **Fast path** | **3–4 weeks** | All M1 inputs available in week 1; Ultimate licence; existing enterprise state backend and runners; no secret found in M2.1 |
| **Expected** | **6–9 weeks** | M1 inputs trickle over 2–3 weeks; edition confirmed mid-flight; one round of security rework |
| **Slow path** | **12+ weeks** | Premium not Ultimate (IMP-3 redesign); a real secret in history (rotation + scrub + re-scan); AWS role creation queued behind a bank change window |

The honest summary for planning: **an AI persona can produce essentially all of the Terraform, the CI
components, the migration scripts, the runbooks and the evidence pack in roughly four to five weeks of
lane time — and it will still be waiting on the bank.** The code is not the bottleneck. Enterprise
inputs, T4 signatures and protected applies are, and no amount of agent throughput compresses them.

### 5.3 What an agent must never do here, however capable

Per [BOOT.md §3](../../context/BOOT.md) and the persona authority matrix:

* Run `terraform apply` against the bank GitLab control plane on its own authority.
* Manufacture the T4 Architecture, Security or Risk & Compliance sign-off on `CR-014`.
* Declare GitLab authoritative, or archive the GitHub origin.
* Resolve **IMP-2** (the persistence-service boundary) — that is Mahesh and Aarti.
* Accept a security control downgrade if the licence turns out to be Premium (**IMP-3**) — that
  is Deepali's exception to grant.
* Declare `S08` criteria re-evidenced — that is Swapnali's verdict on the evidence, not the agent's.

---

## 6. What this plan does not cover

* Render → EKS runtime re-platform (**IMP-5**) — deliberately separated; S09 work with its own gate.
* The `bank-persistence-service` boundary question (**IMP-2**) — parallel CR, not migration scope.
* Contract content. `contracts` is seeded as a **skeleton**; no OpenAPI or AsyncAPI document exists in
  the repository today, and authoring them is Engineering and Architecture work, not migration work.
* GitOps content. `gitops` is seeded as a skeleton; environment desired state depends on EKS existing.
* Any change to service boundaries, topology or scope. A repository move that also moves a boundary is
  a migration nobody can audit.
