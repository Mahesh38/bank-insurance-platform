# CR-015 — Narrow `bank-persistence-service` to Integration Ops / Evidence; persist per bounded context

**Date:** 2026-09-04
**Type:** CONSTRAINT (standing-constraint change; potential supersession of one Accepted register row)
**Raised by:** Stakeholder decision → recorded by agent under human override ([09 §8](../09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process))
**Workstream:** WS-3 (primary) · WS-1 contract correction
**Stage:** S08 — Engineering Foundation (WS-3) / L7 Hardening (WS-1, docs only)
**Decision:** **CANDIDATE** — target transcribed into `ADR-019` and the persistence contract. Human T4 Architecture / Database signatures outstanding. This agent does not approve the CR.
**Origin:** `SUG-20260904-bps` `ADMIT-BYPASS`
**Architecture:** [`ADR-019`](../../platform/architecture-review/08-architecture-decision-log.md#adr-019--persistence-ownership-is-per-bounded-context-bank-persistence-service-is-not-a-platform-wide-gateway)
**Related:** `ARCH-004` · `ADR-008` · `DB-DEC-0001` · [`R0-LLD` §5.1](../../architecture/R0-LLD.md) · PR #82 `CR-015` (same target, GitLab-freeze sequencing **not** copied)

> **ID note.** Sequential `CR-014` on `main` is left unused because it is already in flight on
> PR #86 (Life LOB adapter) and, under a different meaning, on PR #82 (GitLab estate migration).
> This CR reuses **015** so it is the same persistence decision as PR #82's `CR-015`, landed on
> `main` instead of remaining trapped in the GitLab freeze.

---

## 1. Current position

Phase 1 correctly **split Flyway and JPA out of** `1sb-integration-service` into
`bank-persistence-service` (TD-011 / TD-016 / TD-017). That split stands.

Phase 1 then **over-claimed**. The persistence contract, `AGENTS.md`, Aarti's card and the
Accepted decision-register row describe the service as the **centralised DB access point for
all future microservices** ("other MS later", "owns the DB for all consumers").

That extrapolation is already contradicted by artefacts that outrank it:

| Artefact | What it already says |
|---|---|
| [`ARCH-004`](../../platform/architecture-review/08-architecture-decision-log.md) | Shared-HTTP-store scoped **only** to the integration job/correlation store and audit ingestion — not Customer / Lead / Consent / Suitability / Catalogue / Payment / Policy |
| [`ADR-008`](../../platform/architecture-review/08-architecture-decision-log.md) | Ownership is the invariant (one owner, own schema, own credentials, no cross-table access). R0 is **one** Aurora cluster, schema-per-context |
| [`R0-LLD` §5.1](../../architecture/R0-LLD.md) | Keep `bank-persistence-service`; point it at schemas `onesb` + `audit` ingestion. **Do not** route Opportunity / Consent / Payment through it |
| [`DB-DEC-0001`](../../platform/data-architecture/DB-DEC-0001-r0-physical-model.md) | Constrains "extending `bank-persistence-service` to business contexts" |
| [`05-data-architecture.md`](../../platform/architecture-review/05-data-architecture.md) | The spike pattern is sound for **two** related concerns; it "stops being sound as a platform-wide pattern" |
| `identity-authorization-service` | Already owns its own Flyway (`V1__identity_authorization_schema.sql`). That is the correct second-context pattern |
| Doctrine `SC-03` / `SC-21` | One consumer ≠ platform shared service; sharing a capability never means sharing a database |

The live defect is **documentary**: three stale artefacts still describe the Phase 1 over-claim as
if it were the target. Agents following `AGENTS.md` and the decision register will keep adding
schemas to `/internal/v1`. That is an incorrect domain-ownership model (hard `P1` class) and a
standing-constraint lie. Stakeholder instruction 2026-09-04: **do not park; act now**.

A 2026-08-29 board outcome on PR #82 already selected **Option B** (per-context ownership) for
the same conflict against the bank baseline §3.3. This CR lands that **target on `main`**. It
does **not** copy PR #82's GitLab-migration sequencing ("during GitLab `CR-014` migrate
unchanged") and it does **not** manufacture a T4 signature on this branch.

## 2. Proposed change

| # | Change |
|---|---|
| 1 | **Keep** `bank-persistence-service` as the Integration Ops / Evidence bounded context: jobs, offers, poll attempts, raw_payload, `audit_event` ingest, 1SB adapter `payment_session` (link/URL — **not** CAP-301 Payment). |
| 2 | **Stop** treating it as a platform DB gateway. Customer, Lead, Consent, Suitability, Catalogue, Quotation, Proposal, business Payment, Policy, Journey and Identity **never** persist through `/internal/v1`. |
| 3 | **Each new context** owns its schema, credentials, Flyway history and repository layer **in that service**, on the **same** R0 Aurora cluster (`ADR-008`). Identity already does this. |
| 4 | **`1sb-integration-service` still owns no Flyway/JPA** and still talks HTTP to persistence. Bank apps still never call a database. No second audit database. |
| 5 | **Reject** a Flyway-only mega-migrator that holds every context's scripts. **Reject** JDBC from consumers into persistence-owned tables. **Reject** deleting the service or moving its Flyway back into the 1SB adapter. |
| 6 | Rewrite the Accepted register row, standing constraint, `AGENTS.md`, Aarti/Amit cards and the Phase 1 persistence contract so they can no longer be cited as a licence to route all MS through this service. |

## 3. What this CR does not do

- Edit `current_phase` or `stage_status`.
- Delete `bank-persistence-service` or change its V1 DDL.
- Move Flyway into `1sb-integration-service`.
- Authorise an S09 data-migration / table-split programme (Aarti Q4 / restore test still owed
  **if** any current table is later re-homed; today's V1 tables already belong to this context).
- Route identity through `/internal/v1`.
- Add `bank-common-domain` as a persistence dependency.
- Approve GATE-S08 or manufacture T4 Architecture / Database signatures.

## 4. Driver

Stakeholder: review with the boards whether centralising all DB access in
`bank-persistence-service` was the right decision; if not, name the better approach and how to
implement it; and whether Flyway can live in a persistence service that "doesn't depend on
anything". Must act immediately; parking withdrawn.

## 5. How to implement (H0 — this CR)

No new runtime component. The rightful implementation **is the contract correction**.

| Horizon | Action | Owner |
|---|---|---|
| **H0 (this PR)** | ADR-019 + standing-constraint rewrite + persistence contract + cards + V1 comment. Stop adding business tables to this service. | Mahesh (draft) / Amit (docs in engineering SSOT) |
| **H0 (already true)** | `identity-authorization-service` keeps its own Flyway and datasource. Do not reverse it. | Amit |
| **Next context (S09+)** | Opportunity / Consent / Payment-context / Policy ship **in-service** Flyway against their own schema on `r0-platform-{env}`. Per-schema migrator role vs app DML role as `R0-LLD` §5.1 already names. | Aarti (schema) + Amit (module) + Mahesh (boundary) |
| **Not now** | A standalone migrator Job that owns **all** scripts. A per-service migrator Job **for that schema only** is optional later and is not a new shared database. | — |

### Flyway-only service — rejected

A deployable that holds everyone's Flyway still **owns** everyone's schemas. If apps then JDBC to
those tables, that is cross-service DB access (`ADR-008` forbids it; Aarti+Mahesh joint). If they
still go HTTP, the split buys no S08 benefit (anti-over-engineering X1/X6/X7). Optional later: a
**per-service** migrator Job vs API for *that* schema only.

`payment_session` in V1 is the **adapter's** session record, not the Payment bounded context.
Do not conflate with LLD "do not route Payment through it."

## 6. Board review — AI drafts only (T4 human outstanding)

FreshnessCheck at intake: **WARN** (`state_as_of` 25 days old; `04-STAGE_GATES.md` stale). Agents
may proceed with disclosure. PO counter-signature on state still outstanding.

### Board 1 — Architecture (Mahesh)

```yaml
review:
  board: ARCHITECTURE
  reviewer: "Mahesh / Principal Insurance Platform Architect"
  reviewer_type: AI_DRAFT
  decision: APPROVED_WITH_CONDITIONS
  severity: A2   # A1 while Phase 1 contract still says "all MS"; A2 once ADR+docs match LLD
  checks:
    A1: "Keep the service; do not make it the write path for every context"
    A2: "Integration Ops / Evidence is the correct remaining responsibility"
    A3: "HTTP from 1sb-integration → persistence is justified and directional"
    A4: "Standing constraint rewritten rather than silently violated"
    A5: "ADR-019 records the decision; ARCH-004 promoted"
    A6: "No new infrastructure"
    A7: "Stopping now avoids a later split of Opportunity/Consent/Payment out of one HTTP store"
    A8: "S08 records the contract; does not import an S09 table-migration programme"
    A9: "Smallest change: docs + ADR. Identity already demonstrates the pattern"
    A10: "Replacing the Integration Ops store later is one context, not the platform"
  conditions:
    - "Human T4 Architecture sign-off before the standing-constraint rewrite is treated as Accepted"
    - "Do not add Lead/Consent/Payment-context/Policy schemas to bank-persistence-service"
  evidence:
    - "docs/architecture/R0-LLD.md §5.1"
    - "docs/platform/architecture-review/08-architecture-decision-log.md ARCH-004 and ADR-008"
    - "docs/platform/architecture-review/05-data-architecture.md (existing amendment)"
    - "docs/context/roles/mahesh-principal-insurance-platform-architect/14-shared-capability-doctrine.md SC-03 SC-21"
  notes: "The Phase 1 HTTP gateway for every future MS was the wrong extrapolation. The R0 architecture already corrected it. Record ADR-019 and stop implementing the Phase 1 lie."
  date: 2026-09-04
```

### Database — Aarti (mandatory joint with Mahesh)

```yaml
database_decision:
  id: DB-DEC-0003
  subject: "bank-persistence-service is Integration Ops / Evidence, not a platform DB gateway"
  stage: "S08"
  accountable_database_authority: "Aarti — Principal Insurance Data & Database Architect / DBA"
  architecture_owner: "Mahesh"
  engineering_owner: "Amit"
  decision: >
    Keep the existing V1 schema under bank-persistence-service. Do not extend it to business
    contexts. New contexts own Flyway in-process against their own schema on the R0 Aurora
    cluster. Reject a Flyway-only bag of all scripts. Reject cross-service JDBC into
    persistence-owned tables.
  status: APPROVED_WITH_OBSERVATIONS
  database_severity: D1
  rationale: >
    DB-DEC-0001 already constrains extending this service to business contexts. Identity already
    migrated independently. Current V1 tables (jobs, offers, poll, raw_payload, audit_event,
    payment_session-as-adapter-record) are the correct remaining set. A mega-migrator would own
    every schema, which is the ownership failure ADR-008 exists to prevent.
  observations:
    - "Restore/purge evidence for the cluster remains D2 until S09 (known, DB-DEC-0001) — not re-opened here"
    - "If a current V1 table is later re-homed, Q4 integrity/recovery review is Aarti's and is not this CR"
  human_approval_required: true
  revisit_trigger: "Proposal to add a business-context table to bank-persistence-service, or to introduce a second cluster"
  evidence:
    - "docs/platform/data-architecture/DB-DEC-0001-r0-physical-model.md"
    - "services/identity-authorization-service/src/main/resources/db/migration/V1__identity_authorization_schema.sql"
    - "services/bank-persistence-service/src/main/resources/db/migration/V1__init_schema.sql"
```

`DB-DEC-0003` is an index alias for this CR's database verdict. It does not replace `DB-DEC-0001`.

### Board 2 — Technical (Amit)

`APPROVED_WITH_CONDITIONS`. Feasible on the current stack. Identity already has its own Flyway.
Conditions: do not add `implementation(project(":libs:bank-common-domain"))` to persistence;
do not move Flyway back into `1sb-integration-service`; ArchUnit rule "no Flyway/JPA in
integration" stands.

### Board 3 — Product (Rajal)

`NOT_APPLICABLE`. No journey, channel, LOB or acceptance change. Persistence topology is not
Product behaviour.

### Board 4 — Security (Deepali)

`APPROVED_WITH_CONDITIONS` (AI draft). Isolation remains per-schema credentials and no
cross-schema grants (`ADR-008`). Do not treat one HTTP store as a compensating control.
Human T4 if the standing-constraint change is read as a trust-boundary change — notify, do not
waive.

### Board 5 — QA (Swapnali)

`NOT_APPLICABLE` for this documentation CR. Evidence is required when a **new** context's
Flyway lands (migration tests, no cross-schema grant). No V1 DDL change in this PR.

### Board 6 — Risk & Compliance (Shailja)

`NOT_APPLICABLE` to product permissibility. Constraint that **stands**: no second audit
database; audit ingest stays with Integration Ops / Evidence. Human T4 not manufactured.

### Board 7 — Operations (Shivanshi)

Notify. One-cluster blast radius is already accepted in `ADR-008`. This CR adds no runtime,
no new datastore, no pipeline. Do not pull a table-split onto the S08 critical path.

### R12 — Delivery (Kalpana)

Notify. GATE-S08 criteria unchanged. This CR removes a documentary contradiction; it does not
authorise a persistence rewrite on the critical path.

## 7. Impact

- `ADR-019` (Proposed) in the architecture decision log
- `DECISION-REGISTER` §1 — supersede the "platform-common HTTP store for any consumer DB" row
- `CURRENT-STATE.yaml` standing constraint + `id_allocation` (ADR/CR counters only)
- `01-CURRENT_STATE.md` §5
- `AGENTS.md` service table
- Aarti and Amit persona cards
- `docs/1sb-insurance-integration/architecture/bank-persistence-service.md`
- Banner on `TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md`
- Comment-only on `V1__init_schema.sql`
- `BOOT.md` regenerated

GATE-S08 criteria are unchanged. No product scope change.

## 8. Authority

| Role | Action |
|---|---|
| Stakeholder | Authorised immediate action; parking withdrawn |
| Mahesh | Structure (`ADR-019`) — T4 outstanding |
| Aarti | Integrity/recovery and schema ownership — human signature outstanding |
| Amit | Engineering contract and ArchUnit rules that remain |
| Deepali / Shivanshi / Shailja | Notify; no new exposure, cluster, or audit DB |
| Agent | Draft only. Must not mark this CR `APPROVED` |

## 9. Change request record

```yaml
change_request:
  id: CR-015
  raised_by: "agent:cursor"
  date: 2026-09-04
  type: CONSTRAINT
  driver: >
    Phase 1 persistence contract over-claims a platform-wide HTTP DB gateway. R0-LLD §5.1,
    ARCH-004, ADR-008 and DB-DEC-0001 already forbade extending it to business contexts.
    Stakeholder 2026-09-04: review with boards and act; do not park.
  evidence:
    - "R0-LLD §5.1 — keep persistence; do not route Opportunity/Consent/Payment through it"
    - "ARCH-004 — shared-HTTP-store scoped to job store + audit ingest"
    - "identity-authorization-service owns its own Flyway today"
    - "DECISION-REGISTER Accepted row still forbids embedding a DB in a consumer"
  impact:
    scope: "no product scope change"
    stage: "does not block GATE-S08; no S09 table migration authorised"
    dependencies: "none"
    effort: "S — documentation and ADR"
  alternatives_considered:
    - option: "Keep the Phase 1 gateway and route all future MS through /internal/v1"
      consequence: "Single coupling/failure point; contradicts ARCH-004, LLD, SC-21"
    - option: "Delete bank-persistence-service and put Flyway back in 1sb-integration"
      consequence: "Reopens TD-011; 1SB adapter owns durable platform evidence"
    - option: "Flyway-only service holding every context's scripts, apps JDBC independently"
      consequence: "Cross-service DB access, or a split with no benefit"
    - option: "Park until S09"
      consequence: "Stakeholder withdrew parking; agents would keep implementing the over-claim"
  decision: CANDIDATE
  adr: ADR-019
  signature_status: >
    AI-drafted. Human T4 Architecture and Database signatures outstanding. A 2026-08-29
    Option B relay on PR #82 is cited as prior target selection, not as a signature on this
    branch.
```
