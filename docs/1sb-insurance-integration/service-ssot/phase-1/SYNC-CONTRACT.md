# Phase 1 — Dual-dev sync contract

**Branch:** `cursor/phase1-foundations-c259`  
**Goal:** Complete E00 foundations (SHARED-001…004 + TECH-001…003) without merge conflicts.

## Ownership (do not cross)

| Dev | Owns | Paths |
|-----|------|-------|
| **Dev A** | Shared libraries | `libs/bank-common-error/`, `libs/bank-common-security/`, `libs/bank-common-audit/`, `libs/bank-common-observability/` |
| **Dev B** | Integration service scaffold | `services/1sb-integration-service/` (+ root Gradle settings that include both) |

## Shared contracts (both must honour)

1. **Build:** Gradle multi-project, Java **21**, Spring Boot **3.3.x**
2. **Root:** `settings.gradle.kts` includes `libs:*` and `services:1sb-integration-service`
3. **Group:** `com.bank.insurance`
4. **Common lib packages:**
   - `com.bank.common.error`
   - `com.bank.common.security`
   - `com.bank.common.audit`
   - `com.bank.common.observability`
5. **Service base package:** `com.bank.insurance.onesb` (match architecture §3)
6. **Secrets:** `INSURANCE_SECRETS_SOURCE=PROPERTIES|ENV|AWS_SECRETS_MANAGER` (see `config/onesb/secrets-source.example.yaml`)
7. **No real secrets** in git; use placeholders + `application-local.properties.example`
8. **ArchUnit:** nothing outside `adapter.onesb` imports onesb client types (even if stub)
9. **Life LOB:** handlers later; scaffold packages may include `lob/life/saving` and `lob/life/term` empty packages

## Sync points

- Dev A publishes modules with minimal public APIs first so Dev B can depend on them.
- Dev B may use `implementation(project(":libs:bank-common-error"))` etc.; if libs not ready, add dependency declarations and stub compile with empty jars only if blocked — prefer waiting on A’s module skeletons.
- Both update `docs/.../phase-1/STATUS.md` with Done/In-progress.

## Exit criteria (Phase 1)

- `./gradlew build` succeeds
- Service boots; `/actuator/health` UP (DB may use H2/Testcontainers for CI)
- Flyway migrations present for architecture §9 tables
- Shared libs have unit tests
- Phase 1 status doc updated
