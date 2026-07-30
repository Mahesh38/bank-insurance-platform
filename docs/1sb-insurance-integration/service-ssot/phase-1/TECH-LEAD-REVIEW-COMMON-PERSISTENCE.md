# Tech Lead Review — Common Persistence Ownership

**Role:** Tech Lead (20+ years application development)  
**Branch:** `cursor/phase1-foundations-c259`  
**Date:** 2026-07-30  
**Audience:** Dev team executing remediations  
**Senior comment (accepted in full):**

> The persistence service doesn't belong to the 1SB-Integration-service. It is a **common persistence service** used across multiple microservices for DB operations. All Flyway scripts stay in this service. The **audit consumer service** will also use the same persistence service to store audit-related records.

---

## 1. Verdict

Prior remediations correctly **split** DB ownership out of `1sb-integration-service` (TD-003/004/011). That split is structurally right.

What is still wrong is **ownership semantics and naming**:

| Current | Problem |
|---------|---------|
| Module `1sb-persistence-service` | Implies 1SB-only ownership |
| Docs topology “integration owns durable state via 1sb-persistence” | Hides multi-consumer platform role |
| Package `com.bank.insurance.persistence` | Insurance-scoped; OK-ish but prefer platform `com.bank.persistence` |
| Config `insurance.persistence.base-url` | Ties client config to insurance domain |
| Flyway header “for 1SB Integration Service” | Ownership lie |
| Persistence README “callers = 1sb-integration” | Omits audit-consumer and future services |

**Decision:** Reposition persistence as a **bank platform common service**. Rename accordingly. Document multi-consumer contract. Do **not** move Flyway back into any consumer. Do **not** create a full audit-consumer microservice in this loop — document it as a first-class future client of the same HTTP + Flyway service.

---

## 2. Target topology

```text
                    ┌─────────────────────────────┐
  1sb-integration ──┤                             │
  service (:8080)   │   bank-persistence-service  │── PostgreSQL
                    │   (:8081)                   │   (Flyway ONLY here)
  audit-consumer ───┤   /internal/v1/*            │
  service (future)  │                             │
  (other MS later) ─┤                             │
                    └─────────────────────────────┘
```

Rules:
1. **One** persistence deployable owns **all** Flyway scripts for shared insurance/platform tables (jobs, offers, audit_event, payment, …).
2. Consumers talk **HTTP only** — no shared DataSource, no consumer-side Flyway.
3. `audit_event` API is already on persistence; audit-consumer will call `POST /internal/v1/audit-events` (and list) — same service as integration.
4. Naming must not say `1sb-*` for this module.

---

## 3. Naming decisions (binding)

| Artifact | From | To |
|----------|------|----|
| Gradle module / folder | `services/1sb-persistence-service` | `services/bank-persistence-service` |
| JAR / `spring.application.name` | `1sb-persistence-service` | `bank-persistence-service` |
| Java base package | `com.bank.insurance.persistence` | `com.bank.persistence` |
| Entity/repo packages | `…persistence.persistence.entity/repo` | `com.bank.persistence.entity` / `.repo` |
| Client config prefix | `insurance.persistence` | `bank.persistence` |
| Env var | `INSURANCE_PERSISTENCE_BASE_URL` | `BANK_PERSISTENCE_BASE_URL` |

Keep HTTP paths `/internal/v1/...` unchanged (stable contract).

Aligns with existing `bank-common-*` library naming.

---

## 4. Disposition of senior comment

| Senior point | TL disposition | Tracking |
|--------------|----------------|----------|
| Persistence ≠ owned by 1SB integration | **Accept** — platform common service | TD-016, TD-017 |
| Used across multiple microservices | **Accept** — multi-consumer contract | TD-017, TD-020 |
| Flyway stays on this service | **Already true** — reinforce in docs; never reverse | TD-011 (closed) + TD-017 |
| Audit consumer uses same persistence for audit records | **Accept** — document + keep `/audit-events`; stub consumer note | TD-020, TD-021 |

---

## 5. Additional findings (this review)

| Finding | Sev | Action |
|---------|-----|--------|
| Module still named `1sb-persistence-*` | P0 | TD-016 |
| Docs still 1SB-owned framing | P0 | TD-017 |
| Package under `insurance.persistence` + double `.persistence.persistence` | P1 | TD-018 |
| Client config `insurance.persistence.*` | P1 | TD-019 |
| No written multi-consumer / audit-consumer contract | P1 | TD-020 |
| Audit-consumer service not scaffolded | P2 | TD-021 — doc + optional placeholder README only (no full service this loop) |
| Root Gradle name `1sb-insurance-platform` | P3 | Leave (platform umbrella OK); optional later rename |

---

## 6. Fix plan — see task split

[REFACTOR-COMMON-PERSISTENCE.md](./phase-1/REFACTOR-COMMON-PERSISTENCE.md)

---

## 7. Exit criteria

- [x] Module path `services/bank-persistence-service` in settings + builds
- [x] No remaining `1sb-persistence-service` references in code/build (docs may note rename history once)
- [x] Packages under `com.bank.persistence`
- [x] Client uses `bank.persistence.base-url` / `BANK_PERSISTENCE_BASE_URL`
- [x] Platform persistence contract doc lists consumers: integration + audit-consumer (future)
- [x] Flyway still only on bank-persistence-service
- [x] `./gradlew build` green
- [x] TECH-DEBT TD-016…020 closed (021 deferred or doc-only closed)
- [x] Second confirmation circle finds no gaps vs senior comment

---

## 8. Non-goals (this loop)

- Implementing full `audit-consumer-service` runtime (Kafka/SQS listener, etc.)
- Moving `audit_event` table ownership elsewhere
- Changing `/internal/v1` URL paths
- Merging unrelated Phase 2 1SB HTTP client work
