# CR-014 — Migrate the platform from personal GitHub to the company GitLab estate

**Date:** 2026-08-29
**Type:** SCOPE (with `STAGE`, `GOV` and `PLAN` consequences)
**Raised by:** `agent:claude` in the Shivanshi (SRE / `R10` / Board 7) persona, on stakeholder direction
**Workstream:** WS-3 — AU Bank Insurance Distribution Platform
**Stage:** S08 — Engineering Foundation, with S09 — Platform & Environment Foundation overlapped
**Origin:** [`SUG-20260829-glm`](../registers/SUGGESTION-REGISTER.md#sug-20260829-glm--github-to-gitlab-estate-migration) · [`SUG-20260829-imp`](../registers/SUGGESTION-REGISTER.md#sug-20260829-imp--acceptance-of-the-thirteen-glm-001-improvements)
**Plan:** [`GLM-001`](../../platform/gitlab-migration/GLM-001-migration-plan.md)
**Authority document:** *Bank Insurance Platform — GitLab Terraform Bootstrap Requirements* v1.0, 29 Aug 2026, tabled as an **Approved Baseline for SRE Implementation**

> ## Decision: `APPROVED_WITH_CONDITIONS` — 2026-08-29
> Approved by the boards with the **twenty-nine conditions** recorded in
> [`CR-014/verdicts/`](./CR-014/verdicts/README.md), plus the five approval conditions at §11.
>
> **Provenance.** The board outcome was **relayed by `human:Mahesh` (repository owner) on
> 2026-08-29**. This record captures the decision as relayed. It does not reproduce individual
> signature artefacts, because the agent recording it did not witness them — per
> [Rule CC-1](../14-CHANGE_CONTROL.md#3-procedure) an agent may record a decision the owner has
> given, and may never manufacture one. The seven files under `verdicts/` remain **AI-drafted board
> inputs** and are not signature artefacts; they are retained because the twenty-nine conditions
> approved with this CR are defined in them.
>
> **The approval does not lift the conditions.** Two remain hard blocks on the first push to the
> bank estate and neither has a workaround: `C-SEC-1` (clean blocking full-history secret scan) and
> `C-CMP-1` (data residency confirmed permissible). Approval authorises the work; it does not
> authorise starting it before its gates.

---

## 1. Why this needs a CR at all

The migration was triaged `SF1 / SC0` — on-stage and in scope, under `ADR-016` and
`SUG-20260825-arb`, which already ratified GitLab CI/CD and Terraform IaC as the enterprise
baseline. On-stage in-scope work does not normally need a change request. **This does, on three
independent grounds** from [14 §1](../14-CHANGE_CONTROL.md#1-what-needs-a-change-request):

| # | Ground | Trigger row | Approvers required |
|---|---|---|---|
| 1 | Four `GATE-S08` exit criteria change the platform they are evidenced on | *Change a stage's exit criteria* | Architect + PO + affected boards |
| 2 | `docs/governance/`, `scripts/governance/`, `scripts/context/`, `AGENTS.md` and `CLAUDE.md` move to a separate repository | *Change these governance files (`GOV`)* | Architect + PO |
| 3 | The approved delivery approach for CI, deployment and IaC is replaced with a different mechanism | *Approach replaced with a different mechanism* ([14 §4](../14-CHANGE_CONTROL.md#4-changing-an-approved-plan)) | Re-review by all boards that approved the original |

Ground 1 is the one that matters. It is set out in full in §5.

---

## 2. Current position

Quoted from the approved documents as they stand today:

| Document | What it says now |
|---|---|
| [`CURRENT-STATE.yaml`](../state/CURRENT-STATE.yaml) | WS-3 is at `S08 — Engineering Foundation`, `IN_PROGRESS`, phase *"Foundation Recovery Increment — S08 with S09 overlapped"*. `GATE-S08` is `OPEN` with **10 of 10 exit criteria open** |
| `.github/workflows/application-ci.yml` | *"BRANCH PROTECTION — required to close S08-G2, and NOT configurable from this file. A repository administrator must, on `main`… require these status checks to pass, exactly as named"* — four named GitHub checks |
| [`DECISION-REGISTER`](../registers/DECISION-REGISTER.md) `ADR-016` | GitLab CI/CD and Terraform IaC are the enterprise delivery baseline. Status `Proposed (A3_JOINT_REVIEW)` |
| [`DECISION-REGISTER`](../registers/DECISION-REGISTER.md) §1 | *"Persistence is platform-common (`bank-persistence-service`), reached over HTTP"* — status **Accepted** |
| `render.yaml` | Deployment is a Render.com blueprint: one container, two JVMs, one public port |
| Repository | One personal GitHub repository (login recorded only in the sealed bundle). 358 commits · 81 remote branches · **0 tags** · ~14.9 MiB pack |

Measured, not assumed: there is **no** Terraform, **no** GitOps manifest and **no** OpenAPI or
AsyncAPI document anywhere in the repository today.

---

## 3. Proposed change

Adopt the bank baseline and execute [`GLM-001`](../../platform/gitlab-migration/GLM-001-migration-plan.md),
phases M0 → M10, with the thirteen improvements at
[GLM-001 §2](../../platform/gitlab-migration/GLM-001-migration-plan.md#2-improvements-and-corrections--raise-before-executing)
accepted by the repository owner on 2026-08-29.

| # | Change |
|---|---|
| 1 | The GitLab estate is provisioned as Terraform/IaC under `insurance/bank-insurance`, with `product`, `delivery`, `engineering` and `governance` subgroups |
| 2 | The monorepo splits into `product/frontend` (Flutter) and `product/backend` (services + libs) as **orphan first commits** under a company git identity ([`CR-017`](./CR-017-orphan-import-and-file-workbench.md) / `AC-6`). Personal-forge history, authorship and dates are **not** imported. *Supersedes the 2026-08-29 wording that preserved history via `git filter-repo`.* |
| 3 | `contracts`, `infrastructure`, `gitops`, `ci-components`, `security-policies` and `gitlab-bootstrap` are seeded as governed skeletons |
| 4 | **A ninth project, `governance/platform-governance`**, receives `docs/`, `scripts/{governance,context,lifecycle}`, `AGENTS.md` and `CLAUDE.md`. This is an addition to the bank baseline and requires the Appendix C exception at §6 |
| 5 | The three GitHub Actions workflows are re-expressed as versioned components in `engineering/ci-components` and consumed by the application repositories |
| 6 | `S08-G2`'s gate mechanism is redesigned for GitLab semantics — one required pipeline, every gating job `allow_failure: false`, no path filter that can skip a gating job |
| 7 | CodeQL is **replaced**, not ported: GitLab SAST is a different engine and its findings are re-baselined |
| 8 | Full-history secret scanning becomes a **blocking pre-migration gate**, with credential rotation ordered before history scrubbing |
| 9 | GitLab OIDC and AWS STS temporary credentials replace static AWS keys as the deployment authentication mechanism |
| 10 | GitLab is declared the single source of truth at cutover; the GitHub origin is archived read-only at cutover +24 h |

### 3.1 The thirteen accepted improvements, and where each lands

| Improvement | Severity | Lands in | Needs a decision outside this CR |
|---|---|---|---|
| IMP-1 governance tree has no home | `O1` | §3 row 4 · §6 exception | **Yes** — bank Appendix C acceptance |
| IMP-2 shared persistence contradicts spec §3.3 | `O1` | **Not this CR** | **Yes** — [`CR-015`](./CR-015-shared-persistence-vs-bank-baseline.md) |
| IMP-3 CodeQL does not port | `O1` | §3 row 7 · §5 | **Yes** — GitLab edition (M1.2) |
| IMP-4 no named-required-check analogue | `O1` | §3 row 6 · §5 | No |
| IMP-5 do not conflate SCM move with Render → EKS | `O2` | §7 out of scope | Recorded in [`DEC-20260829-01`](../DEC-20260829-01-m0-migration-decisions.md) §4 |
| IMP-6 secret hygiene is a blocking pre-gate | `O0` | §3 row 8 | No — Security owns the verdict |
| IMP-7 gate-evidence continuity | `O1` | §5 | Recorded in [`DEC-20260829-01`](../DEC-20260829-01-m0-migration-decisions.md) §2 |
| IMP-8 81 branches, 0 tags | `O2` | GLM-001 M2.5, M2.6 | No |
| IMP-9 `CI_JOB_TOKEN` allowlisting | `O1` | GLM-001 M3.6, M6.8 | No |
| IMP-10 bootstrap state isolation | `O1` | GLM-001 M3.3 | No |
| IMP-11 Terraform ordering traps | `O2` | GLM-001 M4.1, M4.3, M4.4 | No |
| IMP-12 cheap wins during the split | `O3` | GLM-001 M5.3, M5.8, M2.4 | No |
| IMP-13 declare the freeze window | `O2` | GLM-001 M5.1 | No — Delivery owns the window |

---

## 4. Driver

**External dependency change.** The bank has tabled *GitLab Terraform Bootstrap Requirements* v1.0
as an Approved Baseline for SRE Implementation. The platform's source of truth, CI platform,
deployment authentication and repository topology are decided by that document, not by this
repository. `ADR-016` and `SUG-20260825-arb` already recorded the direction; this CR is the point
at which it becomes executable work with a gate consequence.

This is a valid driver under [14 §2](../14-CHANGE_CONTROL.md#2-change-request-format). It is not
"it would be better".

---

## 5. Impact — the part approvers must read

### 5.1 Stage impact: four exit criteria change their evidence basis

`GATE-S08` has ten exit criteria. Four are evidenced **by the CI platform itself**:

| Criterion | Owner | What evidences it today | After cutover |
|---|---|---|---|
| `S08-G1` CI builds and tests every module on every PR | Amit | GitHub Actions **run history** — evidence tier `E4` | **Re-opened.** Run history does not migrate |
| `S08-G2` Merge to main impossible without a green pipeline | Amit | Four *named* GitHub required status checks | **Re-opened**, and the mechanism must be redesigned (IMP-4) |
| `S08-G5` Secret, SAST, SCA and image scanning in the pipeline | Deepali | gitleaks + CodeQL + Trivy on GitHub | **Re-opened.** CodeQL has no GitLab equivalent (IMP-3) |
| `S08-G9` Pipeline feedback under 10 min p95; flake under 1% | Shivanshi | Measured on GitHub-hosted runners | **Re-opened.** Must be re-measured on bank runners |

`S08-G3`, `G4`, `G6`, `G7`, `G8` and `G10` are unaffected in substance, though `G8` and `G10`
depend on the governance tree remaining reachable — which is why §3 row 4 exists.

> **This CR does not waive, weaken or re-word any exit criterion.** It records that four of them
> will be evidenced on a different platform, and asks the boards to decide *when*. That decision is
> recorded separately in [`DEC-20260829-01 §2`](../DEC-20260829-01-m0-migration-decisions.md#2-m03--gate-evidence-strategy).

### 5.2 Scope

No product scope changes. No bounded context is added, removed, split or merged. No journey, rule,
LOB or acceptance criterion changes. The `out_of_scope` and `never` lists in `CURRENT-STATE.yaml`
are untouched.

### 5.3 Dependencies

| Becomes blocked | Becomes unblocked | Becomes invalid |
|---|---|---|
| All GLM-001 technical phases, on eleven bank enterprise inputs (M1) and on this CR | Nothing yet | `ASM-006` *"No AWS deployment target before Phase 6"* is under pressure — GLM-001 M8 designs AWS OIDC at S09 |

Eleven enterprise inputs are recorded as `ASM-012` … `ASM-022` with expiry 2026-09-19.

### 5.4 Effort

**XL.** ≈175 focused agent-hours across eleven phases; 6–9 weeks calendar expected, 3–4 weeks fast
path, 12+ weeks if the GitLab edition is Premium or a live credential is found in history. The
critical path is enterprise inputs, T4 signatures and protected applies — not engineering effort.

### 5.5 Risk if rejected

The platform stays on a single personal GitHub account with no organisational control, no
enterprise identity, no protected estate, and a deployment path the bank baseline does not permit.
`GATE-S08` would close on infrastructure the bank does not own and cannot audit — which is not a
closed gate, it is a deferred one.

### 5.6 New risks this CR creates

`RISK-016` (secret in migrated history) · `RISK-017` (GitLab edition insufficient for the S08-G5
mechanism) · `RISK-018` (gate evidence lost at cutover) · `RISK-019` (monorepo split breaks
cross-project CI) · `RISK-020` (dual writable sources outlive the freeze). All are recorded in the
[risk register](../registers/RISK-REGISTER.md) with owners and triggers.

---

## 6. The Appendix C exception this CR requests

Appendix C of the bank baseline permits the SRE team to adapt module boundaries and implementation,
*"provided such adaptation must not weaken the baseline architectural boundaries, least-privilege
intent, reproducibility, auditability or immutable artifact-promotion model."*

**Requested exception:** add a ninth project, `governance/platform-governance`, holding the AIGEM
governance model, the registers and the agent context tooling.

**Why it does not weaken anything:** it adds a boundary rather than removing one. The alternatives
are worse — placing 16 MB of governance in `backend` runs the Java build on every governance change
and makes the frontend team a guest in the backend repo to read its own rules; splitting it across
repositories breaks `DOC-MAP.yaml`, `context-load.py` and `FreshnessCheck`, which every agent
session depends on. `S08-G8` and `S08-G10` both fail if the operating model is unreachable.

> **This exception is not the repository owner's to grant alone.** The baseline is a bank document.
> The repository owner accepting IMP-1 admits it as *our* proposal; the bank's GitLab platform and
> architecture authority must accept the exception itself. Until then `governance/platform-governance`
> is a proposal, and GLM-001 M4.3 must not create it.

---

## 7. What this CR does **not** do

- It did **not** approve itself. The approval at the head of this document was taken by the boards and relayed on 2026-08-29; the agent recorded it and did not supply it.
- It does **not** edit `current_phase`, `stage_status` or any stage field in `CURRENT-STATE.yaml`.
- It does **not** waive, re-word or lower any `GATE-S08` exit criterion.
- It does **not** resolve the shared-persistence conflict (IMP-2). That is
  [`CR-015`](./CR-015-shared-persistence-vs-bank-baseline.md), joint Mahesh + Aarti jurisdiction, and it must
  not be resolved as a side effect of a repository move.
- It does **not** authorise the Render → EKS runtime re-platform (IMP-5). That is separate S09 work
  with its own gate.
- It does **not** authorise any `terraform apply` against the bank GitLab control plane.
- It does **not** declare GitLab authoritative or archive the GitHub origin.
- It does **not** grant the Appendix C exception at §6 — it requests it, and `AC-2` makes the bank's written acceptance a precondition of M4.3.

---

## 8. Alternatives considered

| Option | Consequence |
|---|---|
| **Do nothing** — stay on personal GitHub | Contradicts `ADR-016` and the bank baseline. `GATE-S08` closes on infrastructure the bank neither owns nor audits. Rejected. |
| **Mirror to GitLab, keep GitHub writable** | Explicitly forbidden by the baseline §7.2. Two sources of truth is the failure mode, not the migration. Rejected. |
| **Migrate as one repository, split later** | The split is cheapest while history is already being rewritten for the secret scan. Splitting after cutover means a second freeze and a second history rewrite in the bank estate. Rejected. |
| **Migrate and re-platform to EKS in one programme** | Puts the runtime change and the gate-evidence recovery on the same critical path. Deferred by IMP-5, recorded in `DEC-20260829-01 §4`. |
| **Close `GATE-S08` on GitHub first, then migrate** | A live option, not rejected. It is precisely the decision at `DEC-20260829-01 §2`, and it belongs to Delivery and Engineering, not to this CR. |

---

## 9. Board positions attached

Seven AI-drafted positions were attached as **inputs** to the boards. The boards have since approved (see the decision block above); the files below remain inputs, and the twenty-nine conditions approved with this CR are defined in them:

| Board / role | Persona | Drafted verdict | Signature |
|---|---|---|---|
| Board 1 — Architecture | Mahesh | `APPROVE-WITH-CONDITIONS` · `A2` | Approved 2026-08-29 |
| `R3` — Engineering | Amit | `APPROVE-WITH-CONDITIONS` | AI position |
| Board 4 — Security | Deepali | `APPROVE-WITH-CONDITIONS` · `S1` | Approved 2026-08-29 |
| Board 5 — QA | Swapnali | `APPROVE-WITH-CONDITIONS` · `Q1` hold on evidence | AI position |
| Board 6 — Risk & Compliance | Shailja | `APPROVE-WITH-CONDITIONS` · `R2` | Approved 2026-08-29 |
| Board 7 — Operations | Shivanshi | `APPROVE-WITH-CONDITIONS` · `O1` | AI position |
| `R12` — Delivery | Kalpana | `CANDIDATE` — decision windows set | Superseded by the board approval |

Aarti (Database) is **not** an approver on CR-014. Her jurisdiction is engaged by
[`CR-015`](./CR-015-shared-persistence-vs-bank-baseline.md) and cannot be satisfied here.

Index and reading order: [`CR-014/verdicts/README.md`](./CR-014/verdicts/README.md).

---

## 10. Change request record

```yaml
change_request:
  id: CR-014
  raised_by: "agent:claude"
  date: 2026-08-29
  type: SCOPE                # with STAGE, GOV and PLAN consequences
  driver: "external dependency change — bank GitLab Terraform Bootstrap Requirements v1.0"
  evidence:
    - "GitLab Terraform Bootstrap Requirements v1.0 — Approved Baseline for SRE Implementation"
    - "ADR-016 — GitLab CI/CD and Terraform IaC as the enterprise delivery baseline"
    - "GATE-S08 open, 10 of 10 criteria open; S08-G1/G2/G5/G9 evidenced by the CI platform"
    - "application-ci.yml — the S08-G2 mechanism is four named GitHub required status checks"
    - "Measured origin: 358 commits, 81 branches, 0 tags, no Terraform, no GitOps, no contracts"
  impact:
    scope: "no product scope change; no bounded context added, removed, split or merged"
    stage: "S08-G1, G2, G5 and G9 change evidence platform; no criterion is waived or re-worded"
    dependencies: "all GLM-001 technical phases blocked on eleven bank inputs (ASM-012..ASM-022) and on this CR"
    parked_items: "none made eligible"
    effort: "XL"
    risk_if_rejected: >
      The platform remains on a personal GitHub account with no organisational control and a
      deployment path the bank baseline does not permit; GATE-S08 would close on infrastructure
      the bank neither owns nor audits.
  decision: APPROVED_WITH_CONDITIONS
  approvers: ["Board 1 Architecture", "Board 4 Security", "Board 5 QA", "Board 6 Risk & Compliance", "Board 7 Operations", "R3 Engineering", "R12 Delivery"]
  decided_on: "2026-08-29"
  recorded_by: "agent:claude, from a board outcome relayed by human:Mahesh on 2026-08-29"
  conditions:
    - "The 29 board conditions in CR-014/verdicts/ — C-SEC-1..8, C-CMP-1..5, C-ARC-1..5, C-OPS-1..6, C-ENG-1..5, C-QA-1..5"
    - "AC-1 M0.3 Option B: GitHub Actions is kept green for rollback continuity only; S08-G1/G2/G5/G9 are re-evidenced on GitLab; GATE-S08 remains OPEN throughout the migration"
    - "AC-2 M0.4: governance/platform-governance approved as the ninth project, subject to written acceptance of the Appendix C exception by the bank GitLab/architecture authority BEFORE M4.3; on rejection, the documented product/backend/governance/ fallback applies"
    - "AC-3 M0.6: Render is retained as a dev-preview target only — no PII, no real premium or quote values, no production-like data — and is retired only after EKS demonstrates equivalent deployment capability"
    - "AC-4 M9.4: GitHub becomes read-only at cutover, remains restorable for 14 days, and is archived only after the custody and retention disposition is approved"
    - "AC-5 bank-persistence-service migrates UNCHANGED under CR-014; repository migration is not combined with persistence restructuring (C-ARC-3, ADR-019)"
    - "AC-6 CR-017: GitLab receives orphan first commits only; identity-guard must pass (supersedes constraint 2 history preservation)"
    - "AC-7 CR-017: personal GitHub / Cursor workbench is file-level one-way import only; git-object sync forbidden"
    - "AC-8 CR-017: original history is a sealed offline bundle; disposition after Finding B + C-CMP-4"
  signature_status: >
    Board approval relayed by human:Mahesh 2026-08-29 and recorded. The verdicts/ files remain
    AI-drafted inputs, not signature artefacts. Constraint 2 was superseded 2026-08-31 by CR-017
    (orphan import + file-level workbench), also relayed by human:Mahesh.
```

---

## 11. Approval conditions (`AC-1` … `AC-8`)

Attached by the boards at approval, in addition to the twenty-nine conditions in `verdicts/`.
`AC-6`…`AC-8` were added on 2026-08-31 by [`CR-017`](./CR-017-orphan-import-and-file-workbench.md).

| ID | Condition | Gates |
|---|---|---|
| `AC-1` | **M0.3 Option B.** GitHub Actions is kept green **for rollback continuity only**. `S08-G1`, `G2`, `G5` and `G9` are re-evidenced on GitLab. `GATE-S08` remains `OPEN` throughout the migration and is reported open | Continuous · M10.3 |
| `AC-2` | **M0.4 approved, conditionally.** `governance/platform-governance` is the ninth project, **subject to written acceptance of the Appendix C exception by the bank GitLab/architecture authority before M4.3**. On rejection, the documented `product/backend/governance/` fallback applies | **M4.3** |
| `AC-3` | **M0.6.** Render is a dev-preview target only — no PII, no real premium or quote values, no production-like data. Retired only after EKS demonstrates equivalent deployment capability — on capability, not on a date | Continuous |
| `AC-4` | **M9.4.** GitHub becomes read-only at cutover, remains restorable for **14 days**, and is archived only after the custody and retention disposition is approved | **M9.4** |
| `AC-5` | **`bank-persistence-service` migrates unchanged.** Repository migration is never combined with persistence restructuring. `CR-015` remains parallel and non-blocking | **M5.2** |
| `AC-6` | **Orphan first commits only** (`CR-017`). No personal-forge history, authors, committers, trailers or GitHub merge subjects. `identity-guard.py` must exit 0 before push | **M5.2** |
| `AC-7` | **File-level workbench only.** Personal GitHub / Cursor may contribute trees; git-object sync (`fetch` / pull-mirror / `push --all`) is forbidden | Continuous |
| `AC-8` | **Sealed offline bundle** of the original history. Destroy or legal-hold only after Finding B is closed and Board 6 names the disposition (`C-CMP-4`) | **M5.2** create · **M9.4** dispose |

`AC-2`, `AC-5` and `AC-6` are the three that change what the migration may do. `AC-2` means M4.3
creates **eight** projects unless the bank exception has landed; `AC-5` restates `C-ARC-3`; `AC-6`
supersedes constraint 2's history-preserving `filter-repo` push.
