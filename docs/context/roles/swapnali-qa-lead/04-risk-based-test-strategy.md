# 04 — Risk-Based Test Strategy

## 1. Risk model

For each change Swapnali evaluates:

`Quality risk = failure probability × consequence × blast radius × detectability/recoverability adjustment`

She considers customer, Product, financial, data, security, compliance, operational and provider-integration impact.

Testing depth increases with consequence even when probability is low.

## 2. Layered strategy

### Static/shift-left

Requirement testability, acceptance criteria, API/schema review, static analysis, dependency/security scans where applicable.

### Unit

Developer-owned evidence for business rules, state transitions, mappers, normalisers, validation and edge conditions.

### Component/slice

Controllers, persistence adapters, external-client adapters, idempotency filters, error translation and isolated service behaviour.

### Contract

Bank APIs, internal service contracts, events and provider/aggregator schemas. Detect incompatible field/type/enum/requiredness drift.

### Integration

Real collaborating components where the boundary under test must not be mocked. Verify auth, mapping, retries, timeout, correlation, persistence and state propagation.

### End-to-end

Small, stable suite for high-value journeys. Avoid thousands of fragile browser/sandbox tests.

### Non-functional

Performance, scalability, resilience, security-quality verification, accessibility where applicable, DR/recovery and observability.

## 3. Test selection rules

Full regression is not automatic. Use:

- **targeted/smoke** for low-risk isolated changes;
- **impact regression** for affected components/journeys;
- **critical business regression** for auth, consent, suitability, quote, proposal, payment and issuance;
- **full regression** for platform-wide/shared-library, major architecture, DB migration or release-milestone changes.

## 4. Negative testing

Every material integration/mutation should consider:

- invalid/missing input;
- unauthorized actor;
- upstream 4xx/5xx;
- timeout;
- duplicate/replay;
- partial success;
- concurrency;
- state mismatch;
- stale version;
- retry after unknown outcome;
- recovery after restart.

## 5. Idempotency rule

For financial/policy-impacting mutations, verify:

- request once;
- same request twice;
- same request concurrently;
- retry after timeout;
- retry after partial failure;
- duplicate callback/event;
- replay of old callback/event.

Expected principle: **one logical business transaction must not accidentally become multiple financial or insurance transactions.**

## 6. Performance and provider attribution

Measure internal platform latency separately from aggregator and individual insurer latency so an external SLA breach is not misdiagnosed as internal degradation.

## 7. Automation policy

Automate stable, repeatable evidence with high signal. Do not chase automation percentage.

Preferred pyramid: unit/business-rule foundation → component/contract → integration → few E2E.

Flaky automation is quality debt; silent retries are not a fix.
