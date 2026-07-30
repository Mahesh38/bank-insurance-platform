# Test Backlog — QA Lead

**Status:** Open  
**Strategy:** [QA-LEAD-TESTING-STRATEGY.md](./QA-LEAD-TESTING-STRATEGY.md)  
**Rules:** [TESTING-RULES.md](./TESTING-RULES.md)

Priority: **P0** before Phase 3 functional exit · **P1** hardening · **P2** nice-to-have

---

## Baseline debt (today)

| ID | Sev | Item | Owner | Notes |
|----|-----|------|-------|-------|
| QA-001 | P0 | Introduce JaCoCo + CI coverage verification | Dev | **Done (wiring)** — `008ec04`; residual **Partial** (services interim 50% line after QA-002 → package floors QA-003). TL+QA Lead **APPROVED** 2026-07-30. |
| QA-002 | P0 | Persistence API tests: jobs, offers, status patch, payments, audit, `GlobalExceptionHandler` | Dev | **Done** — Job/Payment/Audit API tests + poll-attempt extend; services gate 35%→50%; persistence ~96% line |
| QA-003 | P0 | Integration IT template: service + WireMock 1SB + WireMock/stub persistence | Dev + QA | **Done** — `OneSbConnectivityIT` (IT-I): dual WireMock + `@DynamicPropertySource`; run `./gradlew :services:1sb-integration-service:test --tests '*IT'` |
| QA-004 | P0 | Phase 3 quote path: unit (handler/mapper) + `@WebMvcTest` + IT-I | Dev | Gate FUNC-002 DoD |
| QA-005 | P1 | PR coverage report (Jacoco XML → CI annotation) | DevOps/Dev | |
| QA-006 | P1 | Unit tests: `InMemoryIdempotencyStore`, `OneSbHttpClientPollAdapter`, `SecretsStartupValidator` | Dev | **Done** — `InMemoryIdempotencyStoreTest`, `OneSbHttpClientPollAdapterTest`, `SecretsStartupValidatorTest`; run `./gradlew :services:1sb-integration-service:test` |
| QA-007 | P1 | Lib gaps: `EnvSecretProvider`, `ErrorCodes` usage smoke, security `Role`/`RequiresRole` | Dev | |
| QA-008 | P1 | Dual-service IT-D (optional docker-compose) | QA + Dev | After QA-003 stable |
| QA-009 | P1 | Sandbox E2E charter + automated smoke (nightly) | QA | Secrets-gated |
| QA-010 | P2 | Mutation testing on mappers (PIT) | QA | Phase 4 |
| QA-011 | P2 | k6 performance smoke from NFR table | QA + Eng | Phase 4 |
| QA-012 | P2 | Contract test vs OpenAPI once published | QA | Phase 4 |

---

## Traceability template (Phase 3+)

| Story | AC excerpt | Automated test |
|-------|------------|----------------|
| FUNC-002 | Valid quote → job + 1SB call | `QuoteServiceTest`, `QuoteControllerSliceTest`, `QuoteFlowIT` |
| FUNC-002 | Invalid → 422, no 1SB | WireMock verify 0 |
| FUNC-002 | Poll timeout → QUOTE_TIMEOUT | `AsyncJobPoller` + quote orchestration IT |
| NFR-001 | Idempotent POST | `IdempotencyFilterTest` + controller slice |
| COMP-002 | No PII in logs | `PiiMaskerTest` + audit hash test |

---

## Sprint recommendation

**Immediate (before/alongside first Phase 3 story):** QA-001, QA-002, QA-003, QA-006  
**With FUNC-002:** QA-004  
**Phase 4:** QA-008…012

---

## Sign-off

QA Lead: strategy and rules published. Engineering must treat R2 (same-PR tests) as blocking review criterion starting now.
