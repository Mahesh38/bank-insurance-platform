# 01 — Target Repository Topology

**Status:** Proposal under `CR-002`. Not approved.
**Parent:** [`README.md`](./README.md)

---

## 1. Today

One repository, `mahesh38/bank-insurance-platform`, containing:

```text
build.gradle.kts          one convention block for every module — Java 21, Spring BOM 3.3.4,
settings.gradle.kts       Lombok, JaCoCo, and the per-module coverage floors
libs/                     5 shared libraries, consumed as `include(...)` source dependencies
services/                 5 Spring Boot services
docs/                     governance + business SSOT + platform specs + module SSOT
scripts/governance/       FreshnessCheck.java, ci-checks.py, fixture tests
.github/workflows/        governance.yml
Dockerfile, render.yaml   combined 2-service image for cloud validation
docker-compose*.yml       local stacks (integration + persistence; identity + Keycloak)
```

Everything above is one atomic unit: one commit can change a shared library and every consumer of
it, and one CI run proves the whole thing. **That property is what the split trades away**, and
every cost in [03-MIGRATION-PLAN.md](./03-MIGRATION-PLAN.md) descends from it.

---

## 2. Target repository set

Nine repositories at the end of the migration, in three tiers.

### Tier 0 — the parent

| Repository | Owns | Consumed by |
|------------|------|-------------|
| `bank-insurance-governance` | `docs/` (governance, platform, business SSOT, context), `scripts/governance/`, the reusable CI workflow, the `aigem-triage` agent skill, the `AGENTS.md` stanza template | **Every** other repo, as a pinned submodule at `.governance/` |

This is the repository the request calls "the parent". Its design is
[02-GOVERNANCE-FEDERATION.md](./02-GOVERNANCE-FEDERATION.md).

### Tier 1 — build substrate

| Repository | Owns | Why separate |
|------------|------|--------------|
| `bank-insurance-build-conventions` | Gradle convention plugins (`bank.java-conventions`, `bank.spring-service`, `bank.coverage`) + the platform BOM pinning Spring Boot 3.3.4 and Lombok | Today this logic is the 130-line `subprojects { }` block in the root `build.gradle.kts`. Once repos are separate it must be **published**, or nine repos each fork the toolchain, the BOM, and the coverage floors |
| `bank-insurance-common-libs` | `bank-common-error`, `-security`, `-audit`, `-observability`, `-secrets` — published as five artifacts plus a BOM | See §3 |

### Tier 2 — services

| Repository | From | Workstream | Port |
|------------|------|------------|------|
| `bank-insurance-1sb-integration-service` | `services/1sb-integration-service` | WS-1 | 8080 |
| `bank-insurance-persistence-service` | `services/bank-persistence-service` | WS-1 | 8081 |
| `bank-insurance-workforce-access-bff` | `services/workforce-access-bff` | WS-2 | — |
| `bank-insurance-identity-provider-adapter-service` | `services/identity-provider-adapter-service` | WS-2 | — |
| `bank-insurance-identity-authorization-service` | `services/identity-authorization-service` | WS-2 | — |

### Tier 3 — the seam nobody remembers

| Repository | Owns | Why it must exist |
|------------|------|-------------------|
| `bank-insurance-devstack` | `docker-compose.yml`, `docker-compose.identity.yml`, `config/keycloak/`, `config/onesb/`, `config/catalog/`, `.env*.example`, the combined `Dockerfile` + `render.yaml` | The moment services live in five repos, "start persistence on 8081, then integration on 8080" (`AGENTS.md`) has no home. Without this repo the local and cloud-validation story silently dies at Wave 1 |

**Naming.** One prefix, `bank-insurance-`, so the repositories sort together in the org listing
and a wildcard (`bank-insurance-*`) can drive org-level branch protection, CODEOWNERS, and the
governance-pin audit in [02 §6](./02-GOVERNANCE-FEDERATION.md#6-keeping-the-pins-honest).

---

## 3. Why the libraries are one repository, not five

The obvious symmetry — one repo per publishable unit — fails the anti-over-engineering tests in
[16 §6](../../governance/16-DECISION_MODEL.md#6-anti-over-engineering-tests):

| Test | Five lib repos | One lib repo |
|------|----------------|--------------|
| **X1 named consumer** | No consumer needs `bank-common-audit` to release independently of `bank-common-error` | — |
| **X6 simplest sufficient** | Five sets of CI, CODEOWNERS, release tooling, and governance pins | One |
| **X8 cognitive cost** | A cross-cutting change to the error model becomes a five-PR chain | One PR, one version |

The five libraries share the Spring BOM, change together (they are the cross-cutting concerns of
[ARCH-008](../architecture-review/08-architecture-decision-log.md)), and have no independent
release driver. They ship as **one repository publishing five artifacts and a BOM**, versioned
together. Split them later if — and only if — one of them acquires its own release cadence.

The same reasoning does *not* apply to the services: those genuinely differ in release cadence,
runtime, and owning workstream, which is the entire premise of the split.

---

## 4. What each service repository contains after the move

```text
.governance/                      submodule → bank-insurance-governance @ vX.Y.Z   (read-only)
AGENTS.md                         generated pointer stanza — see 02 §4
CLAUDE.md                         → AGENTS.md
.claude/skills/aigem-triage/      installed from .governance/skills/
.github/workflows/ci.yml          build + test + coverage
.github/workflows/governance.yml  3 lines: `uses:` the parent's reusable workflow
scripts/governance-bootstrap.sh   submodule-free fallback fetch — see 02 §5
build.gradle.kts                  ~20 lines: apply bank.spring-service, declare dependencies
settings.gradle.kts               single project
src/, Dockerfile, README.md
docs/                             SERVICE-scoped SSOT only:
                                    PRODUCT-BACKLOG.md · TECH-DEBT.md · TEST-BACKLOG.md
                                    COVERAGE.md · architecture/ · api contracts
```

The split of `docs/` is the load-bearing part: **service-scoped backlogs move with the service;
everything cross-cutting stays in the parent.** [02 §2](./02-GOVERNANCE-FEDERATION.md#2-what-is-common-and-what-is-not)
draws that line precisely.

---

## 5. Where the ~16 target services fit

[ARCH-003](../architecture-review/08-architecture-decision-log.md) commits the target platform to
roughly 16 domain services, two edge BFFs and a routing layer, **sequenced across four delivery
phases, not built simultaneously**. That matters here in two ways:

1. It is the strongest argument *for* eventually splitting — a 19-module monorepo owned by
   several teams is a different proposition from today's 10 modules owned by one.
2. It is also the argument for **not splitting the world now**: fourteen of those services do not
   exist. Under the target topology they are simply *born* in their own repository at their phase
   (Wave 4 in [03 §3](./03-MIGRATION-PLAN.md#3-the-waves)) — no migration required, ever.

The migration is therefore bounded to the five services that exist today. Everything after that
is greenfield.

---

## 6. Ownership

| Repository | CODEOWNERS | Governance pin bumped by |
|------------|------------|--------------------------|
| `bank-insurance-governance` | Architect + PO (both required) | n/a — it is the source |
| `bank-insurance-build-conventions` | Tech Lead | Tech Lead |
| `bank-insurance-common-libs` | Tech Lead | Tech Lead |
| WS-1 services | Tech Lead + QA Lead | Service owner, weekly audit ([02 §6](./02-GOVERNANCE-FEDERATION.md#6-keeping-the-pins-honest)) |
| WS-2 services | Tech Lead + Security Architect | Service owner |
| `bank-insurance-devstack` | Tech Lead + Ops | Tech Lead |

The parent repository requiring **two** owners is deliberate: it holds the standing constraints
and the stage gates, and [04-STAGE_GATES §5](../../governance/04-STAGE_GATES.md) already forbids
an agent — and a single human — from moving stage state alone.
