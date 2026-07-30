# AGENTS.md

Guidance for cloud agents working in this repository.

## Repository status

Multi-module Gradle (Kotlin DSL) monorepo for the **1SB insurance platform**:

- **Java 21** / **Spring Boot 3.3.4**
- Shared libs under `libs/`
- Services under `services/`:
  - `1sb-integration-service` (port **8080**) — bank-facing 1SB adapter; no local DB
  - `1sb-persistence-service` (port **8081**) — owns Flyway schema + JPA; internal HTTP only

## Cursor Cloud specific instructions

### Services

| Service | Required? | Notes |
|---------|-----------|-------|
| `1sb-integration-service` | Yes (Phase 1+) | Boot app; profiles `local` / `test` / `uat` / `prod`. No datasource — job store via HTTP to persistence. |
| `1sb-persistence-service` | Yes (for local job-store calls) | Boot app on **8081**; owns DB. Local/test use **H2** (`MODE=PostgreSQL`); uat/prod use PostgreSQL. |
| PostgreSQL / H2 | Persistence service only | Integration service never embeds a DB. |

### System tooling (VM)

- **JDK 21** (required for build)
- **Git** 2.43+
- Node.js / Python / Go may also be present but are not required for this Java platform

Docker is not required for unit tests.

### Lint / test / run

```bash
./gradlew test

# Local: start persistence first (8081), then integration (8080)
./gradlew :services:1sb-persistence-service:bootRun --args='--spring.profiles.active=local'
./gradlew :services:1sb-integration-service:bootRun --args='--spring.profiles.active=local'
# Integration job-store calls need persistence on http://localhost:8081
# (override with INSURANCE_PERSISTENCE_BASE_URL / insurance.persistence.base-url)
```

Targeted shared-lib verification:

```bash
./gradlew :libs:bank-common-error:test :libs:bank-common-security:test \
  :libs:bank-common-audit:test :libs:bank-common-secrets:test
```

### Update script

After cloning, no separate install step beyond a Gradle build (wrapper downloads the toolchain/deps).
