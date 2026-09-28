---
lane: persistence-audit
owner: data
verified_on: 2026-09-28
---

# Persistence & audit — context pack

## Summary
`bank-persistence-service` is the **platform-common** persistence service (Flyway + JPA,
`/internal/v1`) and owns the database for all consumers, including the 1SB job store and the
append-only audit store. H2 (`MODE=PostgreSQL`) locally, PostgreSQL in UAT/prod; R0 target is one
Aurora cluster with a schema per bounded context. Maturity ≈ M2–M3. Most common agent error:
adding a datasource or migration to a consumer service.

## Outcome now
- **O-PA-1** R0 physical model aligned to CR-013 (DATA-002 delta) so journey contexts can persist.
- **O-PA-2** Audit evidence path proven append-only and exportable for the compliance review.

## Contracts
- `services/bank-persistence-service/src/main/java/com/bank/persistence/api/internal/v1/dto/`
- Schemas: `docs/platform/data-architecture/schemas/`

## Invariants in play
GR-ARC-03, GR-ARC-07, GR-DAT-02, GR-DAT-03, GR-ENG-01.

## Open decisions
- DB-DEC-0001 R0 physical model — AI-drafted, needs human data-lead decision. **Default:** build to it.
- DB-DEC-0002 / DATA-002 CR-013 schema delta — AI-drafted `CHANGES_REQUIRED`. **Default:** additive migrations only.
- DEC-OPEN-AUDIT-BACKBONE — audit consumer on MSK (ADR-012) vs direct outbox poll. **Default:** outbox is source of truth; consumer behind a port.

## Known debt — do not re-report
TD-010 (idempotency store location) is shared with integration-hub.

## Where truth lives
1. `docs/platform/data-architecture/README.md` and `PLAN-002-r0-physical-data-architecture.md`
2. Decision register: ADR-006 (`lob` dimension), ADR-008 (data ownership), DB-DEC-0001/0002
3. `services/bank-persistence-service/src/main/resources/db/migration/`

## Gotchas
- `lob` is an index prefix, not a partition key (DB-DEC-0001).
- No regulatory evidence may live only in a topic, cache or search index (ADR-011/012/013).
- Migrations touching real data are G7 → R2, even when "just a backfill".
