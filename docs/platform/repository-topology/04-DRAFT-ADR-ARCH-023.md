# 04 — Draft ADR `ARCH-023`

**This is a draft.** It enters
[08-architecture-decision-log.md](../architecture-review/08-architecture-decision-log.md) as
`ARCH-023` **only if `CR-002` is approved**. An agent may draft an ADR; it may not accept one
([Rule CC-1](../../governance/14-CHANGE_CONTROL.md)). Format:
[templates/ADR.md](../../governance/templates/ADR.md).

---

# ARCH-023 — Federated multi-repository topology with a single governance parent

**Status:** Proposed
**Date:** 2026-08-11
**Deciders:** Architect (Mahesh), PO — *pending*
**Workstream:** platform
**Stage:** WS-1 at L7 Hardening · WS-2 at L4/L6 Foundation
**Origin:** `SUG-20260811-r7k` → `CR-002`

## Context

The platform is one Gradle monorepo: 5 services, 5 shared libraries, one root `build.gradle.kts`
carrying the entire build convention, and one `docs/` tree carrying governance, business SSOT,
platform specs and module SSOT. [ARCH-003](../architecture-review/08-architecture-decision-log.md)
commits the target platform to ~16 domain services plus 2 BFFs and a routing layer, sequenced
across four delivery phases.

**Real constraints.** `docs/context/business-problem-statement.md` records the monorepo as one of
the technical architecture decisions to uphold — though `docs/README.md` classes `context/` as
**non-binding**, and no `ARCH-xxx` decision has ever ratified the repository topology. That gap is
what makes this decision necessary rather than merely a change: there is nothing to supersede.
WS-1 is mid-hardening with `GATE-P4` open, and criteria 4.1, 4.2 and 4.7 depend on the build a
split would rewrite.

**Merely current.** One delivery team; five services; no independent release cadence has been
requested; no measured pain from the monorepo has been reported. All four of those change as the
service count grows — which is why this is a decision about *sequencing*, not about *whether*.

**Not known.** When the second delivery team arrives; whether the ~16-service target survives
sponsor review; whether GitHub Packages or AWS CodeArtifact is the artifact host (the platform is
AWS/EKS per [ARCH-001/002](../architecture-review/08-architecture-decision-log.md), which argues
for CodeArtifact, but CI lives on GitHub Actions).

## Decision

We will move to **one repository per service**, with **one parent repository
(`bank-insurance-governance`) holding all cross-cutting documentation and the whole of the AIGEM
governance model**, consumed by every other repository as a **git submodule pinned to a release
tag** at `.governance/`.

Specifically:

- The parent owns `docs/**` (governance, platform, business SSOT, context), the governance
  tooling, the reusable CI workflow, the `aigem-triage` skill, and the `AGENTS.md` stanza
  template. **`CURRENT-STATE.yaml` and all six registers stay central**, not per repo.
- Each service repository owns its own source and its **service-scoped** SSOT only —
  PRODUCT-BACKLOG, TECH-DEBT, TEST-BACKLOG, COVERAGE, service architecture, API contracts.
- Shared libraries ship as **one repository publishing five artifacts and a BOM**, not five
  repositories.
- Build conventions ship as a **published Gradle convention plugin**, carrying the existing
  coverage floors (libs 80/70; `1sb-integration-service` 90/70; other services 50 line) unchanged.
- Migration is **waved**, and no WS-1 service moves before `GATE-P4` has passed.
- The ~14 target services that do not yet exist are **created in their own repositories from the
  outset** and are never migrated.

**Explicitly not in scope of this decision:** the choice of artifact host; the deployment topology
(EKS per ARCH-002 is unaffected); any change to service boundaries, contracts, or the standing
constraints. This decision moves code between repositories; it moves no responsibility between
services.

## Alternatives considered

| Option | Why not |
|--------|---------|
| **Do nothing — stay a monorepo** | Genuinely viable today, and the reason Waves 2–3 are gated rather than scheduled. It stops being viable when independent release cadence or multi-team ownership is required — at which point the migration is strictly larger. Kept alive as the fallback if Wave 1 goes badly |
| **Split now, all at once** | Rewrites the build during hardening, against three open gate criteria that run through it, with no contract test proving the integration ↔ persistence seam afterwards |
| **Copy governance into each repo** ([19 §2](../../governance/19-PORTING_GUIDE.md)) | Nine forks of the pipeline and nine state files. This is precisely the drift AIGEM exists to prevent; [19 §3](../../governance/19-PORTING_GUIDE.md) already prescribes one shared registers folder for multi-team platforms |
| **Track the parent's `main` instead of a tag** | A governance edit would change the rules of in-flight work in nine repositories with no PR in any of them — the opposite of change control |
| **One repo per shared library** | Fails X1/X6/X8: no independent release driver, five sets of CI, and a five-PR chain for one cross-cutting change |
| **Fetch governance over HTTP at agent runtime** | No pinning, no offline, no audit trail; fails in restricted CI and sandboxes |

## Consequences

**Positive**

- Independent release cadence, CODEOWNERS, and CI blast radius per service.
- One governance state for the whole platform, mechanically enforced rather than by convention.
- New services cost nothing to onboard — they are born split.
- Agent behaviour becomes uniform across repositories, because it is the same files.

**Negative / accepted costs**

- Atomic cross-service change is gone; contract versioning and an expand/contract protocol become
  mandatory.
- A binary artifact registry becomes a hard runtime dependency of the build.
- A triage in a service repo needs a register PR in the parent (automated, but real).
- Onboarding grows from `git clone` to clone + submodule + registry credentials + devstack.
- The single-command local stack must be rebuilt as `bank-insurance-devstack`.

**Constrains future work**

- Makes SF4/REJECT: adding a new service *inside* another service's repository; forking any L1/L2
  governance document into a service repo; a second `CURRENT-STATE.yaml`.
- Unblocks nothing currently parked. **Escalates TD-014** (E2E integration ↔ persistence, whose
  unpark trigger has already fired) from parked debt to a Wave 2 entry condition.

## Reversibility

| Question | Answer |
|----------|--------|
| Cost to reverse | **Low** through Wave 1; **medium** after Wave 2; **high** after Wave 3 |
| What makes it expensive | Published library versions consumers already depend on; per-repo history that a re-merge would flatten; archived monorepo |
| Point of no return | Wave 3 (archiving the monorepo). Waves 0–2 keep the monorepo live and frozen-but-recoverable |

## Revalidation triggers

- `GATE-P4` outcome — a failed gate defers Wave 2 automatically.
- The ~16-service target changing materially at sponsor review.
- A second delivery team arriving earlier than assumed (accelerates).
- Wave 1 exit evidence failing — reverts to "do nothing" with the parent retained.
- The artifact host decision landing on CodeArtifact vs GitHub Packages (affects Wave 0 only).

## Compliance and security impact

- **Regulatory obligations touched:** none directly. Audit-event and retention behaviour is
  unchanged; the standing constraint *"no second audit database"* is unaffected and remains
  enforceable because the constraints stay central.
- **Security posture change:** the artifact registry becomes part of the supply chain — it needs
  credential management, provenance, and dependency scanning in every repo. Nine repositories also
  mean nine sets of branch protections and secrets to configure correctly; misconfiguring one is a
  new and real failure mode. **The Security board must review before Wave 0.**
- **Audit or attribution implications:** improved — per-repo history and CODEOWNERS make
  attribution sharper, and the central registers keep the decision trail in one place.
