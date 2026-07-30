# Task Split — Common Persistence Repositioning

**Authority:** [TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md](./TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md) · [TECH-DEBT.md](../TECH-DEBT.md)  
**Branch:** `cursor/phase1-foundations-c259`

---

## Parallelism rules

1. Independent work starts immediately.
2. Dependent rename chain stays with **one** agent (Agent 3).
3. Agent 2 may write docs using the **target** name `bank-persistence-service` even before the folder exists; Agent 3 owns the physical rename.
4. Sync: commit/push often; resolve `settings.gradle.kts` carefully.

---

## Agent 2 — Docs, contract, tech-debt (independent track)

| Order | Task | TD | Notes |
|------:|------|----|-------|
| B2-1 | Add/expand TECH-DEBT rows TD-016…021 (if TL seeded, update status as you go) | — | Keep log accurate |
| B2-2 | Write `docs/.../architecture/bank-persistence-service.md` (or under service-ssot) — **platform common persistence contract**: multi-consumer, Flyway ownership, audit-consumer usage of `/audit-events` | TD-017, TD-020 | Start immediately |
| B2-3 | Rewrite persistence service README for **bank-persistence-service** framing (Agent 3 will move file with folder; if rename not done yet, edit current README and Agent 3 preserves content on move) | TD-017 | Coordinate |
| B2-4 | Update SSOT README product one-liner, AGENTS.md, integration README — common service language; list audit-consumer as future consumer | TD-017 | |
| B2-5 | Fix Flyway SQL header comments (drop “for 1SB Integration Service”) | TD-017 | Touch migration file OK |
| B2-6 | Stub doc only: `docs/.../audit-consumer-service.md` — how it will call bank-persistence for audit records (no full Boot app required) | TD-021 | Doc-only close |
| B2-7 | Update STATUS.md + confirmation notes | — | |

**Must not:** Physically rename the Gradle module folder (Agent 3). May update string references in docs.

---

## Agent 3 — Module rename + package + client wiring (dependent chain)

| Order | Task | TD | Depends |
|------:|------|----|---------|
| B3-1 | Rename folder `services/1sb-persistence-service` → `services/bank-persistence-service`; update `settings.gradle.kts`, `build.gradle.kts` JAR name, `spring.application.name` | TD-016 | — |
| B3-2 | Move Java packages `com.bank.insurance.persistence` → `com.bank.persistence`; flatten `persistence.persistence` → `entity` / `repo` | TD-018 | B3-1 |
| B3-3 | Integration client: `bank.persistence.base-url`, `BANK_PERSISTENCE_BASE_URL`, rename properties class if needed; update YAML + tests + HttpJobStoreAdapter | TD-019 | B3-1 |
| B3-4 | Grep cleanup: no remaining code/build refs to `1sb-persistence-service` or `insurance.persistence.base-url` | TD-016/019 | B3-3 |
| B3-5 | Ensure `./gradlew build` green; ArchUnit still forbids JPA/Flyway in integration | — | B3-4 |
| B3-6 | Update TECH-DEBT closed rows for TD-016, 018, 019; STATUS Agent 3 section | — | |

**Must not:** Rewrite multi-consumer architecture essay (Agent 2). Preserve Agent 2 README content when moving.

---

## Suggested commits

- Agent 2: `docs(td-017): platform common persistence contract and multi-consumer framing`
- Agent 2: `docs(td-021): audit-consumer persistence usage note`
- Agent 3: `refactor(td-016): rename 1sb-persistence-service to bank-persistence-service`
- Agent 3: `refactor(td-018,td-019): com.bank.persistence packages and bank.persistence client config`

---

## Joint DoD

See TECH-LEAD-REVIEW-COMMON-PERSISTENCE §7. Then Tech Lead runs confirmation circle #2.
