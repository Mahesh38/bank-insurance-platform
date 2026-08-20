# 10 — Mahesh North Star Capability Model

## 1. Purpose and the ordering rule

This is the artefact `09 §1` requires before any target-state diagram is drawn.

> **Ordering rule (NS-01).** Capability first, ownership second, deployment third, diagram last.
> A request for "the target architecture diagram" is answered by finishing the capability model and
> then observing that the diagram has become small and obvious.

The model exists because the most expensive mistakes on a platform like this are not made in code.
They are made when a capability is assumed rather than defined — when nobody can say what
*Journey* owns, so it slowly acquires everything; or what *Lead* owns, so quoting logic drifts into
it; or what the bank owns versus the insurer, so an "underwriting service" gets planned for risk
decisions the bank is not permitted to make.

---

## 2. The capability definition contract

**Rule NS-02.** No capability enters the target architecture until all six questions have
defensible answers. "Defensible" means sourced — from a requirement, an accepted decision, a
regulatory obligation or a measured constraint (`AP-12`).

| # | Question | What a bad answer looks like |
|---|---|---|
| 1 | **Why does this capability exist?** | A noun. "Because we need a Customer service." |
| 2 | **What does it own?** | A list of screens, or a list of tables |
| 3 | **What does it *not* own?** | Silence — the single most common cause of boundary rot |
| 4 | **Who uses it?** | "Everyone" |
| 5 | **What systems does it communicate with?** | A diagram arrow with no style, no failure mode |
| 6 | **Why shared or LOB-specific?** | "It feels shared" |

Question 3 carries the most weight. A capability that has never been told what it does not own will
be asked to own it eventually, by someone under delivery pressure, in a sprint nobody reviews.

### 2.1 Contract schema

```yaml
capability_contract:
  id: CAP-XXX
  name: "..."
  plane: 1 | 2 | 3 | 4 | 5
  exists_because: "..."               # Q1 — the business or regulatory reason, sourced
  owns: []                            # Q2 — authoritative data and decisions
  does_not_own: []                    # Q3 — explicit, with the owner named
  used_by: []                         # Q4 — actors and capabilities
  communicates_with:                  # Q5 — each with style and failure posture
    - {target: "...", style: sync | async-event | async-poll | batch, on_failure: "..."}
  sharing: SHARED | LOB_SPECIFIC | SHARED_FRAMEWORK_ISOLATED_RUNTIME
  sharing_rationale: "..."            # Q6
  authoritative_data: []
  invariants: []                      # TI-.. and INV-.. it must uphold
  horizon: H0 | H1 | H2 | H3
  r0_mapping: "#n <context> | none"
  deployable_today: true | false      # deployment is a separate decision — see §4
  open_questions: []
```

### 2.2 Sequencing

`VIN-001` closes with the order in which to do this work, and it is adopted:

> **Customer → Opportunity/Lead → Journey → LOB Cell.**
> Once those four are absolutely clear, the rest of the model falls out of them.

Full contracts for those four are in §6. Everything else in §7 is at catalogue depth until it is
next in line — deliberately, because a model where all thirty capabilities are equally shallow is
worse than one where four are deep.

---

## 3. Capability is not microservice

**Rule NS-03.** A capability is an *ownership* boundary. A microservice is a *deployment* boundary.
They are related the way a job description is related to a person: one may hold several, and the
mapping changes without either concept changing.

`VIN-001 §2` states this and gives the two worked examples the repository should carry:

- **Party + Customer may initially be one service.**
- **Proposal + insurer requirement tracking may initially be one service** — and this one is
  already true in R0, where context #11 is named *Proposal & UW-Tracking*.

**Rule NS-04 — the promotion test.** A capability is promoted to its own deployable unit only when
the boundary test in `04 §4` returns evidence, not preference. In practice one of:

| Trigger | Evidence required |
|---|---|
| Independent scaling profile | Measured or credibly modelled load divergence |
| Independent release cadence | Two teams blocked on each other's release, observed |
| Failure isolation requirement | A failure mode that in-process isolation cannot contain |
| Security or compliance isolation | A control that requires a separate trust or credential boundary |
| Separate team ownership | An actual team, not a planned one |

**Rule NS-05 — demotion is legal.** If two services never change independently, never scale
independently and never fail independently, merging them is a valid architecture decision, not an
admission of failure. Record it as an ADR like any other.

**Rule NS-06 — the count is an output.** Mahesh never opens a target-state answer with a service
count. If asked for one, he gives the capability count, the plane structure, and the deployable
count *for the stated horizon*, with the promotion evidence for each split.

---

## 4. The five planes

`VIN-001` reduces the North Star to five ideas. This is adopted as the top-level structure of the
target state, because it is the version a stakeholder can hold in their head — and an architecture
nobody can restate is an architecture nobody will defend.

| Plane | Name | The question it answers |
|---|---|---|
| **1** | Shared Customer & Sales Platform | *Who is the customer, why are we contacting them, what are we allowed to do, and where are they in the journey?* |
| **2** | Independent Insurance LOB Engines | *How does this specific insurance business actually work?* |
| **3** | Shared Transaction & Portfolio Capabilities | *Which capabilities should not be rebuilt three times?* |
| **4** | External Integration Boundaries | *How do we isolate external-system complexity from the business platform?* |
| **5** | Platform Engineering & Governance | *How do we keep it secure, manageable, scalable and operable?* |

```text
┌─ PLANE 1 — Shared Customer & Sales Platform ─────────────────────────────────┐
│  Party & Customer · Opportunity · Work Management · Consent                   │
│  Product Governance · Journey Registry                                        │
└───────────────────────────────┬──────────────────────────────────────────────┘
                                │ routes by LOB
┌─ PLANE 2 — LOB Cells ─────────▼──────────────────────────────────────────────┐
│   LIFE (H0)          │   HEALTH (H2)        │   GENERAL / MOTOR (H3)          │
│   Journey Execution  │   Journey Execution  │   Journey Execution             │
│   Suitability        │   Suitability        │   Eligibility                   │
│   Quotation          │   Quotation          │   Quotation                     │
│   Proposal / Case    │   Proposal / Case    │   Proposal / Case               │
│   Provider Integration ── shared framework, isolated runtime (09 §5.1) ──────┤
└───────────────────────────────┬──────────────────────────────────────────────┘
┌─ PLANE 3 — Shared Transaction & Portfolio ───▼───────────────────────────────┐
│  Payment · Documents · Policy Portfolio · Notification · Engagement           │
│  Interaction Timeline · Audit & Evidence                                      │
└──────────────────────────────────────────────────────────────────────────────┘
┌─ PLANE 4 — External Integration Boundaries ──────────────────────────────────┐
│  Bank Integration → CBS / bank systems    │   Provider Integration → 1SB / insurers │
└──────────────────────────────────────────────────────────────────────────────┘
┌─ PLANE 5 — Platform Engineering & Governance ────────────────────────────────┐
│  Identity · Authorization · Observability · Events · Configuration           │
│  CI/CD · IaC · Secrets/KMS · DR · SRE · Governance evidence                  │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Rule NS-07 — planes are not layers.** A plane is a *reason for existing*, not a call-stack
position. Plane 3 capabilities are called by Plane 2 and also emit events consumed by Plane 1.
Drawing planes as a strict top-down stack produces the wrong dependency conversation.

**Rule NS-08 — a capability lives in exactly one plane.** If a capability seems to belong to two,
it is two capabilities that have not been separated yet. Notification versus Engagement
(`VIN-001 §27`) is the canonical example: *sending a message* and *deciding whether and when to
engage* look like one capability right up until the sales organisation matures, at which point they
are obviously two.

---

## 5. What the bank owns, and what it does not

The most consequential boundary in the model is not between services. `TI-03`, restated as a
contract because it decides what capabilities may exist at all:

| Concern | Owner |
|---|---|
| Distribution journey, advice, suitability, presentation of products | **Bank platform** |
| Insurer status, outstanding requirements, pending documents, medical requests, counteroffers, next action, communication to customer/RM | **Bank platform** |
| Risk decision, medical assessment, loading, exclusion, rejection, postponement, final acceptance | **Insurer** |
| Policy contract and the insurance liability | **Insurer** |
| Bank-side canonical record of the issued policy for portfolio and servicing | **Bank platform** |

**Consequence:** there is no *Underwriting* capability in this model. There is
**Proposal / Case Management** (`CAP-204`), which tracks an underwriting process the bank observes
but does not perform. Naming it correctly is not pedantry — a capability called "Underwriting
Engine" attracts requirements the bank has no licence to satisfy.

---

## 6. The four foundational contracts

These are the four `VIN-001` says to settle first. They are stated at full contract depth.

### 6.1 `CAP-101` — Party & Customer

```yaml
capability_contract:
  id: CAP-101
  name: "Party & Customer"
  plane: 1
  exists_because: >
    The platform needs one representation of a person irrespective of whether that person is
    currently a bank customer. A DIY visitor exists before the platform knows their CIF; later they
    may authenticate and link to an existing bank relationship. Party (the person) and Bank Customer
    (that person's relationship with the bank) are therefore different concepts with different
    lifecycles, and collapsing them blocks NTB and DIY at H1.
  owns:
    - "Party — the person: identity attributes, contactability, de-duplication and match decisions"
    - "Bank Customer link — the reference from a Party to a CBS/CIF relationship, and its provenance"
    - "Party-level consent references (the evidence itself is owned by CAP-104)"
    - "The freshness policy for any CBS-sourced snapshot it holds"
  does_not_own:
    - "The CBS customer master — owned by Core Banking, reached only via CAP-401"
    - "A replicated copy of all CBS customer data — it holds references and a bounded snapshot"
    - "KYC/CKYC verdicts — insurer/regulatory process surfaced through the LOB cell"
    - "Sales context: source, campaign, assignment, priority — owned by CAP-102"
    - "Any journey state — owned by CAP-106 and the LOB cell"
  used_by: ["RM workspace", "Customer DIY channel", "Call centre", "Certified SP", "every LOB cell", "CAP-102", "CAP-106"]
  communicates_with:
    - {target: "CAP-401 Bank Integration", style: sync, on_failure: "Serve within freshness window; otherwise fail. No stale-unbounded fallback on identity data (S-05)"}
    - {target: "CAP-104 Consent", style: sync, on_failure: "Fail closed — no consent, no personal-data use"}
    - {target: "Audit", style: async-event, on_failure: "Outbox retains; never dropped"}
  sharing: SHARED
  sharing_rationale: >
    The same person buying Life and Health is the same customer. A per-LOB customer record would
    make cross-LOB portfolio, cross-sell, consent and de-duplication impossible, and would produce
    three conflicting answers to "who is this person?".
  authoritative_data: ["Party", "PartyBankCustomerLink", "match/merge decisions"]
  invariants: [TI-02, TI-08, TI-10, "INV-LOG-01 (no PII in logs)"]
  horizon: H0
  r0_mapping: "#4 Customer — R0 implements the ETB half only (CIF-linked snapshot from CBS)"
  deployable_today: true
  open_questions:
    - "Party de-duplication and merge policy across DIY and ETB — Product + Compliance, H1 entry condition"
    - "Whether Party and Bank Customer are one service or two at H1 — apply NS-04"
```

> **R0 delta.** `#4 Customer` today is the *Bank Customer* half. Adding Party at H1 is an extension
> of this capability, not a new one — provided R0 does not bake `customerId == cifId` into contracts
> that H1 must then break. **That is a live H0 design constraint, and it is the cheapest thing in
> this document to get wrong.**

### 6.2 `CAP-102` — Opportunity (Lead)

```yaml
capability_contract:
  id: CAP-102
  name: "Opportunity (Lead)"
  plane: 1
  exists_because: >
    A Lead is a sales/business opportunity, not an insurance transaction. The platform needs a
    durable object that answers "why are we contacting this person?" independently of whether any
    journey has started, so that origination, campaign attribution and conversion measurement
    survive across zero, one or many journeys.
  owns:
    - "Opportunity: source, origination channel, campaign, branch, LOB interest, priority, status, disposition"
    - "The relationship Opportunity → Journeys (one opportunity may produce several over time)"
    - "Opportunity lifecycle: open, working, converted, lost, expired"
  does_not_own:
    - "Quote, Proposal or Payment logic — explicitly excluded (VIN-001 §4)"
    - "Who should act on it: queues, assignment, SLA, escalation — owned by CAP-103"
    - "Journey stage — owned by CAP-106"
    - "The customer record — owned by CAP-101"
  used_by: ["RM workspace", "Branch", "Campaign/bulk upload", "Call centre", "B2C channel", "CAP-103", "Reporting"]
  communicates_with:
    - {target: "CAP-101 Party & Customer", style: sync, on_failure: "Opportunity may exist against an unresolved party; resolution is a task, not a block"}
    - {target: "CAP-103 Work Management", style: async-event, on_failure: "Retry; an unassigned opportunity is visible, not lost"}
    - {target: "CAP-106 Journey Registry", style: async-event, on_failure: "Outbox"}
  sharing: SHARED
  sharing_rationale: >
    Lead generation, origination and attribution mechanics are largely identical across Life,
    Health and Motor. Only `lobInterest` differs, and that is a field, not a boundary.
  authoritative_data: ["Opportunity", "OpportunitySource", "OpportunityDisposition"]
  invariants: [TI-10, TI-13, TI-17]
  horizon: H1
  r0_mapping: "#5 Lead — deliberately deferred to S13; R0 begins a journey from a customer lookup"
  deployable_today: false
  open_questions:
    - "Whether renewal and lapse-recovery opportunities are the same aggregate with a type, or separate — recommend same aggregate, typed"
```

> **Origination set (`VIN-001 §4`):** RM · Branch · Bulk upload · Campaign · B2C · Call centre ·
> Renewal · Lapse recovery · Abandoned-journey recovery. The last three are what make
> `TI-13` structural rather than stylistic: each is an origination *source*, so each naturally
> creates a new opportunity instead of reopening an old journey.

### 6.3 `CAP-103` — Work Management & Assignment

```yaml
capability_contract:
  id: CAP-103
  name: "Work Management & Assignment"
  plane: 1
  exists_because: >
    Something must decide who acts on an opportunity or a stalled journey, and when. This is a
    different question from what the opportunity is (CAP-102) and from where the journey has
    reached (CAP-106). It becomes critical the moment a call centre exists, because that is when
    work starts being routed to a queue rather than to a named person.
  owns:
    - "Queues, work items, assignment (RM / branch / call centre / certified SP)"
    - "Priority, SLA timers, callbacks, escalations, follow-ups, disposition capture"
    - "Routing rules: e.g. Life quote abandoned 24h → Call Centre Life Recovery Queue"
  does_not_own:
    - "The opportunity itself — CAP-102"
    - "Journey state — CAP-106 / LOB cell"
    - "Whether to engage and through which medium — CAP-306 Engagement"
    - "Message delivery — CAP-304 Notification"
    - "Whether an actor is permitted to perform the assigned action — CAP-502 Authorization"
  used_by: ["RM workspace", "Call centre desktop", "Branch ops", "Certified SP app", "Supervisors"]
  communicates_with:
    - {target: "CAP-102 Opportunity", style: async-event, on_failure: "Retry"}
    - {target: "CAP-106 Journey Registry", style: async-event, on_failure: "Retry"}
    - {target: "CAP-306 Engagement", style: async-event, on_failure: "Work item still created"}
  sharing: SHARED
  sharing_rationale: >
    Queueing, SLA and escalation mechanics do not differ by line of business. LOB appears as a
    routing attribute, which is what queue definitions are for.
  authoritative_data: ["WorkItem", "Queue", "Assignment", "SLA state"]
  invariants: [TI-10, TI-14, TI-15]
  horizon: H1
  r0_mapping: "none — R0 has a single RM and no queueing"
  open_questions:
    - "Whether assignment is rules-based or model-assisted at H2 — model-assisted assignment is a consequential-automation question for Shailja"
```

> **Explicit prohibition (`VIN-001 §5`):** do not put work management inside Journey or Quote.
> Both will accept it and neither can give it back.

### 6.4 `CAP-106` — Journey Registry

```yaml
capability_contract:
  id: CAP-106
  name: "Journey Registry"
  plane: 1
  exists_because: >
    A journey must survive channel and actor changes. A customer who starts on mobile, is helped by
    the call centre, continues with an RM and pays on web is on ONE journey (TI-12). Something
    shared must therefore own journey identity, current stage at coarse grain, current owner and
    routing — while the detailed state machine stays inside the LOB that understands it.
  owns:
    - "journeyId — the identity that survives channel, actor and device change"
    - "Who the journey is for (party ref), which LOB, which opportunity ref"
    - "Coarse stage, lifecycle status (active / abandoned / completed), current assisting actor and channel"
    - "Routing: which LOB cell executes this journey"
    - "Journey history as immutable record (TI-13)"
  does_not_own:
    - "LOB-specific stage detail, transitions or business rules — owned by the cell (CAP-201)"
    - "Any other context's business decision (SC-W3-6 / INV-JRN-02) — it holds stage and references only"
    - "Quote, proposal, payment or policy data — references only"
    - "Assignment and SLA — CAP-103"
  used_by: ["All channels/BFFs", "CAP-103", "CAP-306", "CAP-307", "Reporting", "every LOB cell"]
  communicates_with:
    - {target: "LOB cell Journey Execution (CAP-201)", style: sync, on_failure: "Journey holds at current stage; no partial advance"}
    - {target: "Audit", style: async-event, on_failure: "Outbox; journey blocked from terminal SOLD until audit confirms (INV-JRN-05)"}
  sharing: SHARED
  sharing_rationale: >
    Journey identity, ownership and routing are cross-LOB by definition — a customer's journeys must
    be listable and resumable regardless of line. Execution is LOB-specific because Life, Health and
    Motor genuinely differ; keeping execution here would make the shared service a giant state
    machine containing every insurance type (VIN-001 §7), which is the failure this split prevents.
  authoritative_data: ["JourneyIdentity", "JourneyOwnership", "JourneyLifecycleStatus", "JourneyRouting"]
  invariants: [TI-12, TI-13, TI-14, "SC-W3-6", "INV-JRN-02", "INV-JRN-05"]
  horizon: H0
  r0_mapping: "#9 Journey Orchestration — R0 combines Registry and Life execution in one service (legitimate under NS-03)"
  deployable_today: true
  open_questions:
    - "Coarse stage vocabulary shared across LOBs — must be small enough that adding Motor does not change it (LS/JS rules in files 11 and 12)"
```

> **R0 delta.** `#9 Journey Orchestration` today *is* Registry + Life execution in one deployable
> unit. That is correct for H0 under `NS-03`. The split becomes real at H2, when Health arrives —
> and it is cheap **only if** the shared coarse stage vocabulary was kept LOB-agnostic from the
> start. See [`12 §4`](./12-journey-segregation.md).

---

## 7. Capability catalogue

Catalogue depth: identity, ownership summary, sharing verdict and R0 mapping. Full contracts are
written when a capability comes into scope (`NS-02`), in the order of §2.2.

### Plane 1 — Shared Customer & Sales Platform

| ID | Capability | Owns | Does **not** own | Sharing | Horizon | R0 |
|---|---|---|---|---|---|---|
| `CAP-101` | Party & Customer | Party; bank-customer link; snapshot freshness | CBS master; sales context; journey state | SHARED | H0 | #4 |
| `CAP-102` | Opportunity (Lead) | Opportunity, source, campaign, disposition | Quote/proposal/payment logic; assignment | SHARED | H1 | #5 (deferred) |
| `CAP-103` | Work Management & Assignment | Queues, assignment, SLA, escalation, callbacks | Opportunity; journey state; engagement decision | SHARED | H1 | none |
| `CAP-104` | Consent | Consent **evidence**: who, what data, which purpose, which parties, text version, capture method, revocation, expiry | Whether a business action is lawful (Shailja); journey gating logic | SHARED | H0 | #6 |
| `CAP-105` | Product Governance | Insurer master; insurer product; **bank-approved offering**; effective dates; enabled channels; ETB/NTB availability; integration route; eligibility metadata; status | Pricing; insurer underwriting rules; LOB quote construction | SHARED | H0 | #8 |
| `CAP-106` | Journey Registry | Journey identity, ownership, routing, lifecycle | LOB stage detail; other contexts' decisions | SHARED | H0 | #9 (combined) |

> **`CAP-104` — why consent is a capability, not a flag.** Consent is *evidence, not a Boolean*
> (`VIN-001 §15`). A field named `consentGiven=true` cannot answer which text version, which
> purpose, which parties, captured how, revoked when — and those are exactly the questions asked
> when something goes wrong.

> **`CAP-105` — the distinction that makes it shared.** *Insurer catalogue ≠ bank catalogue.* An
> insurer may have 100 products; the bank may approve 8 (`VIN-001 §11`). The bank-approved offering
> is a governed, versioned, effective-dated bank asset. LOB cells **consume** it; they must never
> each maintain their own insurer master, or the eight becomes three different eights.

### Plane 2 — LOB cells *(instantiated per LOB: Life H0 · Health H2 · General/Motor H3)*

| ID | Capability | Owns | Does **not** own | Sharing | R0 |
|---|---|---|---|---|---|
| `CAP-201` | Journey Execution (LOB) | LOB stage machine, transitions, LOB business rules, resumption | Journey identity/routing (`CAP-106`); other contexts' decisions | LOB_SPECIFIC | part of #9 |
| `CAP-202` | Suitability & Eligibility (LOB) | LOB need-analysis model, questionnaire, rules, assessment validity | Insurer underwriting; product approval (`CAP-105`); the *framework* for questionnaires (shared) | LOB_SPECIFIC rules on a SHARED framework | #7 |
| `CAP-203` | Quotation (LOB) | Quote request; eligible product selection; multi-quote orchestration; **canonical** quote representation; expiry; status and comparison | Raw insurer payloads and transformation (`CAP-402`); product approval (`CAP-105`) | LOB_SPECIFIC | #10 |
| `CAP-204` | Proposal / Case Management (LOB) | Application data, nominees, insured persons, declarations, insurer questionnaires, submission, insurer references, **outstanding insurer requirements** | The risk decision and everything in `TI-03`; document storage mechanics (`CAP-302`) | LOB_SPECIFIC | #11 |
| `CAP-205` | Provider Integration (LOB runtime) | Provider protocol, payload transformation, provider credentials, provider-specific issuance interaction | Business decisions; canonical contracts (shared, `CAP-402`) | SHARED_FRAMEWORK_ISOLATED_RUNTIME | #14/#15 |

> **Why Suitability is LOB-sensitive (`VIN-001 §10`).** Life asks about income, dependents, cover
> amount, tenure, objectives. Health asks about members, conditions, sum insured, geography. Motor
> asks vehicle eligibility rather than Life-style suitability at all. The *framework* — questionnaire
> engine, rule pack versioning, assessment validity, evidence capture — is shared; the *rules and
> models* belong to the LOB. And all of it is distributor suitability, which is **not** insurer
> underwriting.

> **Why issuance is not a shared Policy Service (`VIN-001 §19`).** Provider-specific issuance
> interaction stays in the LOB's provider integration. On success it emits the canonical
> **`PolicyIssued`** event, which `CAP-303` consumes. Putting insurer issuance protocols into a
> shared policy service would rebuild, in the most sensitive place in the platform, exactly the
> provider coupling `TI-02` exists to prevent.

### Plane 3 — Shared Transaction & Portfolio

| ID | Capability | Owns | Does **not** own | Sharing | Horizon | R0 |
|---|---|---|---|---|---|---|
| `CAP-301` | Payment | Payment session, link, gateway integration, callback, status, reconciliation, uncertain-payment resolution, refund reference | **When** payment is due and what context applies — the LOB decides | SHARED | H0 | #12 |
| `CAP-302` | Documents | Storage, metadata, access control, scanning, retention, versioning | **Which** document is required — the LOB domain decides | SHARED | H1 | partial (S3) |
| `CAP-303` | Policy Portfolio / Registry | Canonical bank-side policy view: customer, insurer, LOB, product, policy number, premium, coverage, dates, status, document ref, renewal date | Insurer-specific issuance protocols; the policy contract itself | SHARED | H1 | part of #13 |
| `CAP-304` | Notification | Delivery over SMS/email/push/WhatsApp; templates; delivery log | Whether and when to engage (`CAP-306`); business state | SHARED | H0 | #17 |
| `CAP-305` | Audit & Evidence | Append-only business-event evidence, WORM retention, queryable reconstruction | Technical logs (`CAP-504`); compliance verdicts (Shailja) | SHARED | H0 | #16 |
| `CAP-306` | Engagement / Recovery | Whether and when to engage; cadence rules (30 min → push · 6 h → WhatsApp · 24 h → call-centre work item) | Message delivery (`CAP-304`); work item execution (`CAP-103`) | SHARED | H1 | none |
| `CAP-307` | Interaction Timeline | Single chronology per customer/journey: started, quote viewed, RM assisted, call centre contacted, SP joined, proposal submitted, paid | Being the source of truth for any of those events — it is a read model | SHARED | H1 | none |

> **`CAP-301` — the resilience consequence.** Because every LOB depends on Payment, it is the
> capability where a shared dependency becomes a shared *outage*. It carries the highest
> availability target in the platform and no degraded mode on the money path
> (`03-solution-architecture-r0.md §5.3`).

> **`CAP-305` vs `CAP-504` — audit is not logging (`VIN-001 §29`).** Logs answer *what happened
> technically*. Audit answers *who did what business action, when, in what context, with what
> evidence*. They have different retention, different tamper models, different consumers and
> different regulators. **Preserve the R0 audit model.**

### Plane 4 — External Integration Boundaries

| ID | Capability | Owns | Does **not** own | Sharing | Horizon | R0 |
|---|---|---|---|---|---|---|
| `CAP-401` | Bank Integration | CBS/customer lookup, account verification, RM/branch context, other bank-system protocols and credentials | Any insurance business decision; provider protocols | SHARED | H0 | inside #4 today |
| `CAP-402` | Provider Integration Framework | Canonical provider contracts (`IF-1`), authentication framework, credential handling, timeout/retry/breaker policy, error model, idempotency, certificate handling, observability contract | Provider-specific payloads at runtime — that is `CAP-205` per cell | SHARED framework | H0 | #14 Hub + #15 adapter |

> **Why the two are separate (`VIN-001 §16`).** `Insurance Platform → Bank Integration → CBS` and
> `LOB Cell → Provider Integration → 1SB/Insurer` are different problems with different change
> rates, different credentials and different owners. Merging them means a CBS upgrade lands in Life
> Quote code. **`CAP-401` does not exist as a separate unit in R0** — CBS access sits inside `#4
> Customer`. That is acceptable at H0 with one consumer; it becomes debt the moment a second
> capability needs bank data, which is the recorded revisit trigger.

### Plane 5 — Platform Engineering & Governance

| ID | Capability | Owns | Sharing | R0 |
|---|---|---|---|---|
| `CAP-501` | Identity (workforce · customer · partner · service) | Authentication brokering, session custody, identity lifecycle | SHARED | #3 / WS-2 |
| `CAP-502` | Authorization (PDP) | Default-deny RBAC + ABAC + relationship + **certification** decisions | SHARED | WS-2 PDP |
| `CAP-503` | Configuration | Versioned, effective-dated, maker-checker, auditable, reversible configuration | SHARED | #19 (config artefacts at R0) |
| `CAP-504` | Observability | Correlation and trace propagation, metrics, logs, dashboards, SLOs — technical **and** business | SHARED | `bank-common-observability` |
| `CAP-505` | Event Distribution | Durable domain-event delivery — transactional outbox now, broker when justified | SHARED | outbox (S-17) |
| `CAP-506` | Operational Plane | CI/CD, IaC, secrets/KMS, backup/restore, DR, security scanning, vulnerability management, runbooks | SHARED | S08/S09 |

> **`CAP-506` is architecture, not "supporting detail" (`VIN-001 §35`).** For a bank platform the
> operational plane is part of the target architecture. This is the same finding S08/S09 already
> record; `VIN-001` states it independently, which strengthens rather than duplicates it.

> **`CAP-503` will be one of the largest capabilities (`VIN-001 §33`).** Insurance distribution is
> configuration-heavy: insurers, products, channels, branches, eligibility, provider routes, feature
> activation, dates, product versions — each needing versioning, maker/checker where required,
> audit, effective dates and rollback. Underestimating it is how hard-coded product and provider
> behaviour becomes the platform's bottleneck.

---

## 8. Using the model in a review

When Mahesh receives a proposal, the model gives him five fast, high-yield questions:

1. **Which capability owns this?** If the answer is "two", the boundary is wrong or the capabilities
   have not been separated.
2. **Does this ask a capability to own something its contract says it does not?** That is the most
   common `A1` finding, and the cheapest to catch here.
3. **Which plane does it sit in, and does the dependency direction make sense for that plane?**
4. **Is this a capability change or a deployment change?** They have different authority classes and
   different evidence bars (`NS-03`, `NS-04`).
5. **If this shipped, could a new LOB still be added without touching it?** If not, LOB variation has
   leaked into a shared capability (`VR-02`).

---

## 9. Reconciliation of `VIN-001` against accepted repository decisions

Recorded per `09 §10` rules `VI-01`/`VI-02`. **Agrees** = already repository position ·
**Extends** = new, non-conflicting · **Refines** = changes how an accepted decision is expressed ·
**Conflicts** = requires change control.

| `VIN-001` | Claim | Repository position | Verdict |
|---|---|---|---|
| §1 | Bank-owned, insurer-agnostic distribution platform; Life → Health → General | `BG-001`, `BG-004`, `02-vision-goals-principles.md` | **Agrees** |
| §1 | Five assistance modes incl. call centre and certified SP | R0 assisted-first; DIY at R1, hybrid at R2 (`DEC-20260816-03`) | **Extends** — two new actor types, H1 |
| §1 | Start in one channel, continue in another | Not previously stated | **Extends** → `TI-12`; drives `CAP-106` |
| §1 | AWS, workloads private behind controlled public entry | `ARCH-002`, R0 deployment properties | **Agrees** |
| §2 | Capability ≠ microservice; ownership before deployment | `01 §4 AP-01`, `README §10.2`, capability map PO note | **Agrees** — now made operational as `NS-03`/`NS-04` |
| §3 | Party vs Bank Customer | `#4 Customer` = CIF-linked snapshot (ETB only) | **Extends** — H1; **live H0 constraint** (`§6.1`) |
| §4–5 | Opportunity, and Work Management as a separate capability | `#5 Lead` deferred; no work-management context exists | **Extends** — two capabilities, H1 |
| §6–7 | Journey survives channel change; Registry vs LOB Execution | `#9` owns the cross-domain state machine; `SC-W3-6` limits it to stage+refs | **Refines** — `SC-W3-6` preserved and strengthened; split is H2 |
| §8–9 | LOB as an isolation cell containing several capabilities | Single-LOB R0; no cell concept recorded | **Extends** → file `11`; entry gated by `DEC-20260816-05` |
| §10 | Suitability LOB-specific rules on a shared framework | `#7 Suitability`, `SUITABILITY-PACK-v1.0` | **Refines** — pack is Life-shaped; framework/rules split made explicit |
| §11 | Insurer catalogue ≠ bank catalogue; shared Product Governance | `#8 Product Catalogue`, `ARCH-010` | **Refines** — sharpens what it owns |
| §12–13 | Quote and Proposal are strong LOB boundaries | `#10`, `#11` exist per-context, single-LOB | **Agrees**, becomes per-cell at H2 |
| §14 | Bank does not underwrite; call it Proposal/Case Management | Capability map: *"track … not underwrite"* | **Agrees** → promoted to `TI-03` |
| §15 | Consent is evidence, not a Boolean; shared | `#6 Consent` append-only, `CONSENT-PACK-v1.0` | **Agrees** |
| §16 | Separate bank integration from provider integration | CBS access sits inside `#4` today | **Extends** → `CAP-401`, with revisit trigger |
| §17 | Shared integration framework, isolated per-LOB runtime | `SC-W3-5` — all provider traffic through the Integration Hub | **Refines / topology conflict** — reconciled at [`09 §5.1`](./09-target-state-architecture-doctrine.md); ADR required before any split |
| §18 | Payment shared and highly resilient | `#12 Payment`, `C4`, `INV-PAY-*` | **Agrees** |
| §19 | Issuance stays in LOB/provider integration; emit `PolicyIssued` | `#13 Policy & Issuance` is one context | **Refines** — split issuance interaction from portfolio |
| §20 | Shared Policy Portfolio/Registry | Not separately recorded | **Extends** → `CAP-303`, H1 |
| §21 | Renewal/lapse are new opportunities, not reopened journeys | Not previously stated | **Extends** → `TI-13` |
| §22 | Abandonment recovery via Engagement + Work Management, same journey | Not previously stated | **Extends** → `CAP-306`, `TI-12` |
| §23–24 | Call centre / certified SP are actors; certification-aware authorization | `ARCH-020` RBAC+ABAC+relationship; `ARCH-022` RM certification from AD | **Agrees / extends** — certification already anticipated; actor-type breadth is new |
| §25 | Documents shared; requirement decisions domain-owned | S3 used for raw payloads and policy docs; no document capability | **Extends** → `CAP-302` |
| §26–27 | Notification shared; Engagement separate from Notification | `#17 Notification` only | **Agrees / extends** → `CAP-306` |
| §28 | Interaction Timeline | Not recorded | **Extends** → `CAP-307`, read model |
| §29 | Audit ≠ logging; preserve the R0 audit model | `#16`, `INV-AUD-01`, `FF-10` | **Agrees** — explicitly endorses the existing design |
| §30 | Events where sync coupling is unnecessary; **outbox first**, Kafka when justified | `03-solution-architecture-r0.md §5.1` — identical position with recorded revisit triggers | **Agrees** — independent confirmation |
| §31 | Observability spans technical and business metrics; per-LOB dashboards and SLOs | `bank-common-observability`, `05-nfr-catalogue.md` | **Extends** — business-metric correlation and per-LOB SLOs |
| §32 | Failure isolation at LOB **and** provider level, demonstrable | Per-provider bulkheads are an architecture property | **Extends** → `TI-18`; adds the LOB dimension |
| §33 | Configuration is a major capability with maker-checker and effective dates | `ARCH-010`; `#19` deferred, config-as-artefact at R0 | **Agrees / extends** |
| §34 | Ownership non-negotiable; physical DB separation pragmatic | `ARCH-004` database-per-service (`Proposed`) | **Refines / qualifies** — reconciled at [`09 §5.2`](./09-target-state-architecture-doctrine.md); ADR update with Aarti |
| §35 | Operational plane is part of target architecture | S08/S09, `GATE-S08` | **Agrees** |
| §36 | Release follows business maturity; Health only after the shared platform is proven | `DEC-20260816-05` freeze on two gates | **Agrees** — same conclusion, independently reached |

### 9.1 Open items this reconciliation creates

| # | Item | Owner | Type |
|---|---|---|---|
| 1 | `ARCH-004` update: separate ownership invariant from physical topology decision | Mahesh + **Aarti** | ADR update |
| 2 | `SC-W3-5` wording: *"the Integration Hub"* → *"an integration boundary"*, with the split trigger | Mahesh + Deepali + Shivanshi | ADR, before any per-LOB runtime |
| 3 | H0 constraint: no contract may assume `customerId == cifId`, or Party at H1 becomes a breaking change | Mahesh | Design constraint, verify at S11 |
| 4 | Coarse shared stage vocabulary must be provably LOB-agnostic before Health | Mahesh + Rajal | H2 entry condition |
| 5 | Call centre and certified SP as supported actor types | **Rajal** (Product scope) + Shailja (regulated activity) | Product decision — not Mahesh's |
| 6 | Certification-gated regulated sales activity: which actions require which certification | **Shailja** | Compliance obligation |
| 7 | Party de-duplication/merge policy across DIY and ETB | Rajal + Shailja | H1 entry condition |

**None of these is decided by this document.** They are named so they cannot be assumed closed.

---

## 10. Standing of this model

`AI-DRAFTED` grounding context authored in Mahesh's Board 1 lane, derived from the cited repository
sources and from `VIN-001`. It is **not** a ratified architecture baseline: nothing here satisfies
AIGEM T4 human Architecture sign-off, and no capability described at H1–H3 is approved scope.
Precedence per `08 §5`.
