# R0 High-Level Design — AU Bank Insurance Distribution Platform

**Workstream:** WS-3 (primary) · WS-1 (supplier, IF-1) · WS-2 (enabler, IF-2)
**Horizon:** H0 — R0 as designed
**Owner:** Mahesh — Principal Insurance Platform Architect (Board 1)
**Audience:** Product, Architecture, Engineering, SRE, Security, Database, Compliance, Delivery, CTO
**Status:** `AI-DRAFTED` in the Architecture lane. **Mandatory human T4 Architecture sign-off outstanding.** Deepali (Security), Aarti (Database) and Shivanshi (SRE) reviews are required before this pack is cited as S09 input.
**Date:** 2026-08-20 · **revised** 2026-08-24
**Origin:** `SUG-20260820-hl1` · **revision** `SUG-20260824-gp1` … `gp5` ([`CR-012`](../governance/change-requests/CR-012-r0-platform-robustness.md))

> **Revision 2026-08-24 — R0 robustness round.** Five deferred infrastructure layers move into R0
> under `ADR-009` … `ADR-013`. In this document the changes are: boundary 6 and 7 (the bank path is
> provisioned, not deferred), boundary 8 (three new tiers), **boundary 9 rewritten** (a broker
> exists, and the transactional outbox stays in front of it as the source of truth), seams `S-23` …
> `S-26`, and §4.3 (idempotency stays out of the cache, deliberately). Nothing about the journey,
> the actors, the gates or the waves changed.

**Picture this document walks:** [`r0-reference-architecture.svg`](./r0-reference-architecture.svg)

---

## 0. How to read this document

This is the **stakeholder HLD**: one narrative of the complete R0 slice — domain, ten boundaries, communication, APIs, business logic, and the order of work.

It is **not** a new source of truth. Rule `HA-02` still holds: if this file and an authoritative source disagree, the source wins. The SVG is a rendering of the same picture; it owns nothing.

| Need | Authoritative source |
|---|---|
| R0 service set, build waves, seams, fitness functions, DR posture | [`../platform/ws3-platform/03-solution-architecture-r0.md`](../platform/ws3-platform/03-solution-architecture-r0.md) |
| Actors, aggregates, state machines, invariants, saga | [`../platform/ws3-platform/01-domain-model-and-invariants.md`](../platform/ws3-platform/01-domain-model-and-invariants.md) |
| Attributes, classification, retention, system of record | [`../platform/ws3-platform/02-information-model.md`](../platform/ws3-platform/02-information-model.md) |
| Standing constraints `SC-W3-*`, interfaces IF-1/IF-2/IF-3 | [`../platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md`](../platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md) |
| Trust boundaries, payment-device isolation | [`../platform/ws3-platform/04-security-architecture.md`](../platform/ws3-platform/04-security-architecture.md) |
| Numbers (latency, RTO/RPO, retention) | [`../platform/ws3-platform/05-nfr-catalogue.md`](../platform/ws3-platform/05-nfr-catalogue.md) |
| Admitted scope, gate, out-of-scope | [`../governance/state/CURRENT-STATE.yaml`](../governance/state/CURRENT-STATE.yaml) |
| AWS resource mapping for the platform team | [`R0-LLD.md`](./R0-LLD.md) — companion, this same change |

**What this file uniquely does:** it walks the SVG left-to-right and top-to-bottom, states the APIs a caller actually hits, and answers *what to do when* — lifecycle stage, build wave, and release — in one place.

---

## 1. The R0 outcome, in one screen

> One Relationship Manager — the certified Specified Person — sells one Term Life policy to one existing-to-bank customer of one Group A insurer, end to end, through a real interface, with consent and suitability evidence, payment on the customer's own device, an issued and reconciled policy, and a complete audit trail.

That sentence is the whole of `R0-ASSISTED-TERM-SALE`. Everything in this HLD either makes that sentence true or is deferred until it is.

The journey ribbon on the SVG is the acceptance path, not a decoration:

| Step | What must happen | Context that owns it | Hard gate |
|---|---|---|---|
| 1 | Lead created; ETB customer looked up from CBS | #5 Lead · #4 Customer | Origination is RM-only (`AC-8`) |
| 2 | Need analysis and suitability complete | #7 Suitability | **C1** — no quote without a valid, unexpired assessment |
| 3 | Consent granted via customer-device OTP | #6 Consent | **C2** — no proposal without an unexpired grant |
| 4 | Term quote via 1SB, partial success is success | #10 Quotation · #14 Hub · #15 Adapter | C1 re-checked at quote entry (`S-08`) |
| 5 | Proposal submitted; UW tracked | #11 Proposal & UW | Consent grant still valid |
| 6 | Payment link issued to the **customer device** | #12 Payment · #17 Notification | **C4** — never an RM or bank device |
| 7 | Customer pays; bank reconciles | #12 Payment · AU Bank PG | `UNCERTAIN` until settlement; never guessed |
| 8 | Policy issued only against a **RECONCILED** payment | #13 Policy & Issuance | `SC-W3-4` |
| 9 | Audit evidence complete | #16 Audit | Journey cannot reach `SOLD` until audit confirms (`INV-JRN-05`) |

`#9 Journey Orchestration` holds **stage and references only**. It never copies another context's business decision (`SC-W3-6`).

---

## 2. Domain

### 2.1 Two actors, one of them sells

R0 has **two on-platform human actors**. The customer is a participant, not an actor: their device receives an OTP and a payment link and touches no platform service.

| Actor | Identity plane | What they may do | What they never do |
|---|---|---|---|
| **Bank RM** | Workforce, Bank AD federated | Sole origination. Every regulated action. Accountable Specified Person on the record for its life | Never anonymous; never replaceable as the accountable SP |
| **Insurance Partner Representative (IPR)** | Partner, maker-checker provisioned | Assist only: own-insurer product view/selection, gated journey read, annotations | Never an SP. No origination, no advice, no regulated action |
| *Customer* | — | OTP + pay on **their** device | Never a session on the platform in R0 |

**Specified Person is a certification attribute on the RM principal**, not an actor and not a channel (`AC-1`, `ADR-004`). It is evaluated **at the instant of each regulated action**, not at login. Expired, suspended or out-of-LOB-scope certification fails closed.

The actor-type vocabulary is closed at three values: `BANK_RM` · `INSURER_PARTNER_REP` · `SERVICE`. Adding an actor type is an authorization change, not a new service (`AC-3`).

IPR visibility is a **persistence-layer predicate**, not a hidden button (`AC-4`, `AC-5`):

```text
visible_to_IPR(record) :=
        record.leadCreatedBy.actorType = BANK_RM
    AND record.needAnalysisState        = COMPLETED
    AND record.suitabilityState        IN (COMPLETED, OVERRIDDEN)
    AND record.insurerId                = principal.insurerId
    AND principal.actorType             = INSURER_PARTNER_REP
```

Records outside that predicate are **absent from the result set**, never a `403` on a named id (`S-22`, `INV-LED-07`). A refusal that names an id confirms the id.

### 2.2 The Lead is the only on-platform way in

Context **#5 Lead** (identifiers still `leadId` / `INV-LED-*` / `CAP-102`) is the single on-platform origination point (`ADR-005`, `ADR-014`). Opportunity is the durable-demand alias only. Every on-platform downstream aggregate carries `leadId`. There is no BFF-created journey, no quote without a Lead, no second funnel. Off-platform / portal sales enter as Policy ingest (`source=OFF_PLATFORM`) and never create a Lead.

Created **only** by a `BANK_RM` with a valid SP certificate covering the Lead's `lob`. An IPR attempting `lead.create` receives `403 ORIGINATION_RM_ONLY` on every entry point (`FF-16`). After convert + `Payment.RECONCILED` + `Policy.ACTIVE`, the working inbox archives.

### 2.3 Line of business is first-class from release 1

R0 sells `lob = LIFE`, `productClass = TERM`. Health and General follow on the same template, so the dimension is carried now (`ADR-006`):

| Rule | Statement |
|---|---|
| `LB-1` | `lob` is mandatory and non-null on every entity, configuration record, audit event and authorization request |
| `LB-2` | Vocabulary frozen: `LIFE` \| `HEALTH` \| `GENERAL`. R0 populates `LIFE` only |
| `LB-3` | `lob` and `productClass` are different. Recording `lob = TERM` is a defect |
| `LB-4` | Quote and Proposal are **LOB-owned execution** — Health gets its **own** `#10` and `#11`, never `if (lob == …)` |
| `LB-5` | Party, lead, consent evidence, payment, policy registry and audit stay **shared**. Partitioning the *rules* makes Health possible; duplicating the *evidence* makes it unauditable |

On the SVG: rose cell = LOB-owned; `LIFE` square tag = shared code, configuration keyed by `(lob, …)`; no tag = shared forever.

### 2.4 Configuration layer ships in W0b; the admin UI is R0 W4

Context **#19** is a Wave 0b service (`ADR-007`). Rules, journey steps, field validations, document checklists, product eligibility, role→permission grants, provider routing and attribution are resolved from a versioned, append-only, effective-dated, LOB-partitioned store. **No compiled-in fallback** (`CF-1`, `S-21`): an unresolvable rule is a refusal.

The administration UI is **in R0 W4** (`ADR-014`). It is a consumer of the layer, not a re-platforming, and it **must not** sit on the Lead writer (`C-ISO-1`). A missing or late screen still does not justify hardcoding (`CF-5`).

### 2.5 Aggregates — one transaction, one root

| Aggregate | Context | What it is allowed to decide | What it must not hold |
|---|---|---|---|
| `Lead` (opportunity) | #5 | Why we are contacting this person; accountable SP; `lob` | Customer master, journeys |
| `CustomerProfile` | #4 | ETB snapshot used by this journey | CBS itself |
| `Consent` | #6 | Grant + immutable evidence | Downstream copies of the evidence |
| `SuitabilityAssessment` | #7 | Eligible / not, recommended set | Product definitions |
| `Quote` + `Offer[]` | #10 | Price discovery and selection (atomic) | Adapter job ids as identity |
| `Proposal` + `UnderwritingCase` | #11 | Application capture, UW tracking | Payment or policy |
| `Payment` | #12 | Money movement and reconciliation | Card data; RM on the pay path |
| `Policy` | #13 | Issued-policy record and documents | Payment state |
| `Journey` | #9 | Stage + references + party snapshot | Any other context's decision |
| `AuditEvent` | #16 | One append-only evidence row | Updates or deletes |

Cross-context references are by opaque bank-minted id (`ULID`). Provider identifiers live in `externalRefs` and are never the caller's handle.

### 2.6 The journey saga — the one record of where a sale is

Orchestrated, not choreographed. `#9` owns it so a mis-selling review can ask *where did it stop and why* without reconstructing ten logs.

Happy path:

```text
INITIATED
  → NEED_ANALYSIS
  → SUITABILITY_COMPLETE     (C1)
  → CONSENT_CAPTURED         (C2)
  → QUOTING
  → QUOTE_SELECTED
  → PROPOSAL_IN_PROGRESS
  → UNDERWRITING
  → PAYMENT_PENDING          (C4 — link to customer device)
  → PAYMENT_SETTLED          (RECONCILED, not merely captured)
  → ISSUANCE_PENDING
  → ISSUED
  → SOLD                     (policy ACTIVE + payment RECONCILED
                              + issuance confirmed + audit complete)
```

The three failure classes that must never be resolved by a timeout:

| Id | Failure | What the platform does |
|---|---|---|
| F-05 | Paid, not issued | `COMPENSATING` → bounded issuance retry → maker-checked refund. Never auto-refund above threshold |
| F-07 | Reconciliation break | Stay out of `SOLD`. Manual finance procedure. No timeout auto-resolves money |
| F-08 | PG response lost | `UNCERTAIN`. Block a new attempt (`INV-PAY-04`). Next settlement file decides |

A sale is **never** inferred from quote, proposal or payment alone (standing constraint).

---

## 3. Ten boundaries

The SVG and the North Star (`docs/hdl.svg`) use the same bands (`LY-1`). A thin band in R0 is still drawn, with what arrives later greyed and carrying its release. That is how you tell *not built yet* from *does not exist*.

### Boundary 1 — Channels and actors

**Owns:** every human or system that starts or continues a journey.
**R0 contains:** Flutter RM app (token-hiding session — OAuth tokens never reach the device, `S-01`); IPR surface on the **same** gateway and BFF (`S-22`); customer device for OTP and payment only.
**Greyed:** Customer DIY (`#1` BFF, R1); call centre / branch / hybrid (R2).

### Boundary 2 — Edge (the only public entry point)

**Owns:** TLS termination, WAF, throttling, request validation.
**Does not own:** business logic, authorization decisions.
**R0 contains:** Route 53 · CloudFront · AWS WAF · API Gateway · internal ALB.
**Greyed:** insurer callback ingress (R1). R0 **polls** providers instead (`S-11`).
**Rule:** no workload, database or cache is internet-reachable.

### Boundary 3 — Experience / BFF

**Owns:** channel-shaped aggregation, session, token custody.
**R0 contains:** `#2` RM Workspace BFF and the Admin & Configuration BFF (UI over `#19` + MIS). One BFF per **channel**, never per channel × LOB. Field validation rules resolved by `(lob, formId)`. Admin/MIS never use the Lead writer (`C-ISO-1`).
**Greyed:** `#1` Customer BFF (R1); Operations / call-centre BFF (R2).

Flutter never calls a domain service or a database. The BFF holds OAuth tokens; the device sees an opaque session.

### Boundary 4 — Shared platform (built once for every LOB)

**Owns:** who the customer is, why we are contacting them, what we are permitted to do, where the journey is, what was paid, what is held.
**Does not own:** how a product is quoted, proposed or underwritten — that is boundary 5.

| Context | Wave | LOB class | R0 job |
|---|---|---|---|
| #3 Identity & Access (workforce half, WS-2) | enabler | PDP grants are LOB-partitioned | Adapter (Keycloak first) + PDP, default-deny, fail closed 300 ms no retry (`S-02`) |
| #9 Journey Orchestration | W1 | partitioned | State machine; compensation tasks (`S-19`) |
| #5 Lead | W1 | agnostic | Single origination; RM-only create; archive after sold |
| #4 Customer | W1 | agnostic | CBS/CIF lookup and prefill (`S-04`/`S-05`) |
| #6 Consent | W2 | partitioned | Append-only grants; customer-device OTP |
| #7 Suitability | W2 | partitioned | Hard gate; framework shared, **rules** are LOB |
| #8 Product Catalogue | W1 | partitioned | R0 matrix: Life · Group A · Term |
| #12 Payment | W3 | agnostic | Link to customer device; reconcile before issue |
| #13 Policy & Issuance | W3 | agnostic | Issues only against `RECONCILED` |
| #16 Audit & Compliance | W3 | agnostic | Append-only; UPDATE/DELETE refused (`FF-10`); 7-year WORM |
| #17 Notification | W4 | agnostic | OTP + payment-link only; failure never blocks the journey (`S-18`) |
| #19 Configuration | W0b | partitioned | See §2.4 |
| #18 Reporting & MIS | W4 | agnostic | Funnel, sold, on- vs off-platform. Isolated read path (`C-ISO-1`) |

### Boundary 5 — LOB execution cells

**Owns:** the per-LOB execution that cannot be shared. A Life quote and a Health quote do not share a field shape.
**R0 populates LIFE only:** `#10` Quotation (Life) and `#11` Proposal & UW (Life).
**Greyed:** HEALTH cell (R3, gated on WS-1 Phase 5 unfrozen); GENERAL cell (R4).
**Does not own:** the underwriting **decision** — that is the insurer's.

### Boundary 6 — Aggregation and provider connectivity

**Owns:** bank-canonical request in, provider protocol out.
**R0 contains:** `#14` Integration Hub (all provider traffic, `SC-W3-5`) and `#15` 1SB Adapter (exists today, `adapter.onesb.*` only).
**Greyed:** provider router and callback gateway (R1).
**Rule:** no WS-3 service calls an adapter directly. `distributorId` is injected server-side; a caller-supplied value is rejected (`INV-DIS-01`).
**Egress path (`ADR-010`):** provider traffic leaves through the centralised inspection VPC — Transit Gateway, then AWS Network Firewall, then the NAT gateways whose Elastic IPs 1SB allowlists. The mTLS session to 1SB is **not** decrypted; it is matched on destination and passed intact.

### Boundary 7 — External systems

Two boundaries, kept separate on purpose:

| Bank systems | Insurance providers |
|---|---|
| Bank AD / SSO (WS-2 Phase 2 federation; adapter exists first) | 1SilverBullet — Group A, Term only, reached only through `#14` → `#15` |
| Core Banking (CBS) — CIF, ETB prefill | *(direct insurer APIs are R1+)* |
| AU Bank Payment Gateway — 3-D Secure on the customer device | |

**R0 provisions the path to the bank systems rather than deferring it (`ADR-009`).** A Transit
Gateway hub carries CBS and Bank AD traffic over Site-to-Site VPN from day one, with Direct Connect
becoming the primary path when the circuit is accepted and the VPN kept as the standby. `dev` may
use CBS and AD stubs; **`uat` and `prod` may not** — a journey evidenced against a stub is not
evidence, and that is the whole reason this layer moved into R0.

### Boundary 8 — Data and persistence

**Invariant:** one owner per authoritative datum; no service reads another's tables; per-context credential with **no** cross-schema grant.
**Decision (`ADR-008`):** R0 runs **one Aurora PostgreSQL cluster with a schema per bounded context**. A physical cluster per service is not a principle. The first physical split follows the **LOB-cell / shared-platform seam**, not the service boundary.

Also in R0: DynamoDB (journey state, quote jobs, audit event store); S3 + Object Lock (raw payloads, policy docs, 7-year WORM, CRR to `ap-south-2`); Secrets Manager + KMS (India regions only).

**Three tiers added on 2026-08-24, each with a boundary that is part of the decision:**

| Tier | R0 use | What it is never allowed to be |
|---|---|---|
| **ElastiCache for Valkey** (`ADR-011`) | BFF session vault; L2 read-through cache for configuration and catalogue behind the existing in-process L1; per-principal rate-limit counters | A system of record. The idempotency store — that stays transactional with the business write (§4.3). A way to serve configuration past its TTL when the store is down |
| **Amazon MSK** (`ADR-012`) | Domain-event transport and fan-out, fed by the outbox | The audit record. Retention on a topic is an operational parameter; retention on evidence is a licence condition |
| **Amazon OpenSearch** (`ADR-013`) | Operational search over application, firewall, flow and broker logs | The evidence store, or a business search index for catalogue and journey queries |

The existing `bank-persistence-service` stays scoped to the integration job store and audit ingestion. It is **not** extended to the R0 business contexts.

Aarti's Database approval and Deepali's Security review of this topology are **outstanding**, and the 2026-08-24 round widened what they cover: one cluster is still one blast radius, and there are now two more stateful tiers plus a session vault whose read access is read access to live sessions.

### Boundary 9 — Event and messaging

**R0 decision, revised 2026-08-24 (`ADR-012`): there is a broker, and the transactional outbox stays in front of it.** These are not alternatives and the order is the decision.

```text
business txn ─┬─► business tables      one local transaction
              └─► outbox row           ← the source of truth, and the replay log
                      │
              outbox-publisher ──► MSK topic ─┬─► #16 Audit ─► DynamoDB + S3 WORM ← the record
                                              ├─► #17 Notification
                                              └─► compensation / reconciliation signals
```

Why both. The outbox solves the dual-write problem — a commit followed by a publish is two writes with no shared transaction, so a crash loses the event and a retry duplicates the business change. The broker solves fan-out — without it, every new consumer is a new poller against the producer's own database. R0 has three consumer classes already (audit, notification, compensation) and a fourth arrives with Reporting at S13, so the old revisit trigger was going to fire inside R0 rather than after it. Adopting the broker mid-slice, in the middle of evidencing the audit path, was the worst of the three available moments.

**The rule that cannot be traded:** no regulatory evidence exists only in a topic. `#16` Audit consumes the topic and writes DynamoDB and the WORM archive; that write is what lets a journey reach `SOLD` (`INV-JRN-05`), and `FF-26` checks it. Consumers are idempotent on `eventId` and tolerate replay — which is also why the broker needs no DR copy (LLD `D14`).

Must be synchronous: authorization, configuration resolution, critical validation, payment session.
May be asynchronous: audit ingestion, notification, quote/proposal polling.

### Boundary 10 — Platform engineering (IF-3)

This is where the programme **is today**. Waves W0b–W4 start only after **GATE-S08** passes and the S09 critical path lands.

| Stage | What it proves | Gate |
|---|---|---|
| **S08 — Engineering Foundation** | CI, coverage, ArchUnit + FF-01…FF-28, secret/SAST/SCA/image scan, no PII in logs, p95 pipeline < 10 min | `GATE-S08` OPEN |
| **S09 — Platform & Environment Foundation** | IaC, environments, secrets, observability, 7-year WORM in `ap-south-1`, restore **executed and timed**, DR warm standby `ap-south-2` | overlapped with S08 |

---

## 4. Communication

### 4.1 The rule

A human is waiting in session → **synchronous**, with a named timeout and a named failure.
A side effect of a completed step (audit, notification, compensation) → **asynchronous via outbox**, at-least-once, never silently dropped.

Quote and proposal **submit** are async-poll with **no automatic retry on submit** — re-submitting a possibly-processed request is the failure mode the rule exists to prevent. Recover by polling the external reference.

### 4.2 Seam catalogue (S-01 … S-22)

Full semantics (idempotency, timeout, on-failure) live in [`03-solution-architecture-r0.md §5`](../platform/ws3-platform/03-solution-architecture-r0.md#5-seam-catalogue--synchronous-vs-asynchronous-with-semantics). Summary for this HLD:

| Seam | From → to | Style | Failure posture |
|---|---|---|---|
| S-01 | Flutter → BFF | Sync | Typed error; journey stage unchanged |
| S-02 | BFF → PDP | Sync, 300 ms, no retry | **Fail closed — deny** |
| S-03 | BFF → Journey | Sync | No partial stage advance |
| S-20 | BFF → Lead | Sync | `403 ORIGINATION_RM_ONLY` for non-RM |
| S-04 / S-05 | Journey → Customer → CBS | Sync | Hold at `INITIATED`; no unbounded stale identity |
| S-06 | Journey → Consent | Sync | Fail closed; cannot advance |
| S-07 / S-08 | Journey / Quotation → Suitability | Sync | **Fail closed** — `403 SUITABILITY_REQUIRED` |
| S-09 / S-10 / S-11 | Quotation → Hub → 1SB; poll | Async-poll | `PARTIAL` is success; budget exhausted → `TIMED_OUT` |
| S-12 | Proposal → Hub | Async-poll, no auto-retry on submit | `SUBMISSION_FAILED` + ops task |
| S-13 | Payment → AU Bank PG | Sync | `REJECTED`; proposal stays `AWAITING_PAYMENT` |
| S-14 / S-15 | PG → Payment (callback + settlement) | Callback + batch | Missing callback → `UNCERTAIN`; unmatched → `RECONCILIATION_BREAK` |
| S-16 | Policy → Hub | Sync + re-check | `ISSUANCE_DISPUTED` |
| S-17 | any → Audit | Outbox | Business commits; `SOLD` blocked until ack |
| S-18 | Payment/Policy → Notification | Outbox | Never blocks the journey |
| S-19 | Journey → compensation | Durable task | Exhaustion → `MANUAL_INTERVENTION` |
| S-21 | any → Configuration | Sync, cached to TTL (L1 → L2) | **Fail closed** — no compiled-in default |
| S-22 | IPR → BFF → gated read | Sync | Out-of-scope rows absent, not 403-by-id |
| S-23 | outbox-publisher → MSK | Async publish, at-least-once | Outbox row stays unacknowledged; nothing is lost, delivery is delayed |
| S-24 | MSK → `#16` Audit / `#17` Notification | Async consume, replay-tolerant | Dedupe on `eventId`; DLQ after bounded attempts; `SOLD` still blocked until the evidence write lands |
| S-25 | any service → cache tier | Sync, best-effort | **A miss is a read, never an error.** Cache unavailable degrades latency, and never authorisation or configuration correctness |
| S-26 | `#4` / Keycloak → bank systems over the TGW | Sync | Path loss is a typed `503`; `dev` may stub, `uat`/`prod` may not |

Cross-boundary seams that the SVG names rather than draws (they skip bands): `S-04/S-05`, `S-13`, `S-14/S-15`, `S-17`, `S-18`, `S-23/S-24`.

### 4.3 Idempotency

`Idempotency-Key` is required on every mutating platform API (`INV-IDM-01`). Client-supplied at the edge; **server-derived** internally from owning aggregate id + operation. Same body → stored response. Different body → `409 IDEMPOTENCY_KEY_CONFLICT`. Missing → `400 MISSING_IDEMPOTENCY_KEY`. Retention 24 hours, in the **owning service's store**.

**This did not change when the cache tier arrived.** `ADR-011` provisions a shared Valkey tier and explicitly refuses to hold idempotency in it: the record has to be written in the same transaction as the business change, and a cache cannot be transactionally consistent with a database write. Idempotency that is only mostly right on the money path is worse than none — which is why the target-state review's "ElastiCache for idempotency" line is rejected by name rather than left to be discovered.

Money path additionally: no new attempt while a prior one is `AUTHORISED` or `UNCERTAIN`.

---

## 5. API details

These are the **R0 contract sketches** implied by the seams and the information model. They are not a published OpenAPI (that is S08/S11 work, `FF-15`). Flutter talks **only** to `#2`. Service-to-service calls never go via the public gateway.

Common headers on every call: `Authorization` (BFF: session; internal: service identity), `X-Correlation-Id`, `X-Journey-Id` (once a journey exists), `Idempotency-Key` on mutations. `distributorId` is **never** accepted from a caller.

### 5.1 Public / BFF — `#2` RM Workspace BFF

Base: `https://{env}-insurance.aubank.in/api/v1` (name is illustrative; DNS is S09).

| Method | Path | Actor | What it does | Hard gate |
|---|---|---|---|---|
| `POST` | `/sessions` | RM, IPR | Establish opaque session; BFF talks to IdP adapter | Tokens never returned |
| `DELETE` | `/sessions/current` | RM, IPR | Logout; revoke server session | |
| `GET` | `/me` | RM, IPR | Principal + SP certification snapshot + `insurerId` (IPR) | |
| `POST` | `/opportunities` | **RM only** | Create opportunity (`S-20`) | SP cert valid for `lob` |
| `GET` | `/opportunities/{leadId}` | RM; IPR if `AC-4` | Resume / status | IPR: absent if gated |
| `POST` | `/opportunities/{leadId}/journeys` | RM | Start journey from a `QUALIFIED` opportunity | |
| `GET` | `/journeys/{journeyId}` | RM; IPR gated | Stage + references only | |
| `POST` | `/journeys/{journeyId}/need-analysis` | RM | Complete need analysis | SP re-checked |
| `POST` | `/journeys/{journeyId}/suitability` | RM | Evaluate (`S-07`) | C1 |
| `POST` | `/journeys/{journeyId}/consent/challenge` | RM | Send OTP to **customer device** (`S-06`, `S-18`) | C2 |
| `POST` | `/journeys/{journeyId}/consent/verify` | RM | Bind `otpTxnId` to grant | Customer-device OTP |
| `POST` | `/journeys/{journeyId}/quotes` | RM | Create quote (`S-09`); returns `202` + `quoteId` | C1 via `S-08` |
| `GET` | `/journeys/{journeyId}/quotes/{quoteId}` | RM | Poll until `QUOTED` / `PARTIALLY_QUOTED` / `FAILED` / `TIMED_OUT` | |
| `POST` | `/journeys/{journeyId}/quotes/{quoteId}/selection` | RM | Select offer | |
| `POST` | `/journeys/{journeyId}/proposals` | RM | Create draft from selected offer | Unexpired consent |
| `POST` | `/journeys/{journeyId}/proposals/{proposalId}/submit` | RM | Submit (`S-12`); `202` | |
| `GET` | `/journeys/{journeyId}/proposals/{proposalId}` | RM; IPR gated | UW status | |
| `POST` | `/journeys/{journeyId}/payments` | RM | Create payment; **link is sent to customer**, never returned into the RM session | **C4** |
| `GET` | `/journeys/{journeyId}/payments/{paymentId}` | RM | Status including `UNCERTAIN` / `RECONCILED` | |
| `GET` | `/journeys/{journeyId}/policies/{policyId}` | RM; IPR gated | Issued policy + document refs | |
| `GET` | `/catalogue/offerings` | RM; IPR own-insurer | R0 Term matrix | `(lob, insurerId)` |
| `GET` | `/config/forms/{formId}` | RM, IPR | Field validation rules for this `lob` | `S-21` |

**Deliberately absent from the BFF:**

- Any path that takes a card, account or PG credential.
- Any path that returns a payment URL into an RM or bank-employee session (`FF-14`).
- Any path that accepts `distributorId`.
- Customer self-service APIs (`#1`, R1).
- Admin configuration write APIs (R1). Configuration is seeded from source-controlled artefacts in R0.

### 5.2 Customer device — not a platform API

The customer does **not** call the BFF in R0.

| What | Where it terminates |
|---|---|
| Consent OTP | Bank SMS/email → customer phone; verification id posted **by the RM session** after the customer confirms |
| Premium payment | Hosted AU Bank PG page (3-D Secure). Platform receives `S-14` callback and `S-15` settlement file |

That split is control **C4** as architecture, not as a UI convention.

### 5.3 Internal service APIs (cluster-private)

Called by `#2` or `#9` over mTLS or mesh-equivalent later; R0 uses IRSA + NetworkPolicy + service identity. Not published.

| Service | Representative resources | Notes |
|---|---|---|
| #5 Lead | `POST /internal/v1/leads`, `GET …/{leadId}` | Rejects non-`BANK_RM` at the service, not only the BFF |
| #9 Journey | `POST /internal/v1/journeys`, `POST …/{id}/transitions`, `GET …/{id}` | Transition payload is a *reference + event*, never an embedded decision |
| #4 Customer | `GET /internal/v1/customers:lookup?cif=` | Snapshot; does not write CBS |
| #6 Consent | `POST /internal/v1/consents`, `POST …/{id}/verify`, `GET …/{id}` | Append-only |
| #7 Suitability | `POST /internal/v1/assessments`, `GET …/{id}` | `S-08` is a read of validity, 500 ms, no retry |
| #8 Catalogue | `GET /internal/v1/offerings` | Read-through cache inside the service |
| #10 Quotation | `POST /internal/v1/quotes`, `GET …/{id}` | Suitability id required |
| #11 Proposal | `POST /internal/v1/proposals`, `POST …/{id}/submit`, `GET …/{id}` | |
| #12 Payment | `POST /internal/v1/payments`, `POST /internal/v1/payments/pg-callbacks`, `GET …/{id}` | Callback authenticated by PG signature, not by RM session |
| #13 Policy | `POST /internal/v1/policies`, `GET …/{id}` | Guard: payment state `RECONCILED` |
| #14 Hub | `POST /internal/v1/provider-requests`, `GET …/{id}` | Canonical contract IF-1 |
| #15 Adapter | existing 1SB job API | Unchanged; Hub is the only caller from WS-3 |
| #16 Audit | `POST /internal/v1/audit-events` (outbox consumer), `GET` query | INSERT-only IAM |
| #17 Notification | `POST /internal/v1/notifications` | SMS/email; no journey block |
| #19 Configuration | `GET /internal/v1/config:resolve?domain=&lob=&key=&at=` · admin write via maker-checker | Fail closed; admin UI is R0 W4, isolated (`ADR-014`) |
| #3 PDP | `POST /internal/v1/authorize` `{subject, action, resource, context}` | 300 ms, fail closed |

### 5.4 Typical error codes (stable)

| HTTP | Code | Meaning |
|---|---|---|
| 400 | `MISSING_IDEMPOTENCY_KEY` | Mutation without key |
| 403 | `ORIGINATION_RM_ONLY` | Non-RM create |
| 403 | `SUITABILITY_REQUIRED` | C1 |
| 403 | `CONSENT_REQUIRED` | C2 |
| 403 | `SP_CERTIFICATION_INVALID` | Expired / suspended / wrong LOB |
| 403 | `PAYMENT_DEVICE_VIOLATION` | Attempt to issue a pay link into an RM session |
| 409 | `IDEMPOTENCY_KEY_CONFLICT` | Same key, different body |
| 409 | `PAYMENT_ATTEMPT_BLOCKED` | Prior attempt `AUTHORISED` or `UNCERTAIN` |
| 422 | `CONFIGURATION_UNRESOLVABLE` | `S-21` fail closed |
| 503 | (typed) | Dependency down; body names which class (PDP, CBS, PG, Hub) |

---

## 6. Business logic — what the platform will not do

These are standing constraints. Violating one is `SF4` / reject, not a story.

| # | Will not |
|---|---|
| C1 | Produce a quote without a valid, unexpired suitability assessment |
| C2 | Submit a proposal without an unexpired consent grant |
| C3 | Accept `distributorId` from a caller |
| C4 | Execute premium payment on an RM or bank-employee device |
| C5 | Put PII in logs |
| C6 | Store regulated data, backups, logs or archives outside AWS India regions |
| C7 | Allow UPDATE or DELETE on consent, suitability or audit evidence |
| C8 | Infer Policy Sold from quote, proposal or payment alone |
| — | Let a bank app or Flutter call 1SB or a database directly |
| — | Let a platform service call a provider adapter directly |
| — | Let Journey Orchestration hold another context's business decision |
| — | Let an agentic-AI action substitute for a deterministic hard gate |
| — | Use Render.com as a PII or production-like data path (`ADR-001`) |

Per-aggregate legal transitions are in [`01 §4`](../platform/ws3-platform/01-domain-model-and-invariants.md#4-state-machines). Any transition not drawn is illegal and is rejected by the **aggregate**, not by the caller.

---

## 7. What to do when — stages, waves, releases

Three clocks, often confused. They are not interchangeable.

### 7.1 Lifecycle stages (the programme clock — Kalpana / GATE)

Where we **are**. Agents do not edit these fields.

| Stage | Question | Status now |
|---|---|---|
| S07 Solution & Security Architecture | Is the R0 slice designed? | Artefacts exist; human signatures outstanding |
| **S08 Engineering Foundation** | Can we prove the code? | **Current.** `GATE-S08` OPEN |
| **S09 Platform & Environment Foundation** | Can we run, observe and recover it in AWS India? | **Overlapped** with S08. This HLD's companion LLD is the S09 input |
| S11 First vertical slice | Does one real Term sale run through a real UI? | Next, after S08 and the S09 critical path. Entry blocked while GAP-006 (consent) and GAP-007 (suitability) are open |
| S12 Hardening of the slice | Evidence, load, reconciliation | After S11 |
| S13 Expansion | Second channel / remaining deferred contexts | After a proven journey |

**Do not start a business service (W0b–W4) before GATE-S08 and the S09 critical path.** Building `#9` on Render.com with PII is not progress; it is a residency defect (`ADR-001`).

### 7.2 Build waves (the R0 construction clock — this HLD)

What we **build**, in order, once the foundation exists. Colour on the SVG.

| Wave | When | What | Why this order |
|---|---|---|---|
| **W0** | Now | Pipeline, IaC, environments, secrets, observability, WORM, restore drill | S08 + S09. Nothing else is runnable without it |
| **W0b** | First after the gate | `#19` Configuration store + seeds | Every later wave reads it. Building services first is how hardcoded branches get written (`CF-5`) |
| **W1** | After W0b | `#5` Lead, `#9` Journey, `#14` Hub, `#4` Customer, `#8` Catalogue | Spine. Downstream cannot exist without a Lead and a journey |
| **W2** | After W1 | `#6` Consent, `#7` Suitability, `#10` Quotation | The two hard gates, then the quote path they protect |
| **W3** | After W2 | `#11` Proposal, `#12` Payment, `#13` Policy, `#16` Audit | Money, issuance, evidence. Audit must exist before the first regulated journey completes |
| **W4** | After W3 contracts exist | `#2` RM BFF, Flutter RM app, `#17` Notification, Admin UI, `#18` MIS | UI last, against stable contracts. Admin/MIS on the isolated path (`C-ISO-1`). Notification is a C4 dependency |

WS-1 (`#15` adapter) **already exists**. WS-2 (identity adapter + PDP + token-hiding BFF pattern) is an **enabler** in parallel; R0 cannot authorise a single call without the PDP (`S-02`).

Sixteen deployable services plus two apps (RM + Admin), not nineteen. Customer BFF and the direct-insurer adapter remain deferred. Campaign/bulk Lead create stays out.

### 7.3 Releases (the product clock — Rajal)

What the **customer of the programme** gets. Colour on the North Star, not on the R0 SVG.

| Release | What it is | Unpark / entry |
|---|---|---|
| **R0** | This document. Assisted Term, ETB, Group A, one proven sale | Admitted scope in `CURRENT-STATE.yaml` |
| **R1** | DIY customer journey, Customer BFF, Group B redirect, ULIP/Savings, hi-IN, document platform, campaign/bulk on `#5`, journey registry split, callback gateway | After R0 completes a real sale in pilot |
| **R2** | Hybrid assisted ⇄ DIY, call centre, work management, renewals/lapse, NTB + V-KYC | After assisted and DIY both have stable state and hand-off contracts |
| **R3** | HEALTH cell | WS-1 Phase 5 unfrozen **and** R0–R2 have proven the shared platform with real volume |
| **R4** | GENERAL / MOTOR cell | After Health has proven the cell pattern |
| **RN** | Steady state | Direction of travel only — not a plan |

Nothing on the North Star may be cited as authority to start work. Each element still needs AIGEM triage.

The R0 → R1 → R2 *dependency map* (which target component is a prerequisite for which) is a **parked** item (`SUG-20260820-r1t`). It is Kalpana's input as much as Architecture's. This HLD does not produce it.

### 7.4 What to do this week

A short operating picture for anyone picking up work:

1. **Do not admit a second LOB, a Customer BFF, a service mesh, Redis-for-idempotency, or an analytics warehouse on the Lead writer.** Those are recorded deferrals or refusals with triggers. The admin UI and R0 MIS slice were pulled into R0 on 2026-08-25 under `ADR-014` and **must stay off the Lead writer**. The event backbone, cache tier, search pipe, bank connectivity and egress inspection were admitted on 2026-08-24 under `ADR-009` … `ADR-013`.
2. **Close GATE-S08.** Amit, Swapnali, Deepali, Shivanshi, Mahesh. Fitness functions FF-01…FF-28 are the architecture half of that gate — `FF-22` … `FF-28` arrived with the robustness round and are mostly IaC and infrastructure checks, so they land in the S09 lane rather than adding to the application backlog.
3. **Feed S09 from [`R0-LLD.md`](./R0-LLD.md).** Shivanshi provisions; Deepali signs the trust-boundary realisation; Aarti signs the Aurora/DynamoDB/S3 design. Restore is executed and timed (`S09-G7`).
4. **Seed `#19` in W0b** the moment environments exist — consent rule pack and suitability rule pack are Product/Compliance artefacts (GAP-006, GAP-007) and **block S11** until Shailja signs them at evidence level E2.
5. **Then W1 → W2 → W3 → W4**, one wave at a time, one owner per in-flight item.

---

## 8. Workstream split

| Workstream | Role | Delivers in R0 |
|---|---|---|
| **WS-3** | Primary — the platform | Everything in §3 except #3 and #15 |
| **WS-1** | Supplier — IF-1 | `#15` 1SB Adapter, already built; Term path in hardening (`GATE-P4` BLOCKED on sandbox E2E and S08 coverage) |
| **WS-2** | Enabler — IF-2 | Workforce identity: token-hiding BFF pattern, Keycloak behind adapter, PDP. Does **not** deliver retail-customer identity (not required for assisted R0) |

A WS-3 service never imports `adapter.onesb.*`. A Flutter client never sees Keycloak.

---

## 9. What this HLD does not decide

| Item | Owner |
|---|---|
| Physical schema, indexes, `lob` partition keys | Aarti (`OPEN-I6`) |
| Autoscaling targets, node sizing, cost model | Shivanshi (S14 sizes load; S09 lays the floor) |
| Exact IPR permitted-action set (assistance vs solicitation) | Shailja (`OPEN-D9`) — architecture ships the gate default-deny |
| Flutter design system and state management | Mahesh + Rajal (S05 still missing, GAP-009) |
| Topic partition counts, retention windows and the topic-to-consumer IAM matrix | Shivanshi + Deepali. The **backbone** is decided (`ADR-012`, §3 boundary 9); its operational parameters are not architecture's |
| Broker, cache and search node sizing, and the cost envelope for all three | Shivanshi + Kalpana (`RISK-012`). Architecture sized them for availability and evidence, never for throughput |
| The bank's own side of the connectivity: VPN termination, prefixes, firewall change, DX order | Shivanshi + bank network (`DEP-20260824-dx1`). The **pattern** is decided (`ADR-009`); the bank's work is not ours to decide |
| Production IdP (Cognito vs Keycloak) | WS-2 Phase 2 |
| Consent wording and suitability questions | Rajal + Shailja (GAP-006, GAP-007) |

---

**Signed:** Mahesh — Principal Insurance Platform Architect (Board 1), AI-drafted
**signature_status:** `AI-DRAFTED — mandatory human T4 Architecture sign-off outstanding`
**Companion LLD:** [`R0-LLD.md`](./R0-LLD.md)
**Diagram:** [`r0-reference-architecture.svg`](./r0-reference-architecture.svg)
