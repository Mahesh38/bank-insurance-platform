# Layer 2 — Organization Standards

**Layer:** L2 — organization-wide; adapt once, reuse across repositories
**Status:** Draft — reflects standards already evidenced in this repository
**Owner:** Platform / Solution Architect

---

## 1. Purpose

Layer 2 holds the standards that are true for **every project in the organization** and that
the review boards check against. Without it, each repository re-argues the same questions and
the Architecture and Security boards have nothing objective to cite.

This file is deliberately short. It records what is already binding here — it does not invent
new policy. Items marked ⚠️ need organizational ratification.

---

## 2. Architecture principles

| # | Principle | Board check |
|---|-----------|-------------|
| AP-1 | **SOLID + DRY + KISS**, in that order of tie-breaking; KISS wins ties against speculative reuse | A9, T5 |
| AP-2 | **Hexagonal boundaries.** External systems sit behind ports; vendor types never leak past their adapter package | A1, A2 |
| AP-3 | **Replaceable middleware.** Any third-party integration must be swappable without touching orchestration | A3, A10 |
| AP-4 | **One owner per data store.** Services reach other services' data over HTTP contracts, never by sharing a database | A1, A7 |
| AP-5 | **Contract-first for public APIs.** OpenAPI is the source of truth; generated docs follow the contract | T4, P4 |
| AP-6 | **Boundaries are enforced by tests,** not by convention (ArchUnit or equivalent) | A4 |
| AP-7 | **No new runtime component without an ADR** stating its operational cost | A6, X7 |
| AP-8 | **Stage-appropriate simplicity.** Simplicity that suits the current stage is not debt | X5, [15 §1](./15-TECH_DEBT_POLICY.md#1-definition) |

## 3. Security baseline

| # | Standard |
|---|----------|
| SEC-1 | No secrets in source, config, logs, or test fixtures. Secrets come from the secrets SPI |
| SEC-2 | No PII in logs — masking is verified by an automated test, not by review alone |
| SEC-3 | Default deny for authorization; least privilege by role and scope |
| SEC-4 | TLS in transit; encryption at rest for sensitive persisted payloads |
| SEC-5 | All external input validated at the boundary; fail closed |
| SEC-6 | Security-relevant events are auditable with actor attribution |
| SEC-7 | Provider-specific identity technology stays behind an adapter; clients never receive provider tokens |
| SEC-8 | Dependencies are scanned; a reachable vulnerability is a P1 override (O2) |
| SEC-9 | Security debt expires no later than the next stage gate |

## 4. Compliance baseline

| # | Standard |
|---|----------|
| CMP-1 | Regulated actions are auditable, attributable, and traceable to a requirement |
| CMP-2 | Consent is captured and referenced where the regime requires it |
| CMP-3 | Retention periods are explicit, configurable, and enforced by a job — not by hope ⚠️ |
| CMP-4 | Bulk and privileged changes use maker-checker |
| CMP-5 | Raw third-party payloads, where retained, are encrypted and access-controlled |
| CMP-6 | **Compliance debt is never accepted** ([15 §7](./15-TECH_DEBT_POLICY.md#7-debt-and-the-review-boards)) |

## 5. Quality baseline

| # | Standard |
|---|----------|
| Q-1 | Shared libraries: line coverage ≥ 80%, branch ≥ 70% |
| Q-2 | Services: interim line floor, rising per stage ([COVERAGE.md](../1sb-insurance-integration/service-ssot/COVERAGE.md)) |
| Q-3 | Every story covers happy path, validation failure, and a mapped upstream error |
| Q-4 | A bug fix ships with a regression test that fails before the fix |
| Q-5 | Architecture boundary tests run in CI on every build |
| Q-6 | Test data contains no real PII |

## 6. Delivery standards

| # | Standard |
|---|----------|
| D-1 | Small, reviewable batches; one work item per branch where practical |
| D-2 | Commits reference the work item ID |
| D-3 | Definition of Done requires evidence, never assertion |
| D-4 | Only the Tech Lead moves an item to Done |
| D-5 | Deliberate shortcuts are logged with owner, severity, and expiry |
| D-6 | Phase exits are gated by a written review |

## 7. AI agent standards

| # | Standard |
|---|----------|
| AI-1 | Agents follow [09-AI_EXECUTION_RULES.md](./09-AI_EXECUTION_RULES.md) in every repository |
| AI-2 | Agents never self-approve boards that require a human at T4 |
| AI-3 | Agent-authored changes are labelled as such in the PR |
| AI-4 | Agents report partial or failed outcomes honestly, with the actual output |
| AI-5 | Agents never edit stage state or approve change requests |
| AI-6 | Agent suggestions are registered, not implemented in place |

---

## 8. Ratification backlog ⚠️

Items in this file that need an organizational owner before boards can cite them as binding:

| Item | Needs | Owner |
|------|-------|-------|
| CMP-3 retention periods | Confirmed regime per data class (7-year default is provisional for auth events) | Compliance |
| SEC-8 scanning | Named tool, cadence, and the SLA for a reachable finding | Security |
| Q-2 service coverage floor | The per-stage schedule, so "interim" ends | QA Lead |
| AP-7 ADR requirement | Confirmation that it applies to infrastructure, not only application components | Architect |
