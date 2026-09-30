# Audit structure (CR-010 DB-C1 / DB-C2)

**Owner:** Aarti / Database + Deepali / Security  
**signature_status:** `AI-DRAFTED — schema landed; role model documented; no regulated journey writes yet`

## Columns (before any regulated journey writes)

Flyway: `services/bank-persistence-service/src/main/resources/db/migration/V3__audit_event_reconstruction_columns.sql`  
Design DDL: `docs/platform/data-architecture/schemas/14-audit_event_delta.sql`

| Column | Purpose |
|---|---|
| `prior_state` / `new_state` | Reconstruction (TEXT in H2/local) |
| `consent_ref` / `suitability_ref` | Hard-gate evidence pointers |
| `event_schema_version` | Default 1 |
| `retain_until` | Retention horizon (DB-C1) |
| `sequence_no` | Per-`journey_id` ordering (unique index in PostgreSQL design DDL; non-unique index in V3 for H2) |
| `acting_capacity` / `actor_insurer_id` / `assisted_actor_id` / `config_version_ref` | Attribution |

JPA entity mapping of the new columns is deferred until writers populate them, so existing INSERT tests keep using DB defaults.

## Role model (DB-C2)

From `docs/platform/data-architecture/schemas/91-grants.sql`:

- Writer `app_bank_persistence`: `GRANT SELECT, INSERT` on `audit_event` and `raw_payload`; `REVOKE UPDATE, DELETE`.
- Separate purge role (not the service account) for lawful disposal.
- Deletion-refusal test remains FF-10 / S09-G8 — not claimed MET here.

Immutability is this grant set, not a comment.
