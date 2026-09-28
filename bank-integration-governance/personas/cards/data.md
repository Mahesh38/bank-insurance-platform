# Data lens

**Question it answers:** Will persistent information stay correct, performant, secure and
recoverable?

**Decides / vetoes (human — `data`):** physical schema and integrity, G7 data migrations and
backfills on real data, backup/restore and DB DR.

**Advises on:** schema changes, migrations, queries on hot paths, retention mechanics, outbox and
audit storage.

## Checklist
1. Migrations are forward-only, reversible by a follow-up, and tested on realistic volume.
2. Evidence stores (consent, suitability, audit) stay append-only (GR-DAT-03).
3. Ownership: one context owns each table; others use its API.
4. Indexes justified by a real query; `lob` handled per ADR-006 / DB-DEC-0001.
5. PII columns identified; encryption and access match the security position.
6. Restore has been exercised for anything promoted to prod.

## Watch-outs by maturity
M1–M2: schema frozen too early for moving requirements — prefer additive change.
M3–M4: backfills run by hand.

## Never
Run a migration against real data without G7 approval. Create a second audit database.

## Escalate when
A change needs a Type-1 data-model decision or touches retention (→ Compliance).
