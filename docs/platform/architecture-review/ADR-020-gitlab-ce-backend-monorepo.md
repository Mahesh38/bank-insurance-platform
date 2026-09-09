# ADR-020 — GitLab CE H0: team-split polyrepo, Gradle backend monorepo

**Status:** Proposed (`A3_JOINT_REVIEW`) — human T4 Architecture sign-off outstanding  
**Date:** 2026-09-09  
**Deciders (named, not signed):** Mahesh (Architecture) · consulted Amit (Engineering), Shivanshi (SRE / CI platform), Shailja (Compliance — no GitHub integration), Kalpana (Delivery)  
**Workstream:** WS-3 (cutover affects WS-1/WS-2 modules in the same Gradle tree)  
**Stage:** S08 — Engineering Foundation (S09 overlapped)  
**Origin:** `SUG-20260909-glc` · plan [`PLAN-005`](../../governance/plans/PLAN-005-gitlab-ce-backend-monorepo.md)

## Context

The bank GitLab is **Community Edition** at `gitlab-ce.au.bank.in`. Direct GitHub↔GitLab integration is a **compliance prohibition**. Frontend already lives in `Insurance/frontend/nip-app`. The current GitHub repository is a **Gradle monorepo** (6 libs + 20 services, most WS-3 modules still skeletons per [`backend-service-catalog.yaml`](../engineering/backend-service-catalog.yaml)).

[`GITLAB-REPO-STRUCTURE.md`](../engineering/GITLAB-REPO-STRUCTURE.md) previously recommended “monorepo today, one GitLab project per module at cutover”. [`ADR-019`](./ADR-019-microservice-skeleton-scaffold.md) created those modules **in this tree** and deferred the GitLab split.

GATE-S08-G1/G2/G9/G10 need one pipeline that builds every module and one clone for a new engineer. GitLab CE lacks merge-request dependencies, merge trains, and required CODEOWNERS / multiple approval rules (Premium). A 20-project split would force Maven publishing of `bank-common-*` during foundation.

Unknowns: exact CE version and whether Package Registry is enabled on the instance (not required at H0). DevOps owns `.gitlab-ci.yml`; this ADR does not invent the pipeline YAML.

## Decision

1. **H0 GitLab layout is the three projects already created** under group `Insurance`: `platform/nip-governance`, `frontend/nip-app`, `backend/nip-backend`.
2. **`nip-backend` receives this entire GitHub tree** (Gradle + `docs/` + governance scripts). It remains one Gradle monorepo.
3. **Do not create one GitLab project per microservice or shared library at H0.** Catalogue `gitlab_group` values are logical ownership (CODEOWNERS / future extract), not project-creation instructions.
4. **`nip-governance` is CI templates and group policy**, not a second AIGEM source of truth.
5. **Cutover is an air-gapped git bundle (or clean-room tree copy if Compliance forbids history), then `git push` to GitLab.** No importer, no mirroring, no GitHub OAuth, no GitHub Actions trigger.
6. Per-service extract is **parked** until S09 is green, libs are published to GitLab Maven Package Registry, and a service has an independent release cadence.

## Alternatives considered

| Option | Why not |
|--------|---------|
| One GitLab project per Gradle module (previous draft of `GITLAB-REPO-STRUCTURE.md`) | Fails X3/X5/X6 at S08; CE cannot cheaply coordinate 20 MRs; shared libs become a packaging programme; GATE-S08-G1 becomes 20 jobs or a fake monorepo CI |
| Single repo including Flutter | Frontend team and stack already separated; `ADR-015` |
| Split AIGEM into `nip-governance` and leave only Java in `nip-backend` | Breaks one-clone GATE-S08-G10 and the governance jobs that run beside `./gradlew test` |
| GitLab GitHub importer / pull mirror | Named compliance prohibition |
| Do nothing (stay on GitHub) | Bank CI/CD is GitLab (`SUG-20260825-arb` / `ADR-016`); cutover is required, topology still has to be chosen |

## Consequences

**Positive**

- Matches the GitLab CE group the owner already created.
- GATE-S08 CI shape is unchanged: one Gradle invocation.
- Frontend remains a clean team boundary.
- Later extract is possible; joining 20 repos is not required.

**Negative / accepted costs**

- A change in `bank-common-error` still rebuilds the tree (already true).
- CE will not enforce path CODEOWNERS; review is process, not product.
- `nip-backend` ACLs apply to all backend modules; finer GitLab project ACLs wait for an extract.

**Constrains future work**

- Creating `insurance/backend/<service>` projects before the unpark trigger is SF4 against this ADR unless a CR amends it.
- GitHub↔GitLab integration remains REJECT (`SC4` if mandated, else policy).

## Reversibility

| Question | Answer |
|----------|--------|
| Cost to reverse | medium for “we should have split service X” (filter-repo); high for “we split 20 projects and must reassemble” |
| What makes it expensive | published Maven coordinates, duplicated CI, divergent Gradle wrappers |
| Point of no return | first production deploy from a split project with a published lib version other teams consume |

## Revalidation triggers

- GitLab instance upgraded to Premium **and** a service has an independent release train.
- GATE-S08-G1 rewritten to a proven coordinator pipeline.
- Compliance requires physical separation of a module (then extract **that** module only).

## Compliance and security impact

- Regulatory: no live GitHub connection from GitLab CE (Shailja). Provenance via bundle transfer record.
- Security: secrets are **not** copied from GitHub; Deepali/DevOps recreate them. Attack surface is the GitLab project ACL, not a new runtime.
- Audit: prefer keeping git history unless Compliance orders a clean-room root commit.

## Authority

Agent draft for Board 1. **Does not satisfy human T4 Architecture sign-off.** Shivanshi owns CI platform mechanics; Amit owns application CI correctness; Shailja owns the no-integration control outcome.
