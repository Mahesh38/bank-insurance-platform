# Board 4 — Security · Position on CR-014

**Change request:** [CR-014](../../CR-014-gitlab-estate-migration.md) · **Plan:** [`GLM-001`](../../../../platform/gitlab-migration/GLM-001-migration-plan.md)
**Board:** 4 — Security (**veto authority** `B`) · **Role:** `R8`
**Persona:** Deepali — Principal Security Architect / Security Head
**Reviewer type:** `AGENT` · **Self-review:** false · **Change tier:** `T4` · **Date:** 2026-08-29

> ### ⚠ AI-drafted — the mandatory human Security signature is NOT satisfied
> [`11-REVIEW_GATES.md §2`](../../../11-REVIEW_GATES.md): Security verdicts at `T4` require
> `reviewer_type: HUMAN`. No exceptions, no aggregate override. Nothing below has been softened to
> make a human decision look clean.
>
> **`signature_status: AI-DRAFTED — mandatory human Security signature outstanding`**

> ## Drafted verdict: `APPROVE-WITH-CONDITIONS`
> **Security severity: `S1` — high.** Three `S1`, three `S2`. **No `S0` on the change surface as
> specified** — but see `SEC-F01`: the same migration executed *without* its pre-gate is an `S0`,
> and the distance between the two is one skipped step.

---

## 1. What I reviewed

`.gitleaks.toml` (69 lines, including its allowlists) · `.github/workflows/security-scanning.yml`
in full — the `secret-scan`, `secret-scan-history`, `sast`, `sca` and `sbom` jobs, their triggers,
their blocking logic and their severity thresholds · `.trivyignore` · `render.yaml` and its
`sync: false` variables · GLM-001 phases M2, M3, M6, M8 · the bank baseline §4.5, §5.1, §9.2, §9.5.

---

## 2. Findings

### `SEC-F01` · `S1` — history migration is `S0` if the pre-gate is skipped

`security-scanning.yml` runs full-history gitleaks in `secret-scan-history`, which is **scheduled
and non-blocking**. The working-tree scan blocks; history does not. That is a defensible posture for
a personal repository and an indefensible one at the moment 273 commits across 81 branches are
pushed into a bank estate.

The asymmetry is the whole finding: a credential in history is currently one person's problem, and
becomes a bank disclosure obligation the instant it lands on bank infrastructure. **I rate the
change as specified `S1` because IMP-6 makes the scan blocking. I would rate the same migration
executed without that gate `S0`, non-bypassable.**

The ordering in IMP-6 is correct and I am restating it as a condition because it is the part people
get backwards under time pressure: **rotate, then scrub, then migrate.** Scrubbing first leaves the
live credential live and merely harder to find, and it destroys the evidence of what to rotate.

### `SEC-F02` · `S1` — inherited allowlists are inherited control decisions

`.gitleaks.toml` carries allowlists. An allowlist is not configuration; it is a recorded decision
that a specific match is acceptable. Those decisions were made against a personal repository's risk
appetite, by whoever was writing the file at the time, and no bank control framework saw them.

Migrating the file migrates the decisions silently. Every allowlist entry must be re-justified
against bank rules before `security-policies` inherits it, and entries that cannot be justified are
removed — which may itself surface findings the allowlist was hiding.

### `SEC-F03` · `S1` — "GitLab SAST is green" is not "CodeQL was green"

IMP-3 is correct that CodeQL does not port. I am extending it. My concern is not that the engines
differ — it is that the difference will be **discovered as an absence**. GitLab SAST returning zero
findings on a codebase CodeQL had findings on reads identically to a clean codebase, and nothing in
the pipeline distinguishes the two.

Condition `C-SEC-3` below requires a **differential run**: both engines on the same commit, before
cutover, with the delta recorded. What CodeQL finds that GitLab SAST does not is a control gap that
must be named and owned, not absorbed.

### `SEC-F04` · `S2` — the automation identity

Baseline §5.1 already forbids an individual employee personal access token as the permanent
automation identity. I am making it non-waivable within my jurisdiction: the GitLab automation
identity is a dedicated service account, least-privileged, rotatable, auditable, and it is **not**
the same identity used for application deployment. Two identities, two blast radii.

### `SEC-F05` · `S2` — bootstrap state is the crown jewel

IMP-10 is right and understates it. The bootstrap state can delete every repository in the estate,
including the one holding the governance model that would tell you what was lost. Separate backend,
separate KMS key, separate identity, `prevent_destroy`, and a **tested** restore. An untested
restore is a belief.

### `SEC-F06` · `S2` — Render survives the migration

My standing constraint in this repository: **Render.com is dev-preview only and never a data path
for PII or production-like data.** IMP-5 keeps Render as a demo target reachable from GitLab CI.
I have no objection to that, and I am attaching the constraint to it explicitly so it does not get
re-litigated as "it's already deployed there": reachable from a pipeline is not a licence to send it
anything real.

---

## 3. Conditions

| ID | Condition | Must be true before |
|---|---|---|
| `C-SEC-1` | Blocking full-history secret scan over `main` and every branch on the migration allowlist, clean, with the report retained as evidence | GLM-001 M5.2 — the first push to GitLab |
| `C-SEC-2` | On any finding: **rotate the credential first**, then scrub with `filter-repo`, then re-scan clean. Never scrub before rotating | Any history rewrite |
| `C-SEC-3` | Differential SAST run — CodeQL and GitLab SAST on the same commit, delta recorded, every CodeQL-only finding named and owned | GLM-001 M9.4 — cutover |
| `C-SEC-4` | Every `.gitleaks.toml` allowlist entry re-justified against bank rules; unjustifiable entries removed | GLM-001 M5.7 — seeding `security-policies` |
| `C-SEC-5` | GitLab automation identity is a dedicated least-privileged service account, distinct from the application deployment identity | GLM-001 M3.11 — the first plan |
| `C-SEC-6` | Bootstrap state on a separate backend and KMS key from application state, with a **tested** restore | GLM-001 M4.2 — the first apply |
| `C-SEC-7` | Zero static AWS access keys in GitLab CI/CD variables; OIDC + STS proven from a pipeline | GLM-001 M9.1 — acceptance |
| `C-SEC-8` | Render remains dev-preview only — no PII, no production-like data, whatever reaches it from the new pipelines | Continuous |

---

## 4. What I am not deciding

- **I am not pre-approving a control downgrade.** If M1.2 returns Premium rather than Ultimate and
  MR-blocking security policies are unavailable, that is a security exception in my jurisdiction and
  I will not grant it in advance of knowing what is actually unavailable. Do not read this
  `APPROVE-WITH-CONDITIONS` as covering that case; it does not.
- **I am not ruling on data residency.** Shailja raised `CMP-F01` and it is hers. My controls assume
  the estate is somewhere lawful; whether it is, is a Board 6 conclusion.
- **I am not declaring `S08-G5` satisfiable.** That depends on the edition and on `C-SEC-3`.
  Swapnali owns whether the eventual evidence is sufficient.
- **I cannot satisfy my own signature.** This document is the security assessment. The human Board 4
  signature on `CR-014` remains mandatory and outstanding.
