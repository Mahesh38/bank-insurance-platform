# AGENTS.md

Guidance for cloud agents working in this repository.

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
