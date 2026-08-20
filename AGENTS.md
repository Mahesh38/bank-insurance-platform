# AGENTS.md

Entry point for any AI agent working in this repository.

> **Read [`docs/context/BOOT.md`](./docs/context/BOOT.md) first — then read only what your capsule
> names.** This file routes; it does not brief you. `docs/governance/` is binding and wins on any
> conflict with this file, with a persona card or with the context index.

## 1. Session start — four commands

```bash
java scripts/governance/FreshnessCheck.java          # 0 fresh · 1 warn, disclose · 2 do NOT admit new work
cat docs/context/BOOT.md                             # ten facts, stage posture, open gates, standing constraints, known debt
python3 scripts/context/context-load.py resolve "<the request>"   # -> the exact files to read
# then read exactly what it lists, and nothing else
```

`FreshnessCheck` runs on the documented JDK 21 + Git baseline — no build, no dependencies.
`./gradlew governanceFreshness` is equivalent. `BOOT.md` is generated from
[`CURRENT-STATE.yaml`](./docs/governance/state/CURRENT-STATE.yaml) and CI fails if it drifts, so it
is safe to trust and cheap to read.

## 2. The rules that are not negotiable

- **A suggestion is never implemented in the turn it is raised.** Triage it
  ([`aigem-triage`](./.claude/skills/aigem-triage/SKILL.md)), record it, schedule it — then go back
  to the work item you were on.
- **Exactly one work item in flight per agent/owner.** Independent, dependency-safe owners may
  progress in parallel; a blocked item is recorded with owner and date and releases the lane. Only
  the [hard `P1` overrides](./docs/governance/05-PRIORITY_MODEL.md#3-hard-p1-overrides) interrupt.
- **Parked is not deleted.** Every deferral records a target stage and an unpark trigger in
  [`registers/PARKED-BACKLOG.md`](./docs/governance/registers/PARKED-BACKLOG.md).
- **Priority is stage-relative.** Record `priority_now` **and** `priority_at_target`.
- **Do not re-report known debt** — `BOOT.md` section 5, detail in
  [`01 §6`](./docs/governance/01-CURRENT_STATE.md#6-known-open-debt-affecting-triage).
- Every `TODO` carries a work item ID. Nothing is Done without evidence.
- Agents never edit stage state, never approve change requests, and never self-approve a board
  that requires a human. **T4 Architecture, Security and Risk & Compliance sign-offs stay human** —
  an agent may draft the reasoning and assemble the evidence, never the signature.
- **No persona silently crosses its authority boundary.** `Not Authorised` is binding on AI
  behaviour, and expertise is not authority.
- **Do not scale blindly.** Name the business load, the transaction amplification, the *actual*
  bottleneck, the next downstream limit, the safe range and the recovery behaviour. More pods are
  not a diagnosis of an insurer, 1SB, DB, Kafka, cache or payment-system bottleneck.
- **Delivery urgency does not alter specialist authority.** `R12` may make a decision dependency
  time-bound and escalate it; it may never convert missing evidence into approval.

Full binding contract: [`09-AI_EXECUTION_RULES.md`](./docs/governance/09-AI_EXECUTION_RULES.md).
Governance model: [`docs/governance/`](./docs/governance/README.md) (**AIGEM**).

## 3. Personas — load a card, not a package

Every persona below is a **single canonical identity**. Never instantiate a second architect,
product owner, delivery lead, SRE or engineering head because an alias appeared in a request; never
create an eighth review board. Adopt a persona from its **card** (3–6 KB) and open its package only
when a *Load deeper* row in the card matches the question in front of you.

| Persona | Seat | Owns | Card |
|---|---|---|---|
| **Rajal** — Product Owner | Board 3 · `R1` | WHAT / WHY / FOR WHOM, journey behaviour, rules, scope, priority, acceptance, outcome | [card](./docs/context/personas/rajal-product.card.md) |
| **Principal BA** | `R11` (Product delegate) | Process, requirement, rule, information, state and exception clarity; AC quality; traceability | [card](./docs/context/personas/ba-r11-business-analysis.card.md) |
| **Mahesh** — Architect | Board 1 · `R2` | Structure, boundaries, contracts, integration, data ownership, NFR architecture, HLD/LLD, ADRs | [card](./docs/context/personas/mahesh-architecture.card.md) |
| **Amit** — Technical Head | Board 2 · `R3` | Application engineering, coding standards, service patterns, application build/CI correctness | [card](./docs/context/personas/amit-engineering.card.md) |
| **Deepali** — Security Architect | Board 4 · `R8` | Trust boundaries, exposure, IAM security, crypto/secrets, AppSec/API, DevSecOps, threat model | [card](./docs/context/personas/deepali-security.card.md) |
| **Aarti** — Data & Database Architect | Specialist, via existing boards | Persistence suitability, physical model, integrity, DB performance, migrations, backup/DR | [card](./docs/context/personas/aarti-database.card.md) |
| **Swapnali** — QA Lead | Board 5 · `R7` | Risk-based test strategy, critical-journey regression, evidence sufficiency, quality exit | [card](./docs/context/personas/swapnali-qa.card.md) |
| **Shailja S** — Compliance & Risk | Board 6 · `R9` | Regulatory permissibility, PII, consent, retention, mandatory control outcomes | [card](./docs/context/personas/shailja-compliance.card.md) |
| **Shivanshi** — SRE / Reliability | Board 7 · `R10` | Platform operability, CI/CD platform, IaC, SLOs, observability, incidents, resilience, DR, capacity | [card](./docs/context/personas/shivanshi-sre.card.md) |
| **Kalpana** — Delivery Head | `R12` | Integrated plan, critical path, dependencies, capacity, forecast, release orchestration, recovery | [card](./docs/context/personas/kalpana-delivery.card.md) |

**Who decides, who may block, and what each one must never decide alone — one screen:**
[`AUTHORITY-QUICK-CARD.md`](./docs/context/personas/AUTHORITY-QUICK-CARD.md).
**Canonical and binding:**
[`PERSONA-AUTHORITY-MATRIX.md`](./docs/governance/PERSONA-AUTHORITY-MATRIX.md).

**Cross-persona decisions.** When a decision materially affects another persona's governed domain,
consult that persona through the [cross-persona operating
model](./docs/context/roles/shared/cross-persona-operating-model.md) and the authority matrix, plus
the protocol for the domain in play:
[security](./docs/context/roles/shared/security-cross-persona-decision-protocol.md) ·
[SRE](./docs/context/roles/shared/sre-cross-persona-decision-protocol.md) ·
[delivery](./docs/context/roles/shared/delivery-cross-persona-decision-protocol.md) ·
[architecture ↔ compliance](./docs/context/roles/shared/architect-compliance-decision-protocol.md) ·
[product ↔ architecture ↔ compliance](./docs/context/roles/shared/product-architecture-compliance-decision-protocol.md).
Unresolved material conflict escalates to the accountable humans — it is never averaged, defaulted
or silently resolved.

**Aliases resolve to one persona.** `Solution Architect`/`Principal Architect` → Mahesh ·
`DevOps / SRE`/`Reliability Engineering Head`/`Operations`/`R10` → Shivanshi ·
`Delivery Lead`/`Program Delivery Director`/`R12` → Kalpana ·
`Business Analyst`/`Principal BA`/`R11` → the Principal BA ·
`Security Head` → Deepali · `DBA` → Aarti. The full alias set is in
[`context-manifest.yaml`](./docs/context/context-manifest.yaml).

## 4. Finding context without burning the window

| You need | Load |
|---|---|
| The facts before acting | [`docs/context/BOOT.md`](./docs/context/BOOT.md) — tier 0, the only default read |
| The exact files for this task | [`AGENT-CONTEXT-INDEX.yaml`](./docs/context/AGENT-CONTEXT-INDEX.yaml) — 19 capsules, budgeted and CI-checked |
| To triage an input | [`aigem-triage` skill](./.claude/skills/aigem-triage/SKILL.md) |
| To act as a persona | [`docs/context/personas/`](./docs/context/personas/README.md) |
| Where a document lives | [`docs/README.md`](./docs/README.md) — the six buckets and which wins on conflict |
| Safe, non-blocked work to pick up | `python3 scripts/governance/autopilot.py next` |

**Safe autopilot** reports the evidence ledger (`status`), selects automation-eligible non-blocked
work (`next`, `--include-manual` exposes coordination work) and can only emit `CANDIDATE` from
`propose-transition`. It never edits stage state, marks `PASSED`, treats silence as approval or
creates a waiver. Machine contract:
[`GATE-EVIDENCE.yaml`](./docs/governance/state/GATE-EVIDENCE.yaml).

**Humans:** your role card is [`RUNBOOK.md §6`](./docs/governance/RUNBOOK.md#6-role-cards) — one
screen, exact actions, exact cadence. That is all you need to read.

## 5. Repository

Multi-module Gradle (Kotlin DSL) monorepo for the **1SB insurance platform** — **Java 21**,
**Spring Boot**, shared libs under `libs/`, services under `services/`.

| Service | Port | Required? | Notes |
|---|---|---|---|
| `1sb-integration-service` | 8080 | Yes (Phase 1+) | Bank-facing 1SB adapter. **No datasource** — job store via HTTP to bank-persistence. Profiles `local`/`test`/`uat`/`prod`. |
| `bank-persistence-service` | 8081 | Yes, for local job-store / audit HTTP | **Platform-common** persistence (Flyway + JPA + `/internal/v1`); owns the DB for all consumers. H2 (`MODE=PostgreSQL`) local/test, PostgreSQL uat/prod. |
| `audit-consumer-service` | — | Future | Doc stub; will call `/internal/v1/audit-events` on bank-persistence — no second audit DB. |

Architecture invariants enforced in code: bank apps never call 1SB or a database directly · 1SB
specifics live only in `adapter.onesb.*` (ArchUnit) · `1sb-integration-service` owns no Flyway
migrations and no JPA · no PII in logs.

### Build, test, run

```bash
./gradlew test

# Coverage (QA-001 / R7) — reports + gates; HTML/XML under <module>/build/reports/jacoco/test/
./gradlew test jacocoTestReport jacocoTestCoverageVerification

# Local: persistence first (8081), then integration (8080)
./gradlew :services:bank-persistence-service:bootRun --args='--spring.profiles.active=local'
./gradlew :services:1sb-integration-service:bootRun  --args='--spring.profiles.active=local'
# Integration job-store calls need persistence on http://localhost:8081
# (override with BANK_PERSISTENCE_BASE_URL / bank.persistence.base-url)

# Targeted shared-lib verification
./gradlew :libs:bank-common-error:test :libs:bank-common-security:test \
  :libs:bank-common-audit:test :libs:bank-common-secrets:test
```

Module SSOT: [testing rules](./docs/1sb-insurance-integration/service-ssot/TESTING-RULES.md) ·
[QA strategy](./docs/1sb-insurance-integration/service-ssot/QA-LEAD-TESTING-STRATEGY.md) ·
[roles & DoD](./docs/1sb-insurance-integration/service-ssot/ROLE-GUIDELINES-AND-DOD.md) ·
[test backlog](./docs/1sb-insurance-integration/service-ssot/TEST-BACKLOG.md) ·
[coverage gates](./docs/1sb-insurance-integration/service-ssot/COVERAGE.md) (libs 80%/70%; services
interim line floor).

### Environment

**JDK 21** (required for the build) and **Git 2.43+**. Node.js / Python / Go may be present but are
not required for this Java platform. Docker is not required for unit tests. After cloning there is
no install step beyond a Gradle build — the wrapper downloads the toolchain and dependencies.

## 6. Keeping this layer honest

```bash
python3 scripts/context/context-load.py validate   # paths, anchors, budgets, manifest agreement
python3 scripts/context/build-boot-capsule.py      # regenerate BOOT.md after a state change
python3 scripts/context/validate-context.py        # the portable context manifest
python3 scripts/governance/ci-checks.py            # all of the above, in CI
```

A card, capsule or capsule budget that no longer matches its source is a defect: fix it in the same
change. A stale route costs more than no route — the agent trusts it, fails, and explores anyway.
