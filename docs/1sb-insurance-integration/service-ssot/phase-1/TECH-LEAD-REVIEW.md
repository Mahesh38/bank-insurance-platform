# Tech Lead Review — Phase 1 + Senior Feedback

**Role:** Tech Lead (20+ years application development)  
**Branch:** `cursor/phase1-foundations-c259`  
**Date:** 2026-07-30  
**Audience:** Dev team (Agent 2 / Agent 3) executing remediations

---

## 1. Verdict on Phase 1 delivery

Phase 1 **met its exit gate** as a foundations cut: multi-module Gradle, shared libs with tests, Boot scaffold, ArchUnit hex skeleton, secrets SPI + fail-fast, Flyway V1 schema, actuator health via context test.

That said, senior review correctly flags **structural debt introduced early**:

1. Boilerplate in shared libs will compound as DTOs/entities land.
2. Secrets locked inside the integration service violate DRY across the bank platform.
3. Co-locating Flyway/JPA with orchestration couples the replaceable middleware service to a database it should eventually treat as an infrastructure dependency — wrong boundary for a multi-service bank estate.

**Decision:** Accept Phase 1 functional exit; **do not start Phase 2 connectivity** until TD-001…004 / TD-011 are closed on this branch. Deferred items (AWS SM stub, Redis, missing ports) remain Phase 2 backlog.

---

## 2. Disposition of senior comments

| # | Senior comment | TL disposition | Tracking |
|---|----------------|----------------|----------|
| 1 | Too much boilerplate → use Lombok | **Accept.** Domain records stay; Lombok for builder-heavy shared types + future JPA. | TD-001, TD-012 |
| 2 | Extract secrets to small shared lib | **Accept.** SPI + providers → `bank-common-secrets`. | TD-002 |
| 3 | Move all DB-related code to separate service | **Accept.** New `1sb-persistence-service`. | TD-003 |
| 4 | Flyway stays on the DB service | **Accept.** Move V1 migration; strip from integration. | TD-011 |
| 5 | Services talk over HTTP API | **Accept.** Internal REST + RestClient port adapter. | TD-004 |

No push-back. These align with SOLID (SRP), DRY, and replaceable middleware goals.

---

## 3. Additional findings (Phase 1 code review)

| Finding | Severity | Action |
|---------|----------|--------|
| Hand-rolled builders in audit/error/security | P1 | TD-001 |
| Secrets package inside integration service | P0 | TD-002 |
| Empty `adapter/persistence` + Flyway on wrong service | P0 | TD-003/011 |
| Duplicate `ErrorCode` / `ErrorCodes` | P2 | TD-005 |
| Prod profile requires AWS SM but provider stubs | P2 | TD-006 (defer) |
| ArchUnit `allowEmptyShould(true)` | P3 | TD-007 (defer) |
| README / AGENTS.md version & placeholder drift | P3 | TD-008 |
| Incomplete outbound ports vs architecture | P2 | TD-009 (defer) |
| No Redis/idempotency yet | P2 | TD-010 (defer) |

**Architecture note:** Splitting persistence into its own deployable is a **platform evolution** beyond the original “single integration JAR” SSOT sentence. Update SSOT README one-liner to:

> Bank apps call **1sb-integration-service**; that service orchestrates 1SB and persists state via **1sb-persistence-service** (HTTP). Bank apps never call 1SB or the persistence DB directly.

---

## 4. Target module topology (after fixes)

```text
libs/
  bank-common-error
  bank-common-security
  bank-common-audit
  bank-common-observability
  bank-common-secrets          ← NEW (TD-002)

services/
  1sb-integration-service      ← orchestration, 1SB adapters, HTTP→persistence
  1sb-persistence-service      ← NEW (TD-003): Flyway, JPA, internal REST
```

**Dependency rules:**
- Libs never depend on services.
- `bank-common-secrets` has no Spring Boot dependency required for SPI (Spring optional in consumers).
- Integration service **must not** depend on persistence service as a Gradle project for domain code — only HTTP. (No shared entity JAR.)
- Persistence service may use `bank-common-error` (+ optionally audit/observability) for problem JSON consistency.

---

## 5. Fix plan for the team

See [REFACTOR-TASK-SPLIT.md](./phase-1/REFACTOR-TASK-SPLIT.md) for Agent 2 / Agent 3 ownership and ordering.

**Principles for implementers:**
- Prefer smallest change that closes the debt ID.
- Do not implement Phase 2 quote/poll business logic in this pass.
- Keep H2 local profile working on **persistence** service.
- Integration local profile points `insurance.persistence.base-url` at persistence.
- Preserve existing shared-lib public method names where tests depend on them.
- Update [TECH-DEBT.md](./TECH-DEBT.md) status when closing items.
- Update [phase-1/STATUS.md](./phase-1/STATUS.md) with refactor notes.

---

## 6. Exit criteria for this remediation loop

- [ ] `./gradlew build` green (all modules including new ones)
- [ ] Lombok on shared builders; convention documented
- [ ] Secrets only in `bank-common-secrets` (+ service wiring)
- [ ] Flyway only under `1sb-persistence-service`
- [ ] Integration service has no `data-jpa` / Flyway / `db/migration`
- [ ] HTTP API stubs on persistence + client adapter skeleton on integration
- [ ] TECH-DEBT.md updated (TD-001…004, TD-011 closed or in-progress with clear residual)
- [ ] Second TL pass finds no gaps vs senior comments

---

## 7. Explicit non-goals (this loop)

- Real AWS Secrets Manager SDK
- Quote/proposal HTTP to 1SB (TECH-004+)
- Full JPA repository business logic beyond CRUD stubs needed for HTTP contract
- Changing bank public API paths
- Merging to main before second TL confirmation pass
