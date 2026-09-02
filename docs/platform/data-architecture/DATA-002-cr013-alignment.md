# DATA-002 — Alignment of the R0 physical pack with CR-013

**Persona:** Aarti — Principal Insurance Data & Database Architect
**Review id:** `DB-REV-0002` (informal request: “is the DB aligned with recent scope changes?”)
**Verdict record:** [`DB-DEC-0002`](./DB-DEC-0002-cr013-alignment-review.md)
**Work item:** [`DATA-002.work-item.yaml`](./DATA-002.work-item.yaml) · origin [`SUG-20260825-aln`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20260825-aln--cr-013-physical-alignment)
**Pack reviewed:** [`DATA-001`](./DATA-001.work-item.yaml) / [`DB-DEC-0001`](./DB-DEC-0001-r0-physical-model.md)
**Freshness:** `CURRENT-STATE.yaml` `state_as_of` is 15 days old (FreshnessCheck WARN). Review due 2026-09-09. Proceed with disclosure.

This is a **check**. It does not apply Flyway, does not invent a second cluster, and does not
implement the DDL listed under “must create”. Schema work is scheduled as `DATA-002` /
`PLAN-003` and is not done in the turn the gap was raised
([`09-AI_EXECUTION_RULES.md`](../../governance/09-AI_EXECUTION_RULES.md)).

---

## 1. What was compared

| Side | Source | Note |
|---|---|---|
| Physical pack | This branch (`DATA-001`, based on `main` at `95c67f8`) | Rules, `01-physical-design.md`, design DDL under `schemas/` |
| Recent scope | `origin/main` after PR #73 / `docs/governance/change-requests/CR-013-r0-lead-mis-admin-scope.md` | CANDIDATE; human T4 outstanding; transcribed into `current_scope`. Not on this branch until rebase. |
| Decisions | `origin/main` `docs/governance/DEC-20260825-01-lead-domain-decisions.md` D1–D7 | Aarti AI card: Agree, `D2` — no purge until written job + restore test |
| Architecture | `ADR-014` (PROPOSED) on `origin/main` | Lead language, archive, off-platform ingest, admin/MIS, `issuanceMode` |
| Prior robustness | `ADR-012` (outbox + MSK) | Already on the pack’s base (`CR-012`); not a CR-013 item |

`CR-013` is not in this branch’s `DOC-MAP` yet. Cite it from `origin/main` until this branch
rebases. Do not merge blindly: CR-013 rewrote `ws3-platform/02` OPEN-I1/I3/I5/I6; this branch
overlaid DATA-001 pointers on the pre-CR-013 text.

---

## 2. Verdict

**The DATA-001 pack is aligned to pre-CR-013 R0. It is not aligned to CR-013 / ADR-014 / DEC D1–D6.**

| | |
|---|---|
| **Decision** | `CHANGES_REQUIRED` |
| **Severity** | `D1` for missing admitted columns and constraints; `D2` until the W1 archive mechanism is jointly designed and a restore test exists |
| **Integrity guarantee the platform may rely on today** | Keys, LOB CHECK, INSERT-only on existing immutable tables, no cross-schema FK — **for the pre-CR-013 model only** |
| **What must not be claimed** | That Lead archive, off-platform policy ingest, `issuanceMode`, `policy.stateHistory[]`, or an isolated MIS path exist in DDL |

Human `S07-G5` / `ADR-008` signatures stay outstanding. This review does not manufacture them.

---

## 3. Still aligned — keep

Cited against `CR-013` §2–§3, `DEC-20260825-01` §13, `ADR-008`, `ADR-005`, `ADR-011`, `ARCH-004` (CR-013 and DEC live on `origin/main` until this branch rebases).

| Keep | Why it still holds |
|---|---|
| One Aurora cluster, schema per context (`ADR-008`) | D5: isolation is workload and schema, not a second cluster in S08 |
| `bank_persistence` = job store + audit ingest only | ARCH-004 / ADR-008; no second audit DB |
| RM-only Lead create; `created_by_actor_type = 'BANK_RM'`; `source = 'RM'` | `ADR-005` stands; D3 forbids `lead.create` for off-platform |
| `lob` NOT NULL CHECK `LIFE\|HEALTH\|GENERAL` as **index prefix**, not partition key | OPEN-I6; CR-013 does not reopen it |
| Idempotency table in the **owning** schema | `ADR-011` refusal of cache-backed idempotency |
| Sessions in the `ADR-011` cache, not Aurora | Unchanged |
| Administration **versioned config layer** (`administration.configuration_record`, INSERT-only) | `ADR-007` layer stands; UI deferral is withdrawn, the layer is not |
| No CRUD stored procedures (`DR-SP-01`) | Unchanged |
| Payment has no card data; issuance gated on `RECONCILED` | C-ISS-1: STP/INSTA must not skip this |
| Partner visibility as a **predicate**, not a partner-schema grant | Unchanged |
| DIY / hybrid / Group B / campaign bulk Lead create still out | `CR-013` §2 last sentence; WS-3 `out_of_scope` |

Do **not** invent from the old R0-SCOPE one-pager: PTL/RAG fraud schema, IFT/cheque tables, Group B
redirect DB. Those remain out of WS-3 `current_scope` / parked.

---

## 4. Must create or change — DATA-002

Kalpana sequence from `DEC-20260825-01` §11: S08 floor → W0b config layer → **W1 Lead schema (D2)**
→ W2 PPHI map → **W3 issuance + off-platform ingest** → **W4 admin/MIS on isolated path**.

### 4.1 W1 — Lead archive (D2, C-RET-1, INV-LED-08)

**Today** (`schemas/04-opportunity.sql`): `state` CHECK is
`NEW|ASSIGNED|CONTACTED|QUALIFIED|CONVERTED|DISQUALIFIED|EXPIRED`. No `ARCHIVED`. No `archived_at`.

**Need:**

- `ARCHIVED` in the state CHECK (terminal working-inbox state after convert **and**
  `Payment.RECONCILED` **and** `Policy.ACTIVE`).
- `archived_at TIMESTAMPTZ`.
- Physical split of retention classes: working-lead columns may leave `RET-7Y` only after a
  terminal state; attribution columns stay `RET-7Y` — `lead_id`, `accountable_sp_id`,
  `accountable_sp_cert_ref`, `converted_journey_id`, `lob` (`CR-013` §5 C-RET-1).
- Archive ≠ delete (`ID-04`). Aarti’s standing card on DEC §11: **no purge on the R0 cluster
  until a written job + restore test**.

**Not decided here** (`DEC-20260825-01` §12): partition vs archive table vs dump. That is W1
**joint Aarti / Mahesh**. Do not pick a mechanism in this check.

**Spoken name (D1):** architecture primary text says Lead; schema may stay `opportunity` in R0
to avoid identifier/Flyway churn. Renaming the schema to `lead` is a joint Aarti/Mahesh choice,
not a must-do. Identifiers stay `lead_id`.

### 4.2 W3 — `issuanceMode` (D6, C-ISS-1, INV-PRP-06)

**Today:** neither `proposal.proposal` nor `policy.policy` has `issuance_mode`.

**Need:** `issuance_mode VARCHAR NOT NULL` CHECK `STP|NON_STP|INSTA` on **both** tables.
Policy inherits the proposal value. Lead does **not** change shape per mode. Modes must not skip
suitability, consent, customer-device payment, or RECONCILED-before-issue — those remain service
invariants, not a CHECK waiver.

R0 Term’s which-of-three value is configuration (`DEC` §12). The column is mandatory and
non-null; the seed is Rajal’s, not Aarti’s.

### 4.3 W3 — Off-platform policy ingest (D3, C-ING-1, INV-POL-05)

**Today:** `policy.lead_id CHAR(26) NOT NULL`. No `source`. No ingest/maker-checker tables.

**Need:**

- `policy.source` CHECK `ON_PLATFORM|OFF_PLATFORM`.
- `lead_id` **nullable**, with a table CHECK: `ON_PLATFORM` ⇒ `lead_id` present;
  `OFF_PLATFORM` ⇒ `lead_id` **null** (never mint a Lead).
- Ingest batch + row tables on **Policy** (not Lead), with maker-checker columns. File upload
  is a Deepali trust boundary; Aarti owns the durable ingest record and idempotency key.
- Off-platform rows must not satisfy platform conversion KPI predicates (Product rule; enforce
  with `source`, not a second Lead).

### 4.4 W3 — `policy.stateHistory[]` (INV-POL-04, C-RET-2)

**Today:** only current `policy.state`. No append-only history table.

**Need:** `policy.policy_state_history` (or equivalent) INSERT-only: `policy_id`, `from_state`,
`to_state`, `changed_at`, `actor_ref`. Seven-year SoT includes issuance history. Grant pattern
mirrors `audit_event` (SELECT + INSERT, REVOKE UPDATE/DELETE).

### 4.5 W4 — Isolated reporting / MIS path (D4/D5, C-ISO-1)

**Today:** [`01-physical-design.md` §1](./01-physical-design.md#1-topology) says
“Reporting | Not provisioned in R0 | Must not share this cluster”.
[`DB-DEC-0001`](./DB-DEC-0001-r0-physical-model.md) `analytics_implications` says reporting is
out of R0. [`00-design-rules.md`](./00-design-rules.md) `DR-IDX-01` says do not index for
reporting because it is out of R0.

**That statement is now false.** Reporting/MIS (`#18`) and Administration UI (`#19`) are in R0
(`origin/main` `CURRENT-STATE.yaml` `current_scope.in_scope`; `ADR-014`).

**Need (design, not a new S08 microservice):**

- Named isolated read path: Aurora **reader** and/or extract/reporting store. **Forbid**
  MIS, admin dashboards, and reconciliation jobs on the Lead / RM **writer**.
- Reporting indexes belong on the replica/extract, not as speculative writer indexes
  (`DR-IDX-01` rewrite: “do not index the writer in case reporting needs it”).
- Grants: `app_administration` stays on `administration` config; MIS/admin **must not** receive
  `opportunity` writer credentials. Add a replica/extract role; do not GRANT SELECT on
  RESTRICTED ciphertext columns to a dashboard role.
- No new isolation microservice in S08 (`DEC` D5 / `SUG-20260825-wl1` REJECTED).

### 4.6 Per-producer transactional outbox (`ADR-012`) — already owed, not CR-013

**Today:** only `identity.outbox_event` exists. Mutating business schemas have
`idempotency_record` but **no** `outbox_event`. `bank_persistence` has none either.

**Need:** `outbox_event` in every schema that publishes (customer, opportunity, consent,
suitability, catalogue, quotation, proposal, payment, policy, journey, administration, and
the job/audit producer if it emits). Same-transaction as the business write. MSK is the
backbone; the outbox remains the source of truth (`ADR-012`).

This gap predates CR-013 (CR-012). Include it in DATA-002 so W1/W3 writers are not designed
without a publish path.

---

## 5. Must not create

| Tempting extra | Why not |
|---|---|
| Second Aurora cluster for reporting | D5 / ADR-008; isolation is workload, not topology, until measured contention |
| New isolation microservice in S08 | `SUG-20260825-wl1` REJECTED |
| `lead.create` for off-platform sales | ADR-005 + D3 |
| CRUD stored procedures for archive or ingest | `DR-SP-01` |
| Purging working-lead rows before a written job + proven restore | Aarti DEC §11 card; C-RET-2 |
| Renaming `lead_id` / `INV-LED-*` | D1 |
| DIY, hybrid, Group B, campaign bulk, PTL/RAG, IFT/cheque schemas | Still out of WS-3 `current_scope` |
| Declaring PPHI-compliant | D7; human Board 6 only |
| Editing `CURRENT-STATE.yaml` stage fields | Agents never do this |

---

## 6. Merge / document debt (not new tables)

1. Rebase this pack onto `origin/main` before applying DATA-002 DDL.
2. Resolve `ws3-platform/02` and `03` OPEN-I1: keep DATA-001 pointers **and** CR-013 attribute
   changes (`ARCHIVED`, `issuanceMode`, `source`, nullable `lead_id`, `stateHistory`).
3. Rewrite pack sentences that still say “Reporting not provisioned in R0”.
4. Do not treat this branch’s generated `BOOT.md` `out_of_scope` (admin UI / MIS at R1) as
   current — that text is from this branch’s `CURRENT-STATE.yaml`, which predates CR-013.

---

## 7. Evidence cited

- `origin/main:docs/governance/change-requests/CR-013-r0-lead-mis-admin-scope.md` §§2, 3, 5
- `origin/main:docs/governance/DEC-20260825-01-lead-domain-decisions.md` D1–D6, §11–§12
- `ADR-014`, `ADR-012`, `ADR-008`, `ADR-005`, `ADR-007`, `ADR-011`
- [`schemas/04-opportunity.sql`](./schemas/04-opportunity.sql) — no `ARCHIVED`
- [`schemas/09-proposal.sql`](./schemas/09-proposal.sql) — no `issuance_mode`
- [`schemas/11-policy.sql`](./schemas/11-policy.sql) — `lead_id NOT NULL`; no `source`; no history
- [`schemas/01-identity.sql`](./schemas/01-identity.sql) — only existing `outbox_event`
- [`01-physical-design.md` §1](./01-physical-design.md#1-topology) — Reporting “not provisioned”
- Aarti card [`aarti-database.card.md`](../../context/personas/aarti-database.card.md) — joint Mahesh for CDC/CQRS/shared DB
- Review contract [`04-operating-and-review-contract.md` §4](../../context/roles/principal-insurance-data-database-architect/04-operating-and-review-contract.md#4-standard-aarti--dba-decision-output)
