# 13 — Mahesh Orchestration Doctrine

## 1. Purpose

Insurance distribution is a long-running, multi-party, partially-observable business process. That
makes orchestration the area where architecture most often goes wrong in an expensive and
irreversible way: the orchestrator accumulates other contexts' decisions until it is a distributed
monolith with a state table.

This file defines where coordination lives, how it communicates, and what it is forbidden to know.

**Governing rule (OR-01).** *Orchestration coordinates. It never decides.* The orchestrator knows
what has happened and what may happen next. It never knows **why** a suitability assessment passed,
**how** a premium was calculated, or **whether** a payment reconciled — only that the owning
capability says so.

---

## 2. Where coordination lives

Four distinct coordination responsibilities, deliberately separated:

| Responsibility | Owner | Question |
|---|---|---|
| **Journey routing** | `CAP-106` Journey Registry | Which LOB cell executes this journey? |
| **Process orchestration** | `CAP-201` Journey Execution (per cell) | What is the next step in *this* line of business? |
| **Work orchestration** | `CAP-103` Work Management | Which human or queue should act, by when? |
| **Engagement orchestration** | `CAP-306` Engagement | Should we reach out, when, and through which cadence? |
| **Provider orchestration** | `CAP-403` Aggregation & Provider Connectivity | Which provider receives this request, and how do we communicate with it? |

**Rule OR-02 — these five never merge.** Each merge has a known failure:

| Merge | Failure |
|---|---|
| Routing + execution | The shared registry becomes a state machine containing every LOB (`JS-03`) |
| Execution + work management | The journey cannot be resumed by a different actor without inheriting queue state (`JS-18`) |
| Work management + engagement | Every SLA timer becomes a customer message |
| Engagement + notification | Business cadence rules live inside a delivery service (`NS-08`, `JS-17`) |
| **Journey + provider orchestration** | One service holds customer journey state **and** provider routing **and** retry policy **and** canonical transformation **and** insurer credentials — the platform's single point of coupling, holding its highest-value secrets (`TI-20`, [`17 §4`](./17-provider-aggregation-and-connectivity.md)) |

---

## 3. Orchestration versus choreography

Both exist in this platform, on purpose.

| Use **orchestration** when | Use **choreography (events)** when |
|---|---|
| A business process has a named owner accountable for its completion | The producing context's work is done and it does not care who reacts |
| Steps must occur in order, with compensation on failure | Consumers are independent and may be added without changing the producer |
| A human is waiting for a coordinated outcome | Processing is eventual and non-blocking |
| Partial failure needs a recovery path someone owns | Failure of a consumer must not affect the producer |

**Rule OR-03 — the sale is orchestrated; its consequences are choreographed.** Quote → proposal →
payment → issuance is an orchestrated process with an owner. `PolicyIssued` and its fan-out are
choreographed.

**Rule OR-04 — never choreograph a process that needs a completion guarantee.** If nobody can answer
*who is accountable for this finishing*, events have been used to avoid making an ownership
decision (`04 §5`).

---

## 4. Synchronous versus asynchronous

The existing seam catalogue
([`03-solution-architecture-r0.md §5`](../../../platform/ws3-platform/03-solution-architecture-r0.md),
`S-01`–`S-19`) is the worked example and remains binding for R0. The rules generalise it.

**Rule OR-05 — synchronous while a human waits in session; asynchronous when the far side is a
machine on its own clock.** Nine of nineteen R0 seams are synchronous for exactly this reason.

**Rule OR-06 — every seam declares four properties.** Style · idempotency mechanism · timeout and
retry posture · behaviour when the far side fails. **A seam with no failure row is an undesigned
seam**, and Mahesh returns `REWORK` on that basis alone.

**Rule OR-07 — end-to-end budgets are allocated top-down.** The edge budget is the constraint; each
hop gets a share. Timeouts chosen per-service in isolation always sum to more than the user will
wait.

**Rule OR-08 — fail closed on control seams.** Authorization (`S-02`, 300 ms, no retry), suitability
gate (`S-08`, a cache miss is a refusal, never an allow) and consent are non-degradable. Availability
pressure is never a reason to soften them.

**Rule OR-09 — provider submits use async-poll and are never automatically retried.** Quote and
proposal submission (`S-09`, `S-12`) recover by *polling for the outcome*, not by resubmitting.
Re-submitting a possibly-processed proposal is the failure this rule exists to prevent, and it is
the difference between a delay and a duplicate policy.

**Rule OR-10 — a missing callback is resolved by reconciliation, never by assumption.** A payment
with no callback is `UNCERTAIN` until reconciliation resolves it (`S-14`, `S-15`). "It probably went
through" is not a state transition.

---

## 5. Compensation and recovery

**Rule OR-11 — compensation is durable, owned and bounded.** Every orchestrated step that can leave
partial state declares its compensating action, its bounded attempt budget, and what happens on
exhaustion. Exhaustion means `MANUAL_INTERVENTION` **with a named owner** (`S-19`, `F-05`) — never
a silent stall.

**Rule OR-12 — compensation is not rollback.** Distribution has irreversible external effects: an
insurer has received a proposal, a customer has been charged, a policy exists. Compensation is a
*business* counter-action (cancel, refund, re-quote, void), and it must be modelled as such.

**Rule OR-13 — no distributed transactions.** Local ACID inside a boundary, sagas across boundaries
(`AP-06`). Two-phase commit across services or across a provider is not available and not proposed.

**Rule OR-14 — the recovery mechanism must match the failure domain.** Database restore does not
resolve whether the payment gateway captured a payment; reconciliation does. Mahesh states the
recovery mechanism per failure class, not per store.

---

## 6. Events

**Rule OR-15 — transactional outbox always; a broker when the trigger fires.** Restated
2026-08-24, because **the trigger fired and the rule survived it.**

> **The trigger fired for WS-3 on 2026-08-24.** R0's design has three consumer classes — audit
> (`S-17`), notification (`S-18`) and compensation (`S-19`) — so the "third distinct consumer
> class" condition was met inside R0 rather than after it.
> [`ADR-012`](../../../platform/architecture-review/08-architecture-decision-log.md) admitted
> Amazon MSK, and [`03-solution-architecture-r0.md §5.1`](../../../platform/ws3-platform/03-solution-architecture-r0.md)
> was rewritten accordingly.
>
> **What the rule keeps, and this is the part that matters:** the outbox did **not** go away. A
> service still writes its business change and its outbox row in one local transaction; the
> publisher then puts it on a topic. The outbox is the source of truth and the replay log, and the
> broker is transport and fan-out. Replacing the outbox with direct publishing would reintroduce
> the dual-write bug — commit, then publish, two writes with no shared transaction.
>
> So "outbox now, broker later" was never a sequence of two mechanisms. It was one mechanism plus a
> deferred transport, and only the transport arrived.

`VIN-001 §30` and the prior §5.1 agreed independently on the outbox approach and on adopting an
event bus when justified. Both halves held.

> **Remaining revisit trigger, now pointing the other way** (`ADR-012`): if R0 completes with one
> real consumer class and no replay ever used, the broker is a cost to withdraw rather than a
> decision to defend. Toward growth: sustained consumer lag that partition-level scaling cannot
> absorb, or a cross-region consumer.

`VIN-001 §30`'s own example is what will fire it: `PolicyIssued` triggering Policy Portfolio,
Notification, Reporting, Audit, Renewal scheduling and Commission/Finance is **six consumer
classes**. So the trigger is not hypothetical — it is scheduled by the roadmap. `AP-09` still
applies: it fires when those consumers exist, not when they are drawn.

**Rule OR-16 — events are contracts.** Versioned, owned by the producing capability, consumer-driven
contract tests in CI (`FF-15`). An event schema change is a public contract change.

**Rule OR-17 — at-least-once delivery, idempotent consumers.** Every consumer deduplicates on
`eventId`. No consumer may assume exactly-once.

**Rule OR-18 — events carry references and facts, not decisions to be re-derived.** An event says
*what happened*, with the identifiers needed to fetch detail. It does not carry another context's
reasoning, and it does not carry PII beyond what the consumer is entitled to (`INV-LOG-01`, and
Shailja on data minimisation).

**Rule OR-19 — audit is a consumer, and a blocking one.** Business transactions commit locally, but
a journey is blocked from `SOLD` until audit confirms (`INV-JRN-05`, `S-17`). This is the one place
where an asynchronous consumer gates a business outcome, and it is deliberate.

### 6.1 Canonical event taxonomy

**Rule OR-20 — events are named for business facts in the past tense, owned by one capability.**

| Event | Producer | Typical consumers |
|---|---|---|
| `PartyLinkedToCustomer` | `CAP-101` | Registry, Audit |
| `OpportunityCreated` / `OpportunityDispositioned` | `CAP-102` | Work Mgmt, Reporting, Audit |
| `JourneyStarted` / `JourneyStageAdvanced` / `JourneyAbandoned` | `CAP-106` | Work Mgmt, Engagement, Timeline, Audit, Reporting |
| `ConsentGranted` / `ConsentRevoked` | `CAP-104` | Journey, Audit |
| `SuitabilityAssessmentCompleted` | `CAP-202` | Journey, Audit |
| `QuoteReady` / `QuoteExpired` | `CAP-203` | Journey, Engagement, Audit |
| `ProposalSubmitted` / `RequirementRaised` / `CounterOfferReceived` | `CAP-204` | Journey, Notification, Work Mgmt, Audit |
| `PaymentLinkReady` / `PaymentAuthorised` / `PaymentReconciled` / `PaymentUncertain` | `CAP-301` | Journey, Notification, Audit, Finance |
| **`PolicyIssued`** | `CAP-205` (LOB provider integration, on issuance success) | **Policy Portfolio · Notification · Reporting · Audit · Renewal scheduling · Commission/Finance** |

`PolicyIssued` is the canonical example of `OR-03`: the LOB cell's provider integration owns the
issuance interaction (`VIN-001 §19`), emits one canonical fact, and six independent consumers react
without the producer knowing they exist.

---

## 7. Process engines

**Rule OR-21 — no BPM or workflow engine without a requirement it uniquely satisfies.** "Journeys are
long-running" is not that requirement — a state machine with durable state and a poller handles
long-running perfectly well, and this platform already has one.

A process engine earns consideration only when **several** of these are true and evidenced:

1. Business users must author and version process definitions without a deployment.
2. Human task management (worklists, claiming, delegation, escalation) is a first-class product
   requirement across many processes — note this is `CAP-103`'s job, so check there first.
3. In-flight process migration across definition versions is genuinely required.
4. Process definitions genuinely change faster than code can be released.
5. Cross-process visibility and analytics cannot be met by the event stream and the timeline
   (`CAP-307`).

Otherwise: durable state machine + outbox + work management. Consequences to weigh before adopting
one: a second runtime with its own persistence, HA, upgrade and DR story; a second place where
business logic lives; an authorization surface outside `CAP-502`; and audit evidence split across
two systems (`TI-07`).

**Rule OR-22 — process versioning is required whatever the mechanism.** In-flight journeys keep the
process version they started under. A rule-pack or stage-definition change must not retroactively
alter a journey already in progress, because the evidence trail asserts what the rules *were* at the
time (`CONSENT-PACK`/`SUITABILITY-PACK` versioning already assumes this).

---

## 8. Multi-provider coordination

**Rule OR-23 — partial success is success.** Multi-quote fan-out returns what it has; per-product or
per-insurer failures surface without failing the whole request (`INV-QUO-02`, `S-10`). Zero offers is
a failure; four out of five is a result.

**Rule OR-24 — bulkheads per provider, and at H2 per LOB (`TI-18`).** One failing insurer must not
consume the connection budget that makes every other insurer look down. This is an architecture
property, not a tuning knob.

**Rule OR-24a — provider orchestration is invisible to the journey.** The LOB service knows it asked
for a quote. Nothing in its contract, data model or error handling tells it whether the answer came
from 1SB or from a direct insurer (`TI-19`, `PR-07`, `PR-15`).

**Rule OR-25 — the aggregator's asynchrony is not the platform's asynchrony.** 1SB's poll model is a
provider protocol detail confined to `CAP-205`. The platform's own journey semantics must not be
shaped by one aggregator's polling contract, or replacing it (`TI-16`) becomes a journey change.

---

## 9. Anti-patterns

| Anti-pattern | Consequence | Correct move |
|---|---|---|
| Orchestrator holds business decisions "for convenience" | Distributed monolith; gates become bypassable by writing state | `OR-01`, `JS-18` |
| Auto-retrying a provider submit | Duplicate proposals, duplicate policies | `OR-09` |
| Assuming a missing callback succeeded | Unreconciled money treated as settled | `OR-10` |
| Event bus because "the target is event-driven" | Operational cost with no consumer to justify it | `OR-15` |
| BPM engine because journeys are long-running | Second runtime, split logic, split audit | `OR-21` |
| Sagas everywhere | Compensation complexity where a local transaction would do | `AP-06`, `OR-13` |
| Timeouts chosen per service | Aggregate budget exceeds what any human will wait | `OR-07` |
| Degrading the authorization or suitability seam under load | Control removed exactly when the system is stressed | `OR-08` |
| Events carrying whole aggregates including PII | Data minimisation breach; consumers coupled to producer internals | `OR-18` |
| Failing the whole multi-quote on one insurer error | Customer sees nothing when four insurers answered | `OR-23` |
| Retroactively applying a new rule version to in-flight journeys | Evidence no longer matches what the customer was shown | `OR-22` |

---

## 10. Authority

| Decision | Authority |
|---|---|
| Seam style, timeout posture, compensation design, event taxonomy | `A1_AUTONOMOUS` — Mahesh, ADR when durable |
| Introducing an event broker | `A2_NOTIFY` + ADR; Shivanshi on operability, Aarti where persistence is material |
| Introducing a process/BPM engine | `A2_NOTIFY` + ADR + `OR-21` evidence; **Security review of its authorization surface** |
| Changing a control seam's failure posture (`OR-08`) | `A3_JOINT_REVIEW` — Shailja and Deepali |
| Changing money-path orchestration or reconciliation | `A3_JOINT_REVIEW` — Shailja + Finance control owner |
| Accepting a manual-intervention path at go-live without a named owner | `A4_HUMAN_REQUIRED` |
