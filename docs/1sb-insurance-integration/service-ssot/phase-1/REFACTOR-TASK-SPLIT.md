# Phase 1 Remediation — Task Split (Agent 2 / Agent 3)

**Authority:** [TECH-LEAD-REVIEW.md](./TECH-LEAD-REVIEW.md) · [TECH-DEBT.md](../TECH-DEBT.md)  
**Branch:** `cursor/phase1-foundations-c259`

---

## Parallelism rules

1. **Independent first** — start work that does not block the other agent.
2. **Dependent chain stays with one agent** — never split a prerequisite across agents.
3. Sync points: commit + push after each closed TD; re-run `./gradlew build`.
4. Do not edit the other agent’s primary ownership paths without coordinating via STATUS.md.

---

## Agent 2 — Shared platform quality (independent track)

**Owns:** `libs/**` (existing + new secrets), Lombok Gradle wiring, light doc hygiene.

| Order | Task | TD | Depends on | Notes |
|------:|------|----|------------|-------|
| A2-1 | Add Lombok to root `subprojects` (compileOnly + annotationProcessor + test) | TD-001 | — | Start immediately |
| A2-2 | Refactor `AuditEvent`, `ServiceErrorResponse`, `BankPrincipal` to Lombok | TD-001 | A2-1 | Keep public API / tests green |
| A2-3 | Remove unused `ErrorCode` if duplicate of `ErrorCodes` | TD-005 | — | Parallel with A2-2 OK |
| A2-4 | Create `:libs:bank-common-secrets`; move providers + tests | TD-002 | — | Parallel with A2-1..3 |
| A2-5 | Retarget integration service secret wiring to the new lib; delete old package | TD-002 | A2-4 | Touch `services/1sb-integration-service` secrets/config only |
| A2-6 | Document Lombok vs records convention in `libs/README.md` | TD-012 | A2-2 | |
| A2-7 | Fix AGENTS.md / libs README version drift | TD-008 | — | Quick |

**Must not:** Create persistence service; move Flyway; add inter-service HTTP.

**Conflict zone:** If Agent 3 is rewriting `services/1sb-integration-service/build.gradle.kts`, Agent 2 only adds `bank-common-secrets` dependency and removes secret sources — leave JPA/Flyway removal to Agent 3.

---

## Agent 3 — Persistence split + HTTP (dependent chain)

**Owns:** new persistence service, Flyway move, strip DB from integration, HTTP contract + client.

| Order | Task | TD | Depends on | Notes |
|------:|------|----|------------|-------|
| A3-1 | Scaffold `:services:1sb-persistence-service` (Boot app, profiles, actuator) | TD-003 | — | Start immediately (parallel with Agent 2) |
| A3-2 | Move Flyway `V1__init_schema.sql` into persistence; wire Flyway+JPA+H2/PG | TD-011 | A3-1 | |
| A3-3 | Add JPA entity stubs + Spring Data repos for the 6 tables | TD-003 | A3-2 | Use Lombok per TD-012 **if** Agent 2 already added Lombok; else plain JPA until Lombok lands then apply |
| A3-4 | Internal REST API `/internal/v1/...` for job/offer/audit/payment stubs | TD-004 | A3-3 | Problem JSON via bank-common-error |
| A3-5 | Integration: add RestClient `JobStorePort` HTTP adapter + config `insurance.persistence.base-url` | TD-004 | A3-4 | |
| A3-6 | Remove Flyway, JPA, drivers, `db/migration`, empty persistence entity packages from integration | TD-011 | A3-5 | Keep ArchUnit updated |
| A3-7 | ArchUnit: forbid `jakarta.persistence` / `org.flywaydb` in integration service | TD-004 | A3-6 | |
| A3-8 | Persistence README + OpenAPI/markdown internal API list | TD-004 | A3-4 | |

**Must not:** Move secrets lib (Agent 2); rewrite shared builder classes.

---

## Sync contract (file ownership)

| Path | Owner |
|------|-------|
| `libs/bank-common-*` (error/security/audit/observability) | Agent 2 |
| `libs/bank-common-secrets/**` | Agent 2 |
| `build.gradle.kts` (root Lombok) | Agent 2 |
| `settings.gradle.kts` | Both may add includes — **append only**; do not reorder other’s modules |
| `services/1sb-persistence-service/**` | Agent 3 |
| `services/1sb-integration-service/**` secrets/config | Agent 2 for secrets; Agent 3 for DB/HTTP |
| `docs/.../TECH-DEBT.md` status rows | Both update own TDs |
| `docs/.../phase-1/STATUS.md` | Both append their section |

---

## Suggested first commits

- Agent 2: `chore(td-001): add lombok and refactor shared builders`
- Agent 2: `feat(td-002): extract bank-common-secrets library`
- Agent 3: `feat(td-003): scaffold 1sb-persistence-service with Flyway`
- Agent 3: `feat(td-004): internal persistence HTTP API and integration RestClient adapter`

---

## Definition of done (joint)

See TECH-LEAD-REVIEW §6. After both agents finish, Tech Lead runs a **second confirmation pass** for gaps vs senior comments.
