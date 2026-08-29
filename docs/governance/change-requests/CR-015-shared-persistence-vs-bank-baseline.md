# CR-015 — `bank-persistence-service` versus bank baseline §3.3: shared persistence as an accepted decision

**Date:** 2026-08-29
**Type:** CONSTRAINT (potential `REVERSAL` of an Accepted decision)
**Raised by:** `agent:claude` in the Shivanshi (SRE / `R10`) persona, as finding `IMP-2` on [`GLM-001`](../../platform/gitlab-migration/GLM-001-migration-plan.md)
**Workstream:** WS-3 · **Stage:** S08 with S09 overlapped
**Jurisdiction:** **Joint — Mahesh (Board 1, Architecture) and Aarti (Database).** Neither alone.
**Related:** [`CR-014`](./CR-014-gitlab-estate-migration.md) — parallel, and deliberately **not** a dependency

> ## Decision: `PENDING`
> **No verdict is drafted in this CR, by design.** Mahesh's card makes a shared datastore,
> cross-service database access or a source-of-truth change a **mandatory joint review with Aarti
> before any verdict**. An AI position on one half of a joint review is not half an answer — it is a
> way of making the joint review look already-settled. Aarti has not been convened.
>
> **`signature_status: NO POSITION DRAFTED — joint Mahesh + Aarti review not yet held`**

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

- It does **not** propose a verdict, a recommendation, or a preferred option.
- It does **not** reverse, weaken or cast doubt on any Accepted decision. All three stand until a
  joint review says otherwise on new evidence.
- It does **not** block `CR-014` or any `GLM-001` phase.
- It does **not** authorise a schema, persistence or service-boundary change of any kind.
- It does **not** substitute for Aarti. Q4 in particular cannot be answered by anyone else, and an
  AI-drafted persistence-integrity conclusion would be a manufactured one.

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
  decision: PENDING
  approvers: []
  decided_on: null
  conditions: []
  signature_status: "NO POSITION DRAFTED — joint Mahesh + Aarti review not yet held; Aarti not convened"
```
