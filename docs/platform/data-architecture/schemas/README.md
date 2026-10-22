# R0 design DDL

PostgreSQL scripts that implement [`01-physical-design.md`](../01-physical-design.md).
They are **design artefacts**. Existing runtime migrations stay in the owning service
modules. Apply these at S09 by copying into a versioned Flyway file — do not execute
this folder as a superuser against production (`DR-MIG-04`).

**2026-10-21 update.** The files below implement the owner decisions in
[`DECISIONS-2026-10-21.md`](../../architecture-review/DECISIONS-2026-10-21.md) (D2, D3, D4, D5, D8),
[`ADR-024`](../../architecture-review/ADR-024-configuration-driven-rule-engine.md) (rule sets),
[`ADR-025`](../../architecture-review/ADR-025-event-specification-and-publisher.md) (outbox shape:
`outbox_id` ULID, `aggregate_*`, `schema_version`, `parked_at`) and — **as a proposal only** —
[`ADR-026`](../../architecture-review/ADR-026-audit-store-and-transport.md). Each change is commented
with its decision id in the SQL.

| File | Schema / purpose |
|---|---|
| [`00-cluster-bootstrap.sql`](./00-cluster-bootstrap.sql) | Schemas on the one Aurora cluster |
| [`01-identity.sql`](./01-identity.sql) | WS-2 identity (wraps existing Flyway) |
| [`02-bank_persistence.sql`](./02-bank_persistence.sql) | Job store + audit ingest (wraps existing Flyway) |
| [`03-customer.sql`](./03-customer.sql) | Customer (`customer_id` ULID, CIF encrypted + HMAC — `D-023`), snapshot with freshness, outbox |
| [`04-lead_lms.sql`](./04-lead_lms.sql) | Lead LMS (`lead`), exception evaluation evidence (D2) — reconciled with Flyway V2/V3, see [`../03-lead-suitability-e2e.md` §4.6](../03-lead-suitability-e2e.md#46-design-ddl-vs-as-built-flyway-v2v3--reconciliation-2026-10-21) |
| [`05-consent.sql`](./05-consent.sql) | Append-only `consent_record` (D5 / `D-024`, `CNS-QSH`), outbox |
| [`06-suitability.sql`](./06-suitability.sql) | Suitability header + canonical `evaluation` / `evaluation_outcome` per class (D4), caution ack, usage — e2e sheet [`../03-lead-suitability-e2e.md`](../03-lead-suitability-e2e.md) |
| [`07-catalogue.sql`](./07-catalogue.sql) | Product catalogue, outbox |
| [`08-quotation.sql`](./08-quotation.sql) | Structured quote and every offer after `RECONCILED` (D8; `INVALID` offers kept) |
| [`09-proposal.sql`](./09-proposal.sql) | Proposal, `proposal_answer`, `proposal_status_history` (D8), UW |
| [`10-payment.sql`](./10-payment.sql) | Payment (`quote_id`, `proposal_id`, method, `paid_at` — D8), ADR-025 outbox |
| [`11-policy.sql`](./11-policy.sql) | Policy dates, insurer-decision TAT, `policy_status_history` (D8), outbox |
| [`12-journey.sql`](./12-journey.sql) | Journey orchestration, ADR-025 outbox, `processed_event` |
| [`13-administration.sql`](./13-administration.sql) | Versioned configuration, `rule_set_version` (ADR-024), outbox |
| [`14-audit_event_delta.sql`](./14-audit_event_delta.sql) | OPEN-I3 / OPEN-I5 columns on `bank_persistence.audit_event` (current until ADR-026 is accepted) |
| [`15-audit.sql`](./15-audit.sql) | **PROPOSED** (ADR-026): schema `audit`, hash-chained INSERT-only `audit_event`, WORM export heads |
| [`20-dynamodb-working-payload.md`](./20-dynamodb-working-payload.md) | DynamoDB tables for pre-payment quote and proposal state (`DB-DEC-0005`) — not PostgreSQL |
| [`90-routines.sql`](./90-routines.sql) | Triggers, sequence, visibility, S09 purge |
| [`91-grants.sql`](./91-grants.sql) | Least-privilege grants |

## Validating on PostgreSQL

[`scripts/db/validate-postgres.sh`](../../../../scripts/db/validate-postgres.sh) checks this pack and
every service's Flyway migrations against a real PostgreSQL 16. It creates its own throw-away
databases and drops them on exit (`--keep` leaves them for inspection).

```bash
scripts/db/validate-postgres.sh                      # pack + services, host /tmp, port 55432, user postgres
scripts/db/validate-postgres.sh --host db --port 5432 --user postgres pack
scripts/db/validate-postgres.sh services             # Flyway V*.sql of db/migration + db/vendor/postgresql
```

- **Pack order:** `00`…`15`, `90-routines.sql`, then
  [`tests/00-test-roles.sql`](./tests/00-test-roles.sql) (test-only NOLOGIN roles that IaC creates
  in real environments), then `91-grants.sql`.
- **Invariants:** [`tests/10-invariants.sql`](./tests/10-invariants.sql) attaches the triggers
  `90-routines.sql` leaves commented for the owning Flyway, then asserts the designed behaviour by
  SQLSTATE: INSERT-only tables refuse UPDATE/DELETE (trigger and grant), partial unique indexes,
  CHECK constraints, the consent supersede chain, the audit hash chain and uniques, `rule_set_version`
  publish immutability and maker-checker, and that `job_retention` can run purges but not delete
  evidence.
- **Services:** the script reads `spring.flyway.locations` and adds `db/vendor/postgresql` only when
  the service configures `db/vendor/{vendor}`. It merges both directories in Flyway version order
  and applies each file in a single transaction.
- **Exit codes:** `0` means all green, `1` means a pack, invariant or service failed, and `2` means
  bad usage or the server could not be reached.
