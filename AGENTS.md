# AGENTS.md

Guidance for cloud agents working in this repository.

## Governance — read before suggesting or implementing anything

This repository runs the **AIGEM** governance model
(**[docs/governance/](./docs/governance/README.md)**). Before acting on any requirement, bug,
suggestion, or finding:

1. Run the freshness check and act on its exit code
   (`java scripts/governance/FreshnessCheck.java` — exit `2` means do not admit new work).
   Runs on the documented JDK 21 + Git baseline; no build step, no dependencies.
   `./gradlew governanceFreshness` is equivalent.
2. Read **[docs/governance/state/CURRENT-STATE.yaml](./docs/governance/state/CURRENT-STATE.yaml)** —
   which workstream, which lifecycle stage, what is in and out of scope, which gate is open.
3. Read **[RUNBOOK.md §8](./docs/governance/RUNBOOK.md#8-what-the-ai-agent-must-know-about-this-project)** —
   the ten facts you must be able to state before acting, and **how your thinking must change at
   this stage of the project** (§8.3). This is the section that makes you useful here.
4. Follow **[09-AI_EXECUTION_RULES.md](./docs/governance/09-AI_EXECUTION_RULES.md)** — the binding
   agent contract.
5. Triage every input through the pipeline in
   [governance/README.md §5](./docs/governance/README.md) **before** writing code. Most inputs
   terminate in three steps with a one-screen record.

Humans: your role card is in **[RUNBOOK.md §6](./docs/governance/RUNBOOK.md#6-role-cards)** —
one screen, exact actions, exact cadence. That is all you need to read.

**Architecture reviews:** adopt **[Mahesh — Principal Insurance Platform Architect](./docs/context/roles/mahesh-solution-architect.md)** as the single Board 1 architecture persona. For deep authority, decision, HLD/LLD, evidence and exception behavior, load the modular **[Mahesh Principal Architect package](./docs/context/roles/mahesh-principal-insurance-platform-architect/README.md)**. These are the same persona, not two architect roles. An AI agent may draft/simulate Mahesh's reasoning where AIGEM permits but must never impersonate mandatory human Architecture approval; T4 Architecture sign-off remains human as required by [`11-REVIEW_GATES.md`](./docs/governance/11-REVIEW_GATES.md).

**Risk & Compliance reviews:** adopt **[Shailja S](./docs/context/roles/shailja-s-compliance-risk-head/README.md)** as the Board 6 persona and follow its loading order before producing the AIGEM Risk & Compliance verdict. Shailja is grounding context, not a substitute for authoritative regulation/policy or the binding rules in [`11-REVIEW_GATES.md`](./docs/governance/11-REVIEW_GATES.md); T4 still requires the mandatory human sign-off.

**Mahesh ↔ Shailja:** where a consequential architecture decision has compliance/risk impact, use the shared **[Architecture/Compliance Decision Protocol](./docs/context/roles/shared/architect-compliance-decision-protocol.md)**. Mahesh owns design/implementation, Shailja owns compliance permissibility/control outcomes, and unresolved material conflicts escalate to accountable humans. Neither persona may silently override the other's binding domain decision.

Core rules:

- **A suggestion is never implemented in the turn it is raised.** Triage it, record it, schedule
  it — then go back to the work item you were on.
- **Exactly one work item in flight.** Only the P1 override classes may interrupt it
  ([05 §3](./docs/governance/05-PRIORITY_MODEL.md#3-hard-p1-overrides): build failure, exploitable
  vulnerability, incorrect domain model, missing mandatory API, regulatory violation, data
  corruption, blocking dependency, AC failure).
- **Parked is not deleted.** Every deferral records a target stage and an unpark trigger in
  [registers/PARKED-BACKLOG.md](./docs/governance/registers/PARKED-BACKLOG.md).
- **Priority is stage-relative.** The same item is P4 during hardening and P1 at production
  readiness. Record both `priority_now` and `priority_at_target`.
- **Do not re-report known debt** — check
  [01 §6](./docs/governance/01-CURRENT_STATE.md#6-known-open-debt-affecting-triage) first.
- Every `TODO` carries a work item ID. Nothing is marked Done without evidence.
- Agents never edit stage state, never approve change requests, and never self-approve a board
  that requires a human.

Quick triage answer shape:

```text
SUG-00NN · "<the suggestion>"
Stage: <phase> — <fits / belongs to X>     Scope: <SC code>
Necessity: <MUST|SHOULD|COULD|NOT-NOW>     Verdict: <ADMIT|PARK|REJECT|ESCALATE>
Priority: P<n> now · P<n> at target        Recorded: <register file>
Continuing with <current work item>.
```

**Documentation map:** once governance tells you *whether* to act, `docs/README.md` tells you
*where things live* — it indexes every document and explains the folder split: `governance/`
(how work is admitted and gated), `context/` (background, non-binding), `platform/`
(cross-cutting specs), `au-bank-insurance-platform/` (business SSOT), and
`1sb-insurance-integration/` (module SSOT), plus which document wins on conflict. Read it
before assuming a doc is missing.

## Repository status

Multi-module Gradle (Kotlin DSL) monorepo for the **1SB insurance platform**:

- **Java 21** / **Spring Boot 3.3.4**
- Shared libs under `libs/`
- Services under `services/`:
  - `1sb-integration-service` (port **8080**) — bank-facing 1SB adapter; no local DB
  - `bank-persistence-service` (port **8081**) — **platform common** persistence (Flyway + JPA + `/internal/v1`); consumers include integration and future audit-consumer

## Cursor Cloud specific instructions

### Services

| Service | Required? | Notes |
|---------|-----------|-------|
| `1sb-integration-service` | Yes (Phase 1+) | Boot app; profiles `local` / `test` / `uat` / `prod`. No datasource — job store via HTTP to bank-persistence. |
| `bank-persistence-service` | Yes (for local job-store / audit HTTP) | Boot app on **8081**; owns DB for all consumers. Local/test use **H2** (`MODE=PostgreSQL`); uat/prod use PostgreSQL. |
| `audit-consumer-service` | Future | Doc stub only; will call `POST`/`GET` `/internal/v1/audit-events` on bank-persistence — no second audit DB. |
| PostgreSQL / H2 | Persistence service only | Consumers never embed a DB for these tables. |

### System tooling (VM)

- **JDK 21** (required for build)
- **Git** 2.43+
- Node.js / Python / Go may also be present but are not required for this Java platform

Docker is not required for unit tests.

### Lint / test / run

```bash
./gradlew test

# Coverage (QA-001 / R7) — reports + gates
./gradlew test jacocoTestReport jacocoTestCoverageVerification
# HTML/XML under <module>/build/reports/jacoco/test/ — see
# docs/1sb-insurance-integration/service-ssot/COVERAGE.md

# Local: start persistence first (8081), then integration (8080)
./gradlew :services:bank-persistence-service:bootRun --args='--spring.profiles.active=local'
./gradlew :services:1sb-integration-service:bootRun --args='--spring.profiles.active=local'
# Integration job-store calls need persistence on http://localhost:8081
# (override with BANK_PERSISTENCE_BASE_URL / bank.persistence.base-url)
```

**Testing (QA Lead):** follow `docs/1sb-insurance-integration/service-ssot/TESTING-RULES.md`  
Strategy & ownership: `docs/1sb-insurance-integration/service-ssot/QA-LEAD-TESTING-STRATEGY.md`  
**Roles & Definition of Done:** `docs/1sb-insurance-integration/service-ssot/ROLE-GUIDELINES-AND-DOD.md`  
Backlog: `docs/1sb-insurance-integration/service-ssot/TEST-BACKLOG.md`  
Coverage gates: `docs/1sb-insurance-integration/service-ssot/COVERAGE.md` (libs 80%/70%; services interim line floor)

Targeted shared-lib verification:

```bash
./gradlew :libs:bank-common-error:test :libs:bank-common-security:test \
  :libs:bank-common-audit:test :libs:bank-common-secrets:test
```

### Update script

After cloning, no separate install step beyond a Gradle build (wrapper downloads the toolchain/deps).
