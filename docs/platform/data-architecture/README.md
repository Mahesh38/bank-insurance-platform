# R0 Physical Data Architecture — Aarti pack

**Workstream:** WS-3 — AU Bank Insurance Distribution Platform
**Work item:** [`DATA-001`](./DATA-001.work-item.yaml) · origin [`SUG-20260825-db1`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20260825-db1--aarti-r0-physical-data-architecture-pack)
**Owner:** Aarti — Principal Insurance Data & Database Architect
**Co-owners (joint review):** Mahesh (ownership / boundaries) · Deepali (security outcome) · Shailja (retention / PII class)
**Status:** `AI-DRAFTED` — Aarti's human signature for `S07-G5` is outstanding. This pack assembles the evidence; it does not manufacture the approval.
**Stage:** Design is `S07-E04` (S07 exit). Applying DDL, proving restore, and running purge are `S09` and are parked.

This is the physical counterpart of the logical model in
[`ws3-platform/02-information-model.md`](../ws3-platform/02-information-model.md).
That document is explicit: it is **not** a physical schema
([`§1`](../ws3-platform/02-information-model.md#1-scope-and-honest-limits)).
`OPEN-I1` and `OPEN-I6` named this pack.

---

## What this pack is

| Document | Closes |
|---|---|
| [`00-design-rules.md`](./00-design-rules.md) | Binding persistence rules Aarti will not waive |
| [`01-physical-design.md`](./01-physical-design.md) | Topology, datastore per context, tables, relationships, indexes, `OPEN-I6` |
| [`02-operations-and-troubleshooting.md`](./02-operations-and-troubleshooting.md) | Migration, encryption/access implementation, backup/PITR/RPO/RTO, retention/purge, troubleshooting |
| [`DB-DEC-0001-r0-physical-model.md`](./DB-DEC-0001-r0-physical-model.md) | Formal `database_decision` for `ADR-008` / `S07-G5` (draft, unsigned) |
| [`schemas/`](./schemas/) | PostgreSQL design DDL — one script per schema, plus routines and grants |

## What this pack is not

- A human signature on `S07-G5` or `ADR-008`.
- A Flyway migration applied to a running service. Existing migrations stay where they are:
  [`V1__init_schema.sql`](../../../services/bank-persistence-service/src/main/resources/db/migration/V1__init_schema.sql)
  and
  [`V1__identity_authorization_schema.sql`](../../../services/identity-authorization-service/src/main/resources/db/migration/V1__identity_authorization_schema.sql).
- An extension of `bank-persistence-service` to Customer / Opportunity / Consent / Suitability /
  Catalogue / Quotation / Proposal / Payment / Policy / Journey
  ([`ARCH-004`](../architecture-review/08-architecture-decision-log.md),
  [`ADR-008`](../architecture-review/08-architecture-decision-log.md#adr-008--data-ownership-is-the-invariant-physical-cluster-topology-is-an-evidence-led-decision)).
- A second audit database. Audit ingestion remains on the existing store.
- A stored-procedure application layer. Transactional writes stay in the owning service.

## Schemas in the R0 cluster

One Aurora PostgreSQL cluster. One schema per bounded context. No cross-schema grants.

| Schema | SoR context | Script | Code today |
|---|---|---|---|
| `identity` | Identity & Access (WS-2) | [`01-identity.sql`](./schemas/01-identity.sql) | Flyway on `identity-authorization-service` |
| `bank_persistence` | 1SB Adapter job store + audit ingest | [`02-bank_persistence.sql`](./schemas/02-bank_persistence.sql) | Flyway on `bank-persistence-service` |
| `customer` | Customer snapshot | [`03-customer.sql`](./schemas/03-customer.sql) | Design only |
| `opportunity` | Lead / opportunity | [`04-opportunity.sql`](./schemas/04-opportunity.sql) | Design only |
| `consent` | Consent evidence | [`05-consent.sql`](./schemas/05-consent.sql) | Design only |
| `suitability` | Suitability assessment | [`06-suitability.sql`](./schemas/06-suitability.sql) | Design only |
| `catalogue` | Product catalogue | [`07-catalogue.sql`](./schemas/07-catalogue.sql) | Design only |
| `quotation` | Quote and offer | [`08-quotation.sql`](./schemas/08-quotation.sql) | Design only |
| `proposal` | Proposal & UW | [`09-proposal.sql`](./schemas/09-proposal.sql) | Design only |
| `payment` | Payment | [`10-payment.sql`](./schemas/10-payment.sql) | Design only |
| `policy` | Policy & issuance | [`11-policy.sql`](./schemas/11-policy.sql) | Design only |
| `journey` | Journey orchestration | [`12-journey.sql`](./schemas/12-journey.sql) | Design only |
| `administration` | Versioned configuration | [`13-administration.sql`](./schemas/13-administration.sql) | Design only |

Shared cluster objects: [`00-cluster-bootstrap.sql`](./schemas/00-cluster-bootstrap.sql),
audit column delta [`14-audit_event_delta.sql`](./schemas/14-audit_event_delta.sql)
(`OPEN-I3` / `OPEN-I5`), routines [`90-routines.sql`](./schemas/90-routines.sql),
grants [`91-grants.sql`](./schemas/91-grants.sql).

Vendor-owned stores that are **not** designed here: Keycloak's own database, the
`ADR-011` session cache, S3 object bytes, MSK payloads.

## Verdict (draft)

| | |
|---|---|
| **Decision** | `APPROVED_WITH_OBSERVATIONS` as a draft for human Aarti / Mahesh / Deepali / Shailja |
| **Severity** | `D1` — design is sufficient for S07 exit; restore and purge remain unproven (`D2` until S09 evidence) |
| **Integrity guarantee** | Keys, uniqueness, `lob` CHECK, INSERT-only on immutable tables, no cross-schema FK |
| **Recovery guarantee** | R0 design target: RPO 5 minutes (Aurora PITR), RTO 30 minutes (Multi-AZ failover). **Not measured.** |

Cite: [`04-operating-and-review-contract.md` §4](../../context/roles/principal-insurance-data-database-architect/04-operating-and-review-contract.md#4-standard-aarti--dba-decision-output).
