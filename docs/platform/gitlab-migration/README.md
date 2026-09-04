# GitLab Estate Migration — SRE pack

**Workstream:** WS-3 — AU Bank Insurance Distribution Platform
**Origin:** [`SUG-20260829-glm`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20260829-glm--github-to-gitlab-estate-migration)
**Owner:** Shivanshi — Principal SRE / Reliability Engineering Head (Board 7 · `R10`)
**Authority source:** *Bank Insurance Platform — GitLab Terraform Bootstrap Requirements* v1.0
(29 Aug 2026, "Approved Baseline for SRE Implementation") · [`ADR-016`](../../governance/registers/DECISION-REGISTER.md)
· [`SUG-20260825-arb`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20260825-arb--internal-architect-review-alignment-cloudflare-f5-external-alb-gitlab-ebscbs-terraform-cloudtrailcloudwatch)
**Status:** `AI-DRAFTED` — plan only. No Terraform, no CI YAML and no repository split has been
executed. Human T4 sign-off and the Phase M1 enterprise inputs are outstanding.

| Document | What it settles |
|---|---|
| [`GLM-001-migration-plan.md`](./GLM-001-migration-plan.md) | The task list, the sequence, the improvements and the AI-persona effort estimate |
| [`IDENTITY-SANITIZATION.md`](./IDENTITY-SANITIZATION.md) | Denylist for personal-forge / AI-vendor identity (`CR-017` / `AC-6`) |
| [`WORKBENCH.md`](./WORKBENCH.md) | File-level one-way import from the personal GitHub sandbox (`AC-7`) |
| [`C-CMP-1-RESIDENCY-QUESTIONNAIRE.md`](./C-CMP-1-RESIDENCY-QUESTIONNAIRE.md) | Questions bank infra must answer before Board 6 can rule |
| [`FINDING-B-DISPOSITION.md`](./FINDING-B-DISPOSITION.md) | Retirement/rotation evidence pack — unsigned |
| [`M5.2-OPERATOR.md`](./M5.2-OPERATOR.md) | Bank-machine sequence for the orphan import. Preflight cannot push |

## Read this first

The requirements document describes a **clean bank-side provisioning exercise**. This repository is
not a clean origin: it is one personal-GitHub monorepo carrying the frontend, the backend, the
governance operating model and the CI that currently evidences an **open** stage gate. Roughly a
third of the real work — history hygiene, the repository split, gate-evidence continuity and the
homeless `docs/` tree — is origin-specific and appears nowhere in the requirements.

`GLM-001` is that delta plus the specified work, in executable order.
