# S06 — Domain & Information Architecture

**AIGEM stage:** L2 — Domain / Aggregate Design · **Owner:** Mahesh (Architecture) + Aarti (Data)
**Central question:** *What are the concepts, their lifecycles and their invariants?*

---

## 1. Purpose

Establish the conceptual model the whole system will be built on: what things exist, who owns
them, how they change state, and what must always be true about them.

Domain errors are the most expensive class of defect because they surface late and cost the most
to correct. A wrong aggregate boundary is discovered when two services need to update the same
data transactionally, and by then both services exist.

## 2. Entry criteria

- [ ] GATE-S03 passed: requirements, business rules and information model
- [ ] GATE-S02 passed: data classification and control obligations

## 3. Epics and stories

### S06-E01 — Bounded context map · *Mahesh*

| ID | Story | Acceptance criteria |
|---|---|---|
| S06-E01-S01 | Derive contexts from capabilities | Every context traces to capabilities; every capability lands in exactly one context |
| S06-E01-S02 | Define context relationships | Upstream/downstream, conformist, anti-corruption layer, shared kernel — named per pair |
| S06-E01-S03 | Define the language boundary per context | Where the same word means different things, and how it translates at the seam |
| S06-E01-S04 | Justify each context's existence | Each context has a distinct reason to change. A context that always changes with another is not a context |

### S06-E02 — Aggregate and state design · *Mahesh*

| ID | Story | Acceptance criteria |
|---|---|---|
| S06-E02-S01 | Identify aggregates per context | Consistency boundary stated: what must be transactionally consistent |
| S06-E02-S02 | Define the state machine per aggregate | States, permitted transitions, triggers, and terminal states |
| S06-E02-S03 | Define invariants per aggregate | What must always be true; which are enforced where |
| S06-E02-S04 | Define aggregate identity and lifecycle | How it is created, identified, referenced across contexts, and archived |
| S06-E02-S05 | Design the journey state machine | The cross-context saga: quote → proposal → payment → issuance, with compensations for each failure point |

### S06-E03 — Domain invariants and rules placement · *Mahesh + Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S06-E03-S01 | Catalogue every invariant | From the S03 rules catalogue, each mapped to the aggregate that enforces it |
| S06-E03-S02 | Place compliance hard-gates | Where suitability, consent and attribution are enforced — enforcement point named per control |
| S06-E03-S03 | Define what happens when an invariant is violated | Reject, compensate, or escalate — per invariant, not generically |
| S06-E03-S04 | Identify cross-aggregate consistency needs | Where eventual consistency is acceptable and where it is not (money is not) |

### S06-E04 — Logical data model · *Aarti*

| ID | Story | Acceptance criteria |
|---|---|---|
| S06-E04-S01 | Build the logical model per context | Entities, attributes, keys, relationships, normalised |
| S06-E04-S02 | Define the data ownership matrix | Exactly one context may write each field; others read via contract |
| S06-E04-S03 | Define data classification at field level | Every field carries its S02 classification |
| S06-E04-S04 | Define reference and master data | Product catalogue, insurer, branch, user — source of truth per set |
| S06-E04-S05 | Define the audit data model | What an immutable, attributable, reconstructable audit record contains |

### S06-E05 — Canonical contracts · *Mahesh*

| ID | Story | Acceptance criteria |
|---|---|---|
| S06-E05-S01 | Define the canonical model | The bank's own model, independent of any provider's API shape |
| S06-E05-S02 | Define provider translation boundaries | Provider types never leak past the adapter — the existing `adapter.onesb.*` rule, generalised |
| S06-E05-S03 | Define canonical events | Domain events per aggregate transition, with payload and versioning policy |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S06-VT-01 | Contexts are cohesive | For each, ask what would cause it to change | One reason per context |
| S06-VT-02 | Aggregate boundaries are right | Walk each business transaction; count aggregates it must update atomically | Never more than one |
| S06-VT-03 | State machines are complete | Enumerate transitions; check every state is reachable and every non-terminal state has an exit | No orphan or dead-end states |
| S06-VT-04 | Invariants are enforceable | For each, name the code location that will enforce it | 100% placed |
| S06-VT-05 | The saga handles failure | Walk each failure point in the journey | Every failure has a defined compensation or a defined manual procedure |
| S06-VT-06 | Data ownership is unambiguous | Sample 30 fields; ask which context writes them | Exactly one answer each |
| S06-VT-07 | Audit model reconstructs a sale | Take a completed journey; reconstruct it from audit records alone | Full reconstruction possible |
| S06-VT-08 | Canonical model is provider-neutral | Review for provider-shaped concepts | No 1SB or insurer vocabulary in the canonical model |

**S06-VT-05 deserves emphasis.** The failure points in this journey are where the money and the
regulatory exposure sit: payment succeeded but issuance failed; issuance succeeded but
reconciliation failed; consent captured but journey abandoned. Each needs a designed answer
before implementation, not an incident afterwards.

## 5. Exit gate — GATE-S06

| # | Criterion | Evidence level | Evidence artefact |
|---|---|---|---|
| S06-G1 | Bounded context map with relationships and justifications | E2 | Context map, architecture-reviewed |
| S06-G2 | Aggregates with state machines and invariants | E2 | Domain model document |
| S06-G3 | Journey saga designed including compensations | E2 | Saga design with failure matrix |
| S06-G4 | Logical data model per context | E2 | Data model, signed by Aarti |
| S06-G5 | Data ownership matrix complete | E1 | Ownership matrix |
| S06-G6 | Compliance hard-gate enforcement points named | E2 | Control placement map, Compliance-reviewed |
| S06-G7 | Audit model proven to reconstruct a journey | E3 | Reconstruction walkthrough record |
| S06-G8 | Canonical model is provider-neutral | E2 | Architecture review verdict |

**Approvers:** Mahesh (AP) · Aarti (AP) · Rajal (AP, semantics) · Shailja (RV) · Deepali (RV) ·
Swapnali (RV) · Shivanshi (RV)

## 6. Current position in this repository — 🟡 Partial

**Present:** 19 bounded contexts are named with domain ownership and datastore engine in the
business problem statement. The 1SB integration service has a genuine domain layer with ports,
commands and models, and a canonical-model document set covering contexts and payload
simplification. ArchUnit enforces the provider-isolation rule — S06-E05-S02 is implemented and
tested, which is more than most programmes manage.

**Open:**

| Item | Detail |
|---|---|
| Context relationships | Contexts are listed, not related. No upstream/downstream or ACL designations |
| Platform aggregates | No aggregate or state model exists for Lead, Consent, Suitability, Proposal, Payment, Policy |
| **Journey saga** | **Not designed.** This is the central design artefact for a multi-step, multi-party sale and it does not exist |
| Domain invariants | Not catalogued platform-wide |
| Data ownership matrix | Absent |
| Logical data model | Partial — blocked on GAP-016 attribute sheets |
| Audit model | Exists for 1SB; not proven to reconstruct a business journey |

**The saga gap is the important one.** Payment-succeeded-but-issuance-failed is not an edge case
in bancassurance; it is a routine occurrence with direct financial and regulatory consequences.
Designing it after the services exist means retrofitting compensation logic across service
boundaries, which is exactly the expensive class of domain error this stage prevents.

## 7. Premature at this stage

Physical schema and indexing · messaging technology · caching · persistence tuning ·
observability stacks · framework selection.

S06 is about concepts. The moment a discussion becomes "should this be a Kafka topic", it has
left S06 and entered S07 — usually before the concept it would carry has been agreed.
