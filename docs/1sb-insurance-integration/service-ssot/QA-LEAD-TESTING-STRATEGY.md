# QA Lead — Testing Strategy

**Canonical QA persona:** [Swapnali — Principal Insurance Quality Engineering / QA Lead](../../context/roles/swapnali-qa-lead/README.md)  
**Role:** QA Lead  
**Date:** 2026-08-14  
**Applies to:** `1sb-integration-service`, `bank-persistence-service`, `libs/bank-common-*`  
**SSOT companions:** [TESTING-RULES.md](./TESTING-RULES.md) · [COVERAGE.md](./COVERAGE.md) · [TEST-BACKLOG.md](./TEST-BACKLOG.md)  
**Repository-wide quality metrics:** [AIGEM Governance Metrics §3](../../governance/18-GOVERNANCE_METRICS.md#3-merged-quality-health-metrics)

---

## 1. Purpose and source-of-truth rule

This file is the **service execution strategy** for QA. Swapnali's persona package defines platform-wide behaviour, authority, insurance criticality and waiver rules.

Do not duplicate metric values:

- line/branch coverage thresholds and measured coverage → `COVERAGE.md`;
- QA backlog/status → `TEST-BACKLOG.md`;
- repository-wide quality-health metrics → `docs/governance/18-GOVERNANCE_METRICS.md`.

## 2. Current baseline

The repository has meaningful unit/component coverage, WireMock-based integration templates and enforced coverage gates, but still requires progressive bank-grade contract, multi-service, resilience, performance and sandbox journey evidence as the product journey expands.

Current numeric coverage is intentionally not copied here; read `COVERAGE.md`.

## 3. Testing pyramid

```text
                 ┌─────────────┐
                 │  E2E / UAT  │  Few, slow, env-gated
                 ├─────────────┤
                 │ Integration │  Service+DB / dual-service HTTP
                 ├─────────────┤
                 │   Slice     │  MVC/JPA/WireMock/component
                 ├─────────────┤
                 │    Unit     │  Majority — business rules/mappers/policies
                 └─────────────┘
```

| Layer | Target share of automated effort | Reliability posture |
|---|---:|---|
| Unit | 60–70% | Zero flake tolerance |
| Slice/component | 15–20% | Low flake |
| Integration | 10–15% | Stable; quarantine only with tracked debt |
| E2E/sandbox | ~5% | Gated/nightly/manual promote acceptable |

Coverage is a floor, not a goal.

## 4. Required test types

### Unit — developer owned

Business rules/state transitions, mappers/normalisers, validation, edge cases, utilities and shared libraries. Same-PR rule applies.

### Slice/component — developer owned, QA reviewed

`@WebMvcTest`, repository/database slices, WireMock/MockRestServiceServer and architecture/static rules where appropriate.

### Integration — Engineering implements, Swapnali owns scenario sufficiency

- persistence service + DB/API;
- integration service + realistic provider/persistence boundaries;
- dual-service integration where risk justifies it.

### Contract

OpenAPI/internal API/event/provider schema compatibility. Introduce consumer-driven tooling only where multiple consumers and change risk justify the cost.

### E2E/sandbox

Few high-value journeys such as quote → proposal → payment → status/issuance, secrets-gated and not required on every PR.

### Non-functional

Performance, resilience/fault injection, security-quality evidence, observability/recovery, and accessibility where relevant.

## 5. Insurance criticality overlay

When changes affect authn/authz, consent, suitability/eligibility, premium/sum assured, proposal declarations, payment, issuance, reconciliation, PII, audit or idempotency, load Swapnali's protected-gate rules and require risk-proportionate evidence beyond raw coverage.

Payment success must never be treated as equivalent to policy issuance.

## 6. Ownership

| Activity | Engineering/Developer | Swapnali QA | Other authority |
|---|---|---|---|
| Unit/component tests | **R/A implementation** | RV gaps | — |
| Integration automation | R | **A scenario sufficiency** | Aarti/Security/Shailja consulted as applicable |
| E2E/sandbox strategy | C/R | **O/A** | Product provides journey acceptance |
| Coverage wiring in Gradle/CI | **R** | **A thresholds/waiver** | — |
| Test data | C/R | **A quality** | Shailja/Security controls where sensitive |
| Flake policy | R fixes | **A** | — |
| Performance scripts | R | A scenarios | Architecture/Engineering NFR owners |
| Exploratory/UAT quality evidence | C | **O/A** | Product participates in business acceptance |
| DB migration/recovery evidence | R | RV/A quality | **Aarti owns DB guarantees** |

## 7. Developer DoD

Follow [TESTING-RULES.md](./TESTING-RULES.md): same-PR tests, realistic boundary tests, no real secrets/PII, green local/CI evidence and canonical coverage gates.

## 8. AI/automation rules

Agents may generate test scaffolds, edge cases, provider stubs, masked fixtures, regression summaries and performance scripts. They must not weaken assertions, fabricate execution, commit live credentials/PII, or approve their own evidence where human review is required.

## 9. Coverage

All numeric coverage targets and current measured values live in [COVERAGE.md](./COVERAGE.md). Do not copy them here.

A coverage waiver requires the canonical QA waiver process plus the existing TECH-DEBT/expiry control required by `TESTING-RULES.md`.

## 10. Traceability

Material acceptance criteria map to test/evidence identifiers. Q0/Q1 and protected quality-gate paths require complete traceability before quality exit unless a genuinely permitted governed exception is recorded.

## 11. Metrics

Swapnali reads quality-health metrics from [`docs/governance/18-GOVERNANCE_METRICS.md`](../../governance/18-GOVERNANCE_METRICS.md), especially:

- critical-journey evidence coverage;
- acceptance traceability;
- canonical coverage-gate pass;
- open Q0 findings;
- critical production escapes;
- flake rate;
- expired QA waivers;
- release evidence freshness;
- critical idempotency/reconciliation evidence.

The service strategy adds no competing metric targets.

## 12. Immediate execution source

Current QA-001+ status and next work remain in [TEST-BACKLOG.md](./TEST-BACKLOG.md). Update the backlog rather than freezing dated “next actions” in this strategy.
