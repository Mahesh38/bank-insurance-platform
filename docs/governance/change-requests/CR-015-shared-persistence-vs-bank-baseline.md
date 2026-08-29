# CR-015 — `bank-persistence-service` versus bank baseline §3.3: shared persistence as an accepted decision

**Date:** 2026-08-29
**Type:** CONSTRAINT (potential `REVERSAL` of an Accepted decision)
**Raised by:** `agent:claude` in the Shivanshi (SRE / `R10`) persona, as finding `IMP-2` on [`GLM-001`](../../platform/gitlab-migration/GLM-001-migration-plan.md)
**Workstream:** WS-3 · **Stage:** S08 with S09 overlapped
**Jurisdiction:** **Joint — Mahesh (Board 1, Architecture) and Aarti (Database).** Neither alone.
**Related:** [`CR-014`](./CR-014-gitlab-estate-migration.md) — parallel and non-blocking, confirmed at approval (`AC-5`)
**ADR:** [`ADR-019`](../../platform/architecture-review/08-architecture-decision-log.md)

> ## Decision: `APPROVED` — **Option B**, 2026-08-29
> **Persistence ownership is per bounded context, implemented after the migration.** Recorded as
> [`ADR-019`](../../platform/architecture-review/08-architecture-decision-log.md). The target model
> is at §8.
>
> **Provenance.** Board outcome **relayed by `human:Mahesh` (repository owner) on 2026-08-29**, and
> recorded here. No AI position was drafted before the decision, which was the point: the four
> options at §4 were put without a recommendation attached.
>
> **What is decided is the target, not the migration.** Aarti's integrity and recovery guarantees
> (Q4) are **not** satisfied by this approval. The S09 allocation migration requires her independent
> review, including the restore test against `DB-DEC-0001`'s design targets (RPO 5 min, RTO 30 min).
> Deciding a target is not approving a data migration.

---

## 1. The conflict, stated exactly

**The bank baseline says** (§3.3, *Backend service independence rules*):

> "Each bounded context owns its write model/datastore; avoid a generic shared persistence service
> for all domains."

And, listing prohibited shared-library shapes, it permits `common-error`, `common-security`,
`common-audit`, `common-observability` and `common-secrets` while forbidding `common-domain`,
`common-insurance-model`, `common-customer-entity` and `common-policy-entity`.

**This repository says** ([`DECISION-REGISTER`](../registers/DECISION-REGISTER.md) §1, status
**Accepted**):

> "Persistence is platform-common (`bank-persistence-service`), reached over HTTP" — *Constrains:
> any proposal to embed a DB in a consumer.*

And [`AGENTS.md`](../../../AGENTS.md):

> `bank-persistence-service` — "**Platform-common** persistence (Flyway + JPA + `/internal/v1`);
> **owns the DB for all consumers.**"

Two further Accepted decisions rest on it: *"Integration service owns no Flyway/JPA"* and
*"Bank apps never call 1SB or the DB directly"*. Amit's operating rule 3 enforces the first in code.

### 1.1 Why this is not a bug report

The repository did not drift into this. It **decided** it, recorded it, ratified it as *Accepted*,
and built two more Accepted decisions and an ArchUnit-enforced rule on top. Whatever the outcome
here, the starting position is a governed decision, not an oversight — and reversing it needs the
new-evidence grounds in [14 §6](../14-CHANGE_CONTROL.md#6-reversing-a-rejection), not a preference.

The candidate new evidence is precisely this: **an external approved baseline now disagrees**, and
that baseline governs the estate the platform is moving into.

---

## 2. Why this is raised now, and why it is not part of CR-014

Raised now because the migration makes it visible: the split publishes
`services/bank-persistence-service` into a bank estate on day one, in a repository whose governing
specification forbids the pattern. It stops being an internal decision the moment it is a bank asset.

**Kept out of `CR-014` on three grounds:**

1. **Auditability.** A boundary change executed inside a repository move cannot be reviewed
   afterwards — the diff is indistinguishable from the migration. `GLM-001` §6 states this as a
   principle and Mahesh made it binding as `C-ARC-3`.
2. **Jurisdiction.** `CR-014`'s approver set does not include Aarti. Her authority over persistence
   integrity and recovery guarantees cannot be satisfied by a CR she is not on.
3. **Reversibility.** Migrating as-is is fully reversible. Splitting a shared datastore during a
   freeze window is not.

`CR-014` therefore migrates `bank-persistence-service` **unchanged**, and this CR decides its future
on its own evidence, afterwards.

---

## 3. What must be established before any verdict

Neither "the bank says so" nor "we already decided" is sufficient. The joint review needs answers to
these, and an AI persona can assemble evidence for them but must not conclude:

| # | Question | Owner |
|---|---|---|
| Q1 | Is `bank-persistence-service` a **generic shared persistence service for all domains** in the sense §3.3 forbids, or a bounded-context-scoped store that today happens to serve few contexts because few exist? | Mahesh + Aarti |
| Q2 | How many bounded contexts write through it today, and how many are *designed* to at R0? | Aarti |
| Q3 | Does the physical model already separate per-context schemas behind one service, or is it one shared schema? | Aarti — `DATA-001` / `DATA-002` |
| Q4 | What integrity and recovery guarantees does the current arrangement provide that a per-context split would have to reproduce? | Aarti — **not substitutable** |
| Q5 | Is `/internal/v1` an anti-corruption boundary per context, or a shared data API? | Mahesh |
| Q6 | Does the bank baseline's rule bind at R0, or at the point a second write-owning context exists? | Mahesh + bank architecture authority |
| Q7 | What is the migration cost and the data-loss risk of a later split, versus now? | Aarti + Shivanshi |
| Q8 | Does the audit evidence path change? Regulatory evidence must not be weakened by a persistence refactor | Shailja |

Q4 is the one that decides whether this is a design conversation or a data-migration programme, and
it is Aarti's alone.

---

## 4. Options, with no recommendation attached

| # | Option | Note |
|---|---|---|
| A | **No change.** Argue to the bank that the service is context-scoped, not the generic pattern §3.3 forbids; seek an Appendix C exception | Cheapest if Q1 supports it. Fails if Q2/Q3 show one shared schema across contexts |
| B | **Split per bounded context** over time; each owns its write model | Aligns with the baseline and with the platform's own service-independence rules. Cost and risk are Q4 and Q7 |
| C | **Keep the service, separate the schemas** — one deployable, per-context schema ownership behind it | Middle path. Whether it satisfies §3.3 is Q1 and Q5, and may need the bank's reading |
| D | **Defer** with a dated trigger — revisit when the second write-owning context lands | Only legitimate if Q6 says the rule binds at that point rather than at R0 |

An agent recommending one of these before Q1–Q4 are answered would be supplying the joint review's
content. That is exactly what `C-ARC-3` and Mahesh's `NA` list forbid.

---

## 4a. The approved target model — Option B

Approved 2026-08-29. Recorded in full as [`ADR-019`](../../platform/architecture-review/08-architecture-decision-log.md).

| # | Rule |
|---|---|
| 1 | **Every bounded context owns its authoritative write model, schema, credentials, Flyway migration history and repository layer.** No context persists through another context's service |
| 2 | R0 **may** use one shared Aurora PostgreSQL cluster — but **separate schemas per context and no cross-schema grants**. Physical topology stays evidence-led (`ADR-008`); ownership is the invariant |
| 3 | **`bank-persistence-service` is not a platform-wide persistence gateway.** It may survive only as a narrowly defined **Integration Operations / Evidence** context |
| 4 | **Customer, Lead, Consent, Suitability, Catalogue, Quotation, Proposal, business Payment, Policy and Journey must never persist through it** |
| 5 | **Sequencing.** During `CR-014` the service migrates **unchanged**. After migration, its current tables are allocated to their owning contexts through an **independently reviewed S09 migration** |

### 4a.1 This ratifies a design that already exists

The approval commissions no new physical design. Aarti's R0 pack already specifies it:

> [`data-architecture/README.md`](../../platform/data-architecture/README.md): *"One Aurora
> PostgreSQL cluster. One schema per bounded context. No cross-schema grants."*

That pack already lists `customer`, `opportunity` (Lead), `consent`, `suitability`, `catalogue`,
`quotation`, `proposal`, `payment` and `policy` as separate schemas, each marked **"Design only"**,
and scopes `bank_persistence` to *"1SB Adapter job store + audit ingest"* — which is precisely the
Integration Operations / Evidence context rule 3 now names. It also already forbids a second audit
database.

**So the conflict was never architecture versus the bank baseline.** It was three stale artefacts
describing the code-as-built as though it were the decided target:

| Artefact | Said | Status now |
|---|---|---|
| `DECISION-REGISTER` §1 | *"Persistence is platform-common (`bank-persistence-service`), reached over HTTP"* — **Accepted** | **Superseded by `ADR-019`** |
| `AGENTS.md` service table | *"owns the DB for all consumers"* | **Corrected** |
| `CURRENT-STATE.yaml` standing constraint | *"Persistence is platform-common, not 1SB-owned"* | **Replaced** — the "not 1SB-owned" half survives |

### 4a.2 What does not change

Three things that look adjacent and are unaffected, stated so nobody re-opens them:

- *"Bank apps never call 1SB or a database directly"* — **stands**.
- *"`1sb-integration-service` owns no Flyway migrations and no JPA"* — **stands**, and its ArchUnit
  enforcement stands. The service still reaches its job store over HTTP; what changed is that the
  store belongs to a narrowly scoped context rather than a platform-wide gateway. Only the old
  *reason* for the rule (*"persistence is platform-common"*) retired, not the rule.
- *"No second audit database"* — **stands**. Audit ingest stays with the Integration Operations /
  Evidence context.

### 4a.3 What is still owed

| # | Owed | Owner | When |
|---|---|---|---|
| 1 | Integrity and recovery guarantees for the allocation (Q4), including the restore test against RPO 5 min / RTO 30 min | **Aarti — not substitutable** | S09, before the migration runs |
| 2 | The table-by-table allocation from `bank_persistence` to owning contexts | Aarti + Mahesh | S09, independently reviewed |
| 3 | Confirmation that the surviving Integration Operations / Evidence scope is drawn where `ADR-019` rule 3 intends | Mahesh + Amit | S09 |

---

## 5. Impact

| | |
|---|---|
| **Scope** | No product scope change. Bounded contexts and journeys are unaffected whichever option wins |
| **Stage** | Does not block `GATE-S08`. Options B and C would be S09+ implementation work |
| **Dependencies** | **Blocks nothing in `CR-014`.** `C-ARC-3` migrates the service as-is. Kalpana has recorded that this CR must not be pulled onto the migration's critical path |
| **Decisions at stake** | Three Accepted decisions plus one ArchUnit-enforced rule (Amit rule 3) |
| **Effort** | A: S · C: M · B: **L–XL** with a data migration |
| **Risk if not decided** | The platform sits in a bank estate in visible contradiction of that estate's governing specification, with no recorded position on it. That is worse than either answer |

---

## 6. What this CR does **not** do

- It did **not** propose a verdict or a preferred option. Four options were put without a recommendation attached, and the boards chose B.
- It **does** now supersede one Accepted decision (*"Persistence is platform-common"*), via `ADR-019`. The other two — *"Integration service owns no Flyway/JPA"* and *"Bank apps never call 1SB or the DB directly"* — **stand unchanged**; see §4a.2.
- It does **not** block `CR-014` or any `GLM-001` phase.
- It does **not** authorise the S09 allocation migration. That needs Aarti's independent review and its own plan.
- It does **not** substitute for Aarti. Q4 remains hers and remains unanswered; approving a target model did not answer it.

---

## 7. Change request record

```yaml
change_request:
  id: CR-015
  raised_by: "agent:claude"
  date: 2026-08-29
  type: CONSTRAINT           # potential REVERSAL of an Accepted decision
  driver: >
    External approved baseline disagrees with a ratified internal decision. GitLab Terraform
    Bootstrap Requirements v1.0 section 3.3 forbids a generic shared persistence service for all
    domains; the decision register carries "Persistence is platform-common" as Accepted.
  evidence:
    - "Bank baseline section 3.3 — each bounded context owns its write model/datastore"
    - "DECISION-REGISTER section 1 — 'Persistence is platform-common (bank-persistence-service)', status Accepted"
    - "AGENTS.md — bank-persistence-service 'owns the DB for all consumers'"
    - "GLM-001 IMP-2"
  impact:
    scope: "no product scope change under any option"
    stage: "does not block GATE-S08; options B and C are S09+ work"
    dependencies: "blocks nothing in CR-014 — C-ARC-3 migrates the service as-is"
    parked_items: "none"
    effort: "S to XL depending on option"
    risk_if_rejected: >
      The platform sits in a bank estate in visible contradiction of that estate's governing
      specification with no recorded position on it.
  alternatives_considered:
    - option: "resolve it inside CR-014, while the repositories are being split anyway"
      consequence: >
        A boundary change inside a repository move is indistinguishable from the move in the diff
        and cannot be reviewed afterwards. Rejected by C-ARC-3.
  decision: APPROVED
  option_selected: "B — persistence ownership per bounded context, implemented after migration"
  approvers: ["Board 1 Architecture", "Database (Aarti) — target model only"]
  decided_on: "2026-08-29"
  recorded_by: "agent:claude, from a board outcome relayed by human:Mahesh on 2026-08-29"
  adr: ADR-019
  conditions:
    - "During CR-014 bank-persistence-service migrates UNCHANGED; repository migration is never combined with persistence restructuring"
    - "R0 may use one Aurora cluster but must use separate schemas per context and no cross-schema grants"
    - "Customer, Lead, Consent, Suitability, Catalogue, Quotation, Proposal, business Payment, Policy and Journey never persist through bank-persistence-service"
    - "The table allocation runs as an independently reviewed S09 migration; Aarti's integrity and recovery review (Q4) is outstanding and not substitutable"
  signature_status: >
    Target model approved and relayed 2026-08-29. Aarti's review of the S09 allocation migration is
    preserved and outstanding — the target is decided, the data migration is not approved.
```
