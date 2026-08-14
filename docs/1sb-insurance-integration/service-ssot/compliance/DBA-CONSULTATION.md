# DBA consultation — `audit_event` immutability

**Question referred:** compliance **Q2** — "Are database-enforced insert-only permissions
required before production?"
**Referred by:** Mahesh (Solution Architect), 2026-08-14 · **To:** Database Administrator
**Ratified by:** Risk & Compliance, once the DBA has answered

> **One decision is needed, and it is yours.** Everything below exists so it can be made in one
> sitting. Engineering has deliberately not implemented a grant model — picking a database
> permission scheme without the DBA is how you end up with one that does not fit the bank's
> account provisioning.

---

## 1. What exists today

`audit_event` is described in the migration as an immutable, append-only compliance log:

```sql
-- audit_event
-- Immutable compliance audit log — service account has INSERT only
CREATE TABLE audit_event (
    event_id            VARCHAR(36)              PRIMARY KEY,
    event_time          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    action              VARCHAR(100)             NOT NULL,
    actor_id            VARCHAR(100)             NOT NULL,
    ...
);
```

**That comment is the entire control.** There is no `GRANT`, no `REVOKE`, and no separate role
anywhere in `V1__init_schema.sql`. The application connects with a single account — in
`docker-compose.yml`, the Postgres owner `bank_persistence` — which can `UPDATE` and `DELETE`
these rows freely.

The application never issues an update or delete: `AuditEventController` exposes `POST` and
`GET` only, and `AuditEventRepository` is used for `save` and reads. So the *behaviour* is
append-only. The *permission* is not.

**As of 2026-08-14 the table is actually being written to** — audit persistence landed with
RISK-012. Until then the question was theoretical. It is not any more.

---

## 2. The decision

> **Does `audit_event` need database-enforced insert-only permissions, and if so, from which
> environment onwards — UAT, or production only?**

Sub-questions that change the shape of the answer:

| # | Question |
|---|---|
| 2a | Should the application use a **separate database role** for audit writes, distinct from the role that owns the schema? |
| 2b | Should `UPDATE` and `DELETE` be revoked, or is a **trigger** that raises an exception preferable? (A trigger survives a role being re-granted by mistake; a revoke is simpler to reason about.) |
| 2c | Who holds the account that **can** delete — needed for the retention job that will eventually purge expired rows — and how is its use logged? |
| 2d | Does Flyway need its own migration role, given that the app role would no longer own the schema? |
| 2e | Same question for `raw_payload`: it holds encrypted customer payloads and is arguably more sensitive than the audit log. Should it get the same treatment? |

---

## 3. Options, with the trade-offs Engineering can see

| Option | How | Cost | Weakness |
|---|---|---|---|
| **A — do nothing** | Keep the convention | None | The control is a comment. An incident, a bad migration, or an ORM mistake can rewrite history, and nothing detects it |
| **B — revoke UPDATE/DELETE from the app role** | Separate `bank_persistence_app` role; owner role runs migrations only | Small: one migration, plus environment provisioning | A future grant silently restores the hole. Needs the retention job to run as a different account |
| **C — trigger that rejects UPDATE/DELETE** | `BEFORE UPDATE OR DELETE … RAISE EXCEPTION` | Small; independent of role management | A superuser can drop the trigger; needs an exemption path for the retention purge |
| **D — B + C together** | Both | Small | The retention job needs both a role and a trigger exemption — most moving parts |
| **E — append-only storage outside Postgres** | WORM object store or a ledger database | Large; new infrastructure | Almost certainly disproportionate at this stage; noted for completeness |

**Engineering's non-binding view:** **B**, with **C** if the DBA considers role drift a realistic
risk in this estate. Both are cheap now and awkward after the table has years of data. E is out
of proportion to Phase 4.

We hold this loosely — the DBA sees the account provisioning model and we do not.

---

## 4. Constraints Engineering can state

- **Flyway owns the schema** and runs from `bank-persistence-service` on startup. Any role split
  must keep migrations working in local, UAT and production.
- **H2 is used in tests** in PostgreSQL-compatibility mode. A Postgres-only construct must be
  guarded so the test suite still runs — a migration that H2 cannot parse breaks CI.
- **A retention job will exist** (Phase 6). It must delete expired `raw_payload` rows, and
  possibly expired audit rows depending on the retention answer. Whatever is chosen must leave a
  sanctioned deletion path.
- **No production deployment exists yet**, so there is no migration-of-live-data problem. This is
  the cheapest possible moment to change it.

---

## 5. Answer block

```text
DBA:                        __________________________
date:                       __________________________

2  — DB-enforced insert-only required?      YES / NO
     if YES, from which environment?        UAT / PRODUCTION / OTHER: __________
2a — separate role for audit writes?        YES / NO
2b — mechanism                              REVOKE / TRIGGER / BOTH / OTHER: __________
2c — account permitted to delete, and how its use is logged:
     ____________________________________________________________
2d — separate Flyway migration role?        YES / NO
2e — same treatment for raw_payload?        YES / NO

chosen option (section 3):  A / B / C / D / E / other: __________

notes / constraints Engineering has not accounted for:
     ____________________________________________________________
```

Once answered, this is attached to the [4.4 review pack](./COMPLIANCE-REVIEW-PACK.md) as the
evidence for Q2, and Compliance ratifies it. The implementation becomes a work item with the
DBA's chosen option as its acceptance criterion.

---

## 6. Related

| Document | Why |
|---|---|
| [COMPLIANCE-REVIEW-PACK.md](./COMPLIANCE-REVIEW-PACK.md) | Finding 2 — where this question came from |
| [REGULATORY-RETENTION-FINDINGS.md](./REGULATORY-RETENTION-FINDINGS.md) | Retention, which determines what the deletion path must do |
| `RISK-013` in the [risk register](../../../governance/registers/RISK-REGISTER.md) | The open risk this closes |
| `V1__init_schema.sql` | The migration to change |
