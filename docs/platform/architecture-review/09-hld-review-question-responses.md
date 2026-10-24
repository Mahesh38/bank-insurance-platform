# HLD Review — Responses to Internal Review Questions

**Up:** [docs index](../../README.md) → [platform](../README.md) → [architecture review](./README.md) → **HLD review responses**
**Subject:** Questions raised against [`docs/hdl.svg`](../../hdl.svg) (R0 High-Level Design) in internal architecture review
**Date:** 2026-08-18
**Status:** **Analysis only.** This document takes no decision, changes no service, opens no gate and creates no work item. It answers eleven review questions by stating (a) what the repository already contains, (b) what exists in design but is not built, (c) what is genuinely missing, and (d) where we already have the capability under a different name. Anything that needs to become a decision must go through the normal AIGEM route afterwards.

---

## 0. Read this first — why the diagram looks like it ignores these questions

`docs/hdl.svg` is deliberately **the R0 slice**, not the platform. Its own footer says so:

> *Deferred to S13: Lead (#5), Customer BFF (#1), Reporting & MIS (#18), Admin UI (#19)*

R0 is defined in [`03-solution-architecture-r0.md §2`](../ws3-platform/03-solution-architecture-r0.md) as
**one RM, one ETB customer, one Term product, one insurer, end to end** — twelve deployable services
plus one app, out of a nineteen-context target. Eight of the eleven questions below are about
contexts the target architecture already names and the R0 diagram deliberately leaves out. That is a
**scoping** answer, not a hole — but three questions (quote caching logic, LOB blast-radius
isolation, post-issuance servicing/BMS) are real design gaps and are marked as such.

### Summary table

| # | Question | Verdict | Where it lives today |
|---|---|---|---|
| 1 | Lead system, RM-created leads, lead lifecycle | **Designed in full, not built** — deferred to S13 | Context #5; state machine, attributes, invariants, BRD §3 |
| 2 | Telecallers / call centres, customer-created leads, drop-offs | **Partly designed, actor missing** — DIY blocked on customer identity | Channel enum, `ABANDONED` paths, `source` enum; no telecaller actor anywhere |
| 3 | Is Journey Orchestration a "lead middleware"? | **No** — and deliberately not | Contexts #9 vs #5, INV-JRN-02, relationship R-02 |
| 4 | Quote caching, now and after own integrations | **Real gap.** Three in-process caches exist; no quote/premium cache, no policy | `MasterDataService`, `OneSbProposalAdapter`, `InMemoryIdempotencyStore` |
| 5 | LOB blast-radius separation (health change ≠ motor impact) | **Partly there** — compile-time modularity yes, deploy/runtime isolation no | LOB handler registries, ArchUnit, per-provider bulkheads (designed) |
| 6 | Separate journey service per LOB? | **No — recommend one engine, LOB-parametrised** | "One workflow, many LOBs" principle already in the 1SB architecture |
| 7 | Quote data retention | **Class assigned (`RET-7Y`), horizon unconfirmed** | Information model §2.2/§4.5, NFR-DAT-01 |
| 8 | Proposal data retention | **Class assigned (`RET-7Y` + encrypted payload store)** | Information model §4.6, INV-PRP-05 |
| 9 | Payment / policy issuance retention | **Assigned — note `RET-POLICY+7Y`, not 7Y flat** | Information model §2.2, NFR-DAT-01/02 |
| 10 | Post-proposal requirements (docs, medicals) | **Status model exists; capability not built, no document store** | `BankApplicationStatus.UW_REQUIREMENTS`, Proposal `REQUIREMENTS_PENDING` |
| 11 | Post-issuance BMS (renewal, service requests) | **Out of scope by decision (R2+); half of it exists as Policy & Issuance** | Charter §3.2, Policy state machine already models `LAPSED`/`SURRENDERED`/`MATURED` |

---

## 1. Lead system — RM creates a lead for themselves, lifecycle managed on the lead

### What we already have (design-complete)

Lead is **bounded context #5**, and it is one of the more completely specified contexts in the repo:

| Artefact | Location |
|---|---|
| Service, owner, datastore | [`02-target-microservices-architecture.md`](./02-target-microservices-architecture.md) — Lead, own Aurora PostgreSQL, talks to Customer + RM Workspace BFF |
| Aggregate + consistency boundary | [`01-domain-model-and-invariants.md §3`](../ws3-platform/01-domain-model-and-invariants.md) — root `leadId`; Lead + assignment history + follow-ups |
| Full state machine | §4.1 — `NEW → ASSIGNED → CONTACTED → QUALIFIED → CONVERTED`, plus `DISQUALIFIED` and `EXPIRED` |
| Attribute sheet | [`02-information-model.md §4.2`](../ws3-platform/02-information-model.md) — `leadId`, `customerId`, `state`, `lob`, `source`, `assignedRmId`, `assignmentHistory[]`, `followUps[]`, `expiresAt`, `convertedJourneyId` |
| Invariants | INV-LED-01 (terminal state accepts no transition), INV-LED-02 (exactly one journey converts a lead), INV-LED-03 (assignment only to a currently SP-certified principal) |
| Business requirements | [`BRD-OVERVIEW.md §3`](../../au-bank-insurance-platform/requirements/BRD-OVERVIEW.md) — create, update, follow-up/reminder, assignment/reassignment, duplicate flagging, expiry, deletion at R0; bulk upload and campaigns at R1 |
| Acceptance criteria | [`S03-requirements-evidence.md`](../../application-lifecycle-bible/evidence/S03-requirements-evidence.md) — `AC-LEAD-010-1` (lead create mints `leadId` **and** `journeyId`, owned by creating RM), `AC-LEAD-020-1` (resume at last incomplete step), `AC-LEAD-020-2` (90-day dormancy, reopening re-validates consent and suitability), `AC-LEAD-030-1` (status history queryable with timestamps and actors) |
| Screens | [`S05-experience-evidence.md`](../../application-lifecycle-bible/evidence/S05-experience-evidence.md) — `SCR-02` RM pipeline (own leads, filter, resume), `SCR-05` lead create / LOB confirm |
| Migration constraint | [`07-BUSINESS-CLARIFICATIONS §14`](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) — Lead module lives **inside** the platform for MVP, must be migratable to the bank's internal system (**"Sampath"**) later |

**So the answer to "how are we going to handle the lead system" is: exactly as specified in context #5 —
the RM creates a lead for themselves (`source = RM`, `assignedRmId = self`), the lead owns the
pre-sale lifecycle (assign → contact → qualify → convert/disqualify/expire), and conversion is
driven by the journey reaching `SOLD`, not by the lead itself.**

### What is missing

1. **No code.** `services/` contains four services (`1sb-integration-service`,
   `bank-persistence-service`, `identity-authorization-service`,
   `identity-provider-adapter-service`) plus `workforce-access-bff`. There is no Lead service, and
   R0 does not build one — [`03-solution-architecture-r0.md §3`](../ws3-platform/03-solution-architecture-r0.md)
   defers it to S13, and the architecture review puts it in phase **P3**.
2. **Two configuration values are undecided**: the lead ageing horizon (`expiresAt`) is OPEN-D4, and
   whether reassignment restarts the follow-up SLA is OPEN-D1. Both are Product decisions, not
   architecture ones.
3. **Duplicate-lead detection** is a BRD R0 line (§3.7) with no design behind it — no matching rule,
   no merge semantics. Worth raising in the review as a genuinely open item.
4. **The Sampath migration path is asserted, not designed.** Nothing states whether Sampath becomes
   the system of record with the platform reading it, or the platform keeps leads and syncs. That
   choice changes the Lead context's contract materially and should be recorded before Lead is built.

### Naming note for the review

The diagram has no "lead" box because R0 starts a journey from a **customer lookup**, not from a
lead. In the target state, `leadId` and `journeyId` are minted together at lead creation
(`AC-LEAD-010-1`) — the lead is not a stage of the journey, and the journey is not a field on the lead.

---

## 2. Telecallers from call centres, customer-created leads, and drop-offs

This is the question the current documentation answers **least** well, and the honest answer has
three parts.

### 2a. Customer-created leads ("customer shows interest")

- The data model **already allows it**: `Lead.source` enumerates `RM, campaign, self-service`, and
  `Journey.channel` enumerates `RM-assisted / self-service / hybrid`.
- The **journey is blocked on one unresolved item**: there is no customer principal. Retail-customer
  authentication is explicitly out of WS-2 scope, while `R0-SCOPE` originally put self-service in
  Day-1 scope. That contradiction is recorded as **A-F03 / condition C-07** (Board 1),
  **SEC-OPEN-1** (security architecture §3.1) and **GAP-023**, and was resolved by sequencing:
  **DEC-20260816-03 — R0 is assisted-first; DIY revisits at R1; hybrid at R2**, with the Customer
  BFF (#1) landing with DIY at R1.
- So: *we can model a customer-originated lead today; we cannot let a customer log in and act on it
  until the customer identity decision is made.* That decision is the critical path for DIY, not the
  journey engine.

### 2b. Telecallers / third-party call centres

**This actor does not exist anywhere in the repository.** Not in the persona set, not in the
capability map, not in WS-2's role model (which covers bank workforce, with partner hierarchy depth
at R1), not in the RBAC/ABAC policy design, not in the HLD. It is a genuine gap and should be raised
as one.

The architecture does not need to change to absorb it, and this is the useful thing to say in the
review:

| Concern | How the existing architecture absorbs a telecaller |
|---|---|
| Who they are | A **workforce principal in a different org unit**, federated or locally provisioned through the existing `identity-provider-adapter` → PDP path. Not a new identity mechanism |
| What they may do | An **ABAC/relationship policy** in `identity-authorization-service`: may act on leads assigned to their queue, may not act on another RM's leads. Default-deny already applies (S-02, fail closed) |
| Which journey they run | **The same one.** A telecaller-assisted journey is `channel = RM-assisted` with a different actor role — it is not a new state machine |
| Selling authority | INV-LED-03 already blocks assignment to a principal without a valid SP certificate for the LOB. If telecallers only *assist* and never *advise*, that becomes a distinct role with a distinct entitlement — a Compliance question (IRDAI SP certification), not an architecture one |
| Ownership hand-off | Lead `assignmentHistory[]` is already append-only with `{rmId, assignedAt, assignedBy, reason}` — telecaller → RM hand-off is a reassignment, already modelled |
| Attribution | Every audit event already carries `actor_id`, `actor_type`, `distributor_id`, `agent_id`. A telecaller-assisted sale stays attributable |

**What must be added, and is not there:** a telecaller/call-centre role in the WS-2 role model and
authority matrix; a queue/worklist concept (leads are assigned to a principal today, not to a pool);
a DPDP position on a **third-party processor** handling customer personal data for outbound calling;
and a decision on whether a telecaller may complete a DIY customer's journey or only unblock it
(that is the hybrid mode-switching question, currently R2).

### 2c. Drop-offs — "they drop in between, how are we managing it"

This part **is** designed, and well:

| Mechanism | Where |
|---|---|
| Every non-terminal journey stage has an `ABANDONED` exit on inactivity timeout | Journey saga, §5 of the domain model |
| Every non-terminal journey **must** carry a next action and an inactivity horizon (`nextActionDueAt`) | INV-JRN-03 — a journey with no horizon fails validation at creation |
| Resume semantics | `AC-LEAD-020-1` — reopening resumes at the last *incomplete* step with prior data intact |
| Dormancy | `AC-LEAD-020-2` — 90 days without activity ⇒ `DORMANT`; reopening **re-validates consent and suitability currency** (aligned to consent rule pack CNS-R21) |
| Abandoned-with-consent case | Failure matrix F-02 — consent evidence is retained, never deleted, and the journey records `ABANDONED` with the consent reference |
| Quote expiry mid-proposal | F-03 — proposal held in `DRAFT`, journey returns to `QUOTING` for a re-quote, captured values retained and re-applied |
| Payment window expiry | Journey `PAYMENT_PENDING → ABANDONED`; payment link TTL is a first-class state |

**What is missing on drop-off:** the *re-engagement* half. There is no campaign/nurture capability
(R1), Notification (#17) is scoped in R0 to the transactional minimum (OTP + payment link only), and
BRD §12 welcome calling is R1. So today a drop-off is **recorded and resumable, but nobody is
automatically chased.** That is the gap a telecaller operating model would consume, and it is the
strongest argument for pulling Lead + Notification breadth forward if the telecaller model is real.

---

## 3. Is Journey Orchestration a kind of lead middleware service?

**No, and the distinction is deliberate and load-bearing.**

| | Lead (#5) | Journey Orchestration (#9) |
|---|---|---|
| Reason to change | Bank **sales-management** policy: assignment, ageing, campaigns | The **shape of the sale process** itself |
| Owns | The pre-sale opportunity and who is working it | Stage + references for one attempted sale, and the compensation saga |
| Lifecycle | May outlive many attempts; may never convert | One journey, one outcome |
| Relationship | Upstream of the journey — R-02: Journey → Lead is U/D, OHS | Saga owner for all fulfilment contexts |

The hard rule is **INV-JRN-02**: *a Journey never stores a business decision (eligibility, price, UW
outcome); it stores stage plus references* — enforced by an ArchUnit-style build assertion, not by
convention. Its attribute sheet (§4.9) carries `leadId` as a **reference** and nothing else from the
Lead context.

Three practical consequences worth stating in the review:

1. **If Journey held lead state, the Sampath migration becomes a rewrite.** Lead is the one context
   the business has already said will move to another system. Keeping it separate is what makes that
   a contract swap instead of surgery on the saga.
2. **Journey is middleware for the *sale*, not for the *lead*.** It is an orchestrated saga (chosen
   over choreography specifically so there is one queryable, attributable record of where a sale is
   and why it stopped — the thing the legacy AU Beema Portal cannot produce).
3. **Where the industry says "lead management system", we have two things**: Lead (#5) for the
   opportunity, Journey (#9) for the sale. `LeadQualified` starts a journey; `JourneySold` converts
   the lead (INV-LED-02: exactly one journey may convert a lead; a second event is idempotently
   ignored and a differing `journeyId` raises an integrity alert).

---

## 4. Quote caching — today via 1SB, and after our own insurer integrations

### What exists in code today

| Cache | Implementation | TTL | Notes |
|---|---|---|---|
| Master/lookup data | `MasterDataService` — in-process `ConcurrentHashMap` | 4 h (`insurance.masters.cache-ttl-seconds`) | Serves **stale on 1SB failure** — deliberate degradation |
| Proposal form schema | `OneSbProposalAdapter` — in-process map keyed by `{lob, productCode, manufacturerId, version}` | 1 h | Documented target is Redis; today it is per-pod |
| Idempotency | `InMemoryIdempotencyStore` + `IdempotencyFilter` | 24 h contract | Replay of the same key returns the stored response — a de-dup cache, not a result cache |
| Quote **results** | `integration_job` + `integration_job_offer` in `bank-persistence-service` | durable | `GET /v1/quotes/{jobId}` reads the store, **never re-calls 1SB** |

**So there is no premium/quote cache today.** What we have is: repeat *reads* of a quote are served
from the durable job store, and provider *metadata* (masters, schemas) is cached in-process. Redis is
not deployed — that is **TD-010**, parked, "before horizontal scale-out".

### What the design says

- ElastiCache Redis is the platform cache tier, in front of Aurora/1SB, TTL-based, "exactly the
  `ProposalSchemaCache` pattern already in the 1SB adapter — reused platform-wide, not reinvented
  per service" ([`03-communication-patterns.md`](./03-communication-patterns.md)).
- Config changes bust caches via a `ConfigChanged` event rather than services blocking on
  Administration & Config being up.
- Quote and Offer carry `validUntil` in the information model — i.e. quote validity is already a
  domain concept, distinct from any cache TTL.
- The number driving all of this: **NFR-LAT-03 — quote result end to end p95 < 5 s**.

### The gap, stated plainly

Nothing in the repository defines a **quote result cache**: no key, no TTL policy, no invalidation
rule, no owning service, no rule about when a cached premium may and may not be used. That is a real
finding from this review.

### Recommended answer (for the review, not yet a decision)

**Which service owns the logic: Quotation (#10).** Not the adapter, not the Integration Hub.
Reasons: the cache must be keyed on **bank-canonical risk attributes** so it behaves identically
whether the offer came from 1SB or from a direct insurer integration; the Hub is a routing layer with
no business data by design; and the adapter is provider-shaped, so a cache there fragments the moment
a second provider appears.

**What may be cached, and what may not:**

| Data | Cacheable | Why |
|---|---|---|
| Product/eligibility matrix, master enums, form schemas | **Yes**, hours | Low change rate, no financial consequence — this is what we already cache |
| Indicative premium for **display/comparison** | **Yes**, short (minutes) | Latency lever for re-quote and list refresh |
| Premium used at **offer selection, proposal or payment** | **No — always re-validate** | INV-PAY-03 requires `Payment.amount` to equal the selected offer to the paise; a stale premium becomes a financial break, not a UX defect |
| Insurer decline / out-of-bound results | Yes, shorter TTL | Avoids hammering a provider for a known-bad risk, but must expire fast — underwriting rules change |

**Cache key = a canonical risk fingerprint**, not the raw request: `{lob, insurerCode, productCode,
sumAssured, coverTerm, PPT, premiumFrequency, mode, member set (age band, gender, tobacco, income
band), pincode/zone, add-on set}` **plus a version salt** = `{catalogueVersion, rateTableVersion,
configVersion}`. The version salt is what makes invalidation tractable: an insurer rate change bumps
the salt and the whole cohort ages out without a scan.

**TTL** = `min(insurer-declared quote validity, configured display TTL)`, and the cached entry is
**never** allowed to outlive the `Quote.validUntil` of the aggregate it was derived from.

**Two layers**, matching what already exists: L1 in-process (seconds, per pod, for burst/refresh) and
L2 ElastiCache Redis (shared, minutes) — closing TD-010 gives us L2 for idempotency, schemas and
quotes at once.

**Also add in-flight de-duplication** (single-flight): two identical quote requests arriving together
should join one provider call rather than fan out twice. That is distinct from result caching and is
usually the bigger win on a fan-out/poll workload like ours.

**When we integrate insurers directly:** the caching layer does **not** move. Quotation keeps owning
it; each adapter contributes only its provider's declared quote validity and rate-table version to
the key salt. 1SB's own internal caching becomes invisible to us — which is the point of the
canonical contract, and the reason the cache must not live in the 1SB adapter today.

**Standing constraint:** the cache is an optimisation over the durable `Quote` aggregate, never a
system of record. Every premium a customer was shown must remain reproducible for seven years
regardless of cache state (see §7).

---

## 5. LOB blast-radius separation — a health change must not break motor

### What exists today (compile-time and structural isolation — real, and already proven)

| Mechanism | Evidence |
|---|---|
| One workflow, many LOBs | `QuoteService` / `ProposalService` hold the orchestration once; LOB differences live only in handlers and mappers |
| Per-LOB strategy beans, resolved by enum | `LobQuoteHandlerRegistry` / `LobProposalHandlerRegistry` — `EnumMap<Lob, Handler>`, duplicate registration fails at startup, unknown LOB is a typed `422 UNSUPPORTED_LOB` |
| Per-LOB packages | `lob/life/term`, `lob/life/saving`, each with a `package-info` |
| LOB is a discriminator field, not a URL path | One endpoint family per capability; no per-LOB API surface to fork |
| Provider types cannot leak | INV-ACL-01 / ArchUnit — 1SB types confined to `adapter.onesb.*`; the rule generalises per adapter |
| Per-LOB observability | `OneSbCallMetrics` — latency and error rate per LOB **and** operation |

Adding or changing a LOB therefore means adding or changing a handler, not touching the shared quote
or proposal orchestration. That is the isolation we actually have, and it is the right first layer.

### What is missing

**Everything after compile time.** Term and motor ship in the same artefact, in the same pod, behind
the same connection pool. A health change today means a redeploy of the service that also serves
motor. Specifically absent:

1. **No per-LOB kill switch / feature flag.** The `RoutingPolicy` per LOB/product designed for the
   Integration Hub (#14) is not built — the Hub itself is not built.
2. **No per-LOB runtime bulkhead.** Per-**provider** bulkheads are designed (seam S-10, NFR-THR-03,
   "one failing insurer must not make every insurer look down") but not implemented, and provider ≠ LOB.
3. **No per-LOB release gate.** No LOB-scoped contract-test suite that must pass before a LOB's
   handler changes ship.
4. **No per-LOB canary or progressive rollout.**

### Recommended answer

Keep **one service per capability, isolation per LOB inside it**, and add four things in this order:

1. **Routing + enablement in the Integration Hub** — enabling/disabling or re-pointing a LOB becomes a
   config change with a `ConfigChanged` event, not a deployment. This is the single highest-value item.
2. **Bulkhead and circuit-breaker per `{provider, LOB}` pair**, not per provider alone — so a slow
   health insurer cannot drain the outbound connection budget that motor quotes need.
3. **Per-LOB rule packs and form schemas versioned in Administration & Config** — so a health
   underwriting-rule change is a config release with its own rollback, never a code release.
4. **Per-LOB test suites as a release gate** plus per-LOB SLOs on the existing per-LOB metrics, so
   "did the health change hurt motor" is answerable from a dashboard rather than from an incident.

Physically separating LOBs into different deployables buys the last 10% of isolation for a large
multiple of the operational cost — thirteen services becomes thirty-nine — and duplicates the
compensation logic, which is the code we least want copied. Revisit only if a LOB develops a
genuinely different scaling or lifecycle profile (motor renewals at 10× the volume of new life sales
is the realistic trigger).

---

## 6. Should the journey for different LOBs live on different services?

**Recommendation: no — one Journey Orchestration service, LOB-parametrised.**

Reasons, in order of weight:

1. **The journey's reason to change is the shape of the sale process, and that is LOB-independent.**
   Lead → suitability → consent → quote → proposal → payment → issuance is the same spine for term,
   health and motor. A context that always changes with another is not a context (the
   distinct-reason-to-change test, §2.3 of the domain model).
2. **Splitting it triples the saga and compensation code — the most safety-critical code we have.**
   The paid-but-not-issued path (F-05), the `UNCERTAIN` payment path (F-08), the reconciliation break
   (F-07) are identical per LOB and will silently drift once copied.
3. **Audit reconstruction and MIS become per-LOB.** The gapless `sequence_no` per `journeyId` and the
   single-query journey reconstruction (S06-G7) are the platform's regulatory answer; three engines
   means three reconstructions.
4. **The 1SB service already proves the pattern works** — "one workflow, many LOBs" is an existing,
   tested principle in this repository, not a hypothesis.

**What genuinely varies per LOB, and where it should live:**

| Variation | Where |
|---|---|
| Which steps are required (motor inspection, health medicals, life ACR) | **Per-LOB journey definition** — a versioned step/state template consumed by one engine, not a forked engine |
| Form schemas and validation | Already dynamic, provider-supplied, cached per `{lob, product, manufacturer, version}` |
| Eligibility and suitability rules | Versioned rule packs in Administration & Config |
| Insurer/product routing | `RoutingPolicy` per LOB/product in the Integration Hub |
| Provider payload mapping | Per-LOB handlers/mappers in the adapter — as today |

The one change this implies to the current design: the journey state machine should be **data-driven
per LOB** (a declared step catalogue with guards), not a hard-coded enum sequence. That is worth
saying now, because it is cheap before Journey Orchestration is built and expensive afterwards.

---

## 7. How long do we keep quote data?

**Class assigned, horizon not yet confirmed by Compliance.**

| Data | Class | Horizon |
|---|---|---|
| `Quote` + `Offer` aggregate (including members, requested cover, selection) | `RET-7Y` | Record close + 7 years |
| Raw 1SB quote request/response payloads | 7 years | `raw_payload.retain_until` column **exists today**; S3 + Object Lock in target |
| Quote **job** / correlation records, idempotency keys | `RET-TRANSIENT` | ≤ 24 h — "short TTL, never treated as durable records" |
| Quote cache entries (§4) | not a retention class | Cache is not a record |

Backed by NFR-DAT-01 (7 years from event time), NFR-DAT-02 (deletion under Object Lock is refused,
proven by FF-10) and NFR-DAT-07 (purge past horizon with a **disposal audit record**, PII-06).

**Two things to flag honestly in the review:**

1. **The 7-year value is not yet ratified.** OPEN-I4 / NFR-OPEN-4 / D-011: retention horizons must be
   confirmed against the final IRDAI/DPDP position, owner Shailja, before S11 entry. The rule the
   repo applies until then is "configurable, policy-driven controls" — do not hard-code.
2. **We have not distinguished a converted quote from an abandoned one, and DPDP makes that matter.**
   A quote that became a policy is part of the policy record and should follow the policy. A quote
   that was never converted is personal data held for a purpose that ended. Under DPDP minimisation,
   keeping an abandoned prospect's health and income answers for seven years needs a stated basis.
   **Recommendation to put to Compliance:** converted quote → follows the policy (`RET-POLICY+7Y`);
   abandoned quote → business retention (12–24 months) then anonymise, keeping only non-personal
   aggregates for MIS. This is currently undecided and is the sharpest retention question on the list.

---

## 8. How long do we keep proposal data?

| Data | Class | Notes |
|---|---|---|
| `Proposal` aggregate, `UnderwritingCase`, requirements | `RET-7Y` | Record close + 7 years |
| Proposal **form values containing PII** | `RET-7Y`, `RESTRICTED` | INV-PRP-05 — encrypted payload store only, never in queryable proposal columns; schema-assertion test enforces it |
| Raw provider proposal payloads | 7 years, immutable | S3 + Object Lock, cross-region replicated to ap-south-2 |
| Audit events for every proposal transition | `RET-7Y-IMMUTABLE` | INSERT-only role, RPO 0 |

**Declined, withdrawn and abandoned proposals follow the same horizon** — they are regulated records
of a sale attempt carrying consent and suitability evidence, and the audit reconstruction test
requires them. The DPDP minimisation argument in §7 is weaker here precisely because a submitted
proposal is a regulated record, not a marketing artefact.

**Gap:** documents attached to a proposal (KYC, income proof, medicals) have **no retention class,
because no document store is modelled for R0** — see §10.

---

## 9. How long do we keep payment and policy issuance data?

| Data | Class | Horizon — note the difference |
|---|---|---|
| `Payment`, attempts, reconciliation record | `RET-7Y` | Record close + 7 years |
| `Policy` record | **`RET-POLICY+7Y`** | **Policy termination + 7 years** — for a 30-year term plan that is 37+ years, not 7 |
| Customer profile snapshot used by the journey | `RET-POLICY+7Y` | Same long tail |
| Policy documents / COI | `RET-7Y-IMMUTABLE` | S3 Object Lock, WORM |
| Audit events (payment link issued, capture, reconciliation, issuance, confirmation) | `RET-7Y-IMMUTABLE` | RPO 0 (NFR-DR-03) — regulatory evidence gets no loss window |

**`RET-POLICY+7Y` is the one the review will not have priced.** It means the platform must retain a
customer's personal data for the life of the policy plus seven years, which drives the encryption key
hierarchy (CMK per data class), archive tiering, and the fact that "delete the customer" is never a
hard delete while a policy is live.

**What is missing on retention as a whole — the same gap across §7, §8 and §9:**

1. **No purge/disposal implementation anywhere.** NFR-DAT-07 and PII-06 specify disposal with an
   audit record; the retention cleanup job is backlog item **TECH-009**, parked
   ("Retention job for raw payloads; backup/restore verification", target Phase 6.3), and **GAP-028**
   (key rotation / retention purge jobs) is open. Retention is a two-sided obligation and we
   currently implement only the keeping half.
2. **Object Lock / WORM is designed, not provisioned** — S09 work, verified by FF-10 and gate S09-G8.
3. **No legal-hold concept.** A record under dispute, grievance or claim must be exempt from purge.
   Nothing in the model expresses that. Worth adding to the retention decision when Compliance
   ratifies the horizons.

---

## 10. Managing post-proposal requirements — document upload, medicals

### What we already have

| Layer | What exists |
|---|---|
| Provider APIs, catalogued | `getReq` (pending docs/medicals), `docupload`, `docdownload`, CKYC, OTP, inspection — [`api-catalog`](../../1sb-insurance-integration/api-catalog/README.md) with extracted schemas |
| Provider statuses, normalised **in code today** | `REQUIREMENTS_PENDING`, `DOCUMENT_UPLOAD_INITIATED/PENDING`, `DOCUMENTS_UPLOADED`, `REQUIREMENT_VERIFICATION`, `OTP_*`, `KYC_*`, `INSPECTION_*` → **`BankApplicationStatus.UW_REQUIREMENTS`** |
| Domain states | Proposal `UNDER_WRITING ⇄ REQUIREMENTS_PENDING` (`requirementsRaised()` / `requirementsSatisfied()`), and `REQUIREMENTS_PENDING → EXPIRED` on requirement SLA elapse |
| Aggregate decision | Requirements are entities **inside** the `Proposal` aggregate — `UnderwritingCase` is deliberately not its own aggregate, so a requirement update is not a distributed transaction |
| Journey intent | The universal LOB journey already names step 6 "Underwriting & requirements (docs, medicals, CKYC, OTP, inspection)" and an **RM task queue** for `REQUIREMENTS_PENDING` |
| Due-date primitive | INV-JRN-03 — every non-terminal journey carries `nextActionDueAt` |

So the **state model is complete and the provider contract is understood.** The status normaliser
that turns eight provider statuses into one bank stage is running code today.

### What is missing

1. **No requirement or document API is implemented.** The adapter's outbound ports are quote,
   proposal, payment, status, poll, masters — there is **no `getRequirement` port and no `docUpload`
   port**. Fetching and fulfilling requirements is not built.
2. **No document store or document context.** The domain model states this explicitly: *"no
   'Document' context for R0"*. Policy PDFs have an S3 home; **proposal-stage documents and medical
   reports do not.** No classification, no retention class, no CMK assignment, no virus scanning, no
   file type/size policy, no upload token model.
3. **No requirement worklist service.** The RM task queue is named in a journey document and exists
   nowhere else — no service owns "open requirements assigned to me, with SLAs".
4. **No requirement reminder loop.** Notification (#17) in R0 is OTP + payment link only.
5. **Discovery is poll-only.** 1SB pushes nothing; requirements surface when we poll application
   status. Nobody has sized that poll (how often, for how many open proposals, for how long) —
   it is the sleeper capacity question in this area.
6. **Medicals are a coordination problem we have not modelled at all** — TPA/diagnostic-centre
   scheduling, appointment status, report custody. Health data is `RESTRICTED` with a dedicated CMK,
   so this is not a small feature.

### Recommended shape

- **Requirement stays inside Proposal & UW (#11)** — the aggregate decision is already right; add a
  `Requirement` entity with `{type, raisedAt, dueBy, state, fulfilmentRef}` and drive
  `REQUIREMENTS_PENDING` from the count of open requirements.
- **Promote a Document/Content service at R1**, not R0 — one custody store for suitability PDFs,
  proposal attachments, medical reports and policy documents, each with its own retention class and
  CMK. Doing this once is much cheaper than three services each growing an S3 bucket. It also becomes
  the foundation for servicing documents in §11.
- **Requirement upload from the customer device follows the C4 pattern already built for payment** —
  a link to the customer's registered contact, never an RM-session upload of customer KYC. That
  reuses an existing, non-waivable control rather than inventing a second one.
- **The RM worklist is a read model over Journey + Proposal**, not a new source of truth.

---

## 11. Post-payment BMS — booking management, renewals, service requests

### The scope position first

This is **out of R0 and R1 by explicit decision**, and the review should hear the decision rather
than a design:

| Item | Position |
|---|---|
| Renewals and servicing (BR-SERV) | **R2+** ([WS-3 charter §3.2](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md)) |
| Claims administration, insurer underwriting decisioning | **Explicitly out** — GAP-029, GAP-030 |
| Post-issuance policy status check | BRD §13 — **R0 read-only status**, R1 full servicing views |
| Welcome calling | BRD §12 — R1 |
| Commission calculation | BRD §15 — R1/R2+ |
| Reporting & MIS (#18) | R0 pilot funnel, R1 full; service deferred to S13 |

### What we already have that a BMS would be built on

More than the review may expect:

1. **The Policy aggregate already models the post-issuance lifecycle**: `ACTIVE → LAPSED` (renewal
   missed), `→ SURRENDERED`, `→ MATURED`, `→ FREE_LOOK_CANCELLED`, plus `freeLookExpiresAt`. The
   domain model says why: *"omitting them would let the R0 implementation treat `ACTIVE` as terminal
   and hard-code that assumption into the schema… the cost of the omission is a migration, not a
   feature."* R0 implements only the issuance half, but the model does not have to be reopened.
2. **Retention already assumes the long tail** — `RET-POLICY+7Y` exists precisely for servicing and dispute.
3. **Document custody and audit** — policy documents in WORM storage, every transition audited.
4. **The provider seam is reusable** — renewal quotes, endorsements and servicing calls route through
   the Integration Hub with `distributorId` injected server-side (INV-DIS-01), exactly as new business does.
5. **Reporting & MIS (#18) is designed as event-fed and read-only**, never synchronously queried by
   transactional services.

### What a "PolicyBazaar-style BMS" needs that we do not have

| Capability | Status |
|---|---|
| **Policy Servicing context** — post-issuance case management (endorsement, cancellation, free-look processing, refund, complaint/grievance, IGMS) | Missing entirely. Not one of the nineteen contexts. The DBA has already flagged **endorsements (DB-F08)** as a modelling gap |
| **Renewal engine** — due-date tracking, reminder cadence, renewal quote, renewal payment, lapse/revival | Missing. `LAPSED` is modelled as a state with nothing that drives it |
| **Service request / ticketing** with SLAs and ownership | Missing |
| **Customer 360 / portfolio view** | Requires Customer BFF (#1), deferred to R1 |
| **Insurer status sync** for issued policies (the policy changes at the insurer, not with us) | Missing — status polling today is proposal-scoped, not policy-scoped |
| **Commission and payout** | R1/R2+, not designed |
| **DWH push** | BRD §14, R1 |

### Recommended architectural answer for the review

1. **Do not extend Journey Orchestration into servicing.** The sale journey is terminal at `SOLD`.
   Servicing is a **second, long-running lifecycle over the Policy aggregate** with a different owner,
   different SLAs and a different actor set. Merging them recreates exactly the "one god state
   machine" failure that INV-JRN-02 exists to prevent.
2. **Add Policy Servicing as a new bounded context (#20)** at R2, upstream-consuming Policy (#13) and
   using the same Integration Hub seam. Policy & Issuance keeps issuance; servicing gets its own
   reason to change (servicing policy and insurer servicing contracts change independently of how a
   sale completes).
3. **Renewal is a journey, and it can reuse the journey engine** — a renewal is quote → payment →
   policy update, with no suitability gate and a different consent basis. That is the strongest
   argument for the LOB-parametrised, data-driven journey definition recommended in §6: a renewal
   becomes another journey definition, not another engine.
4. **The document store (§10) must land before servicing**, not with it — servicing generates
   endorsement documents, revival letters and grievance correspondence from day one.

### Naming map (for the review conversation)

| Their term | Our term |
|---|---|
| BMS / booking management | **Policy & Issuance (#13)** for the issuance half; the servicing half does not exist yet |
| Booking | Proposal → Policy conversion |
| Lead middleware | **Lead (#5)** + **Journey Orchestration (#9)** — two contexts, deliberately |
| DIY journey | Self-service journey (`Journey.channel = self-service`) |
| Telecaller | Not modelled — would be a workforce principal with an ABAC policy, on the same assisted journey |
| Requirement / pending docs | `BankApplicationStatus.UW_REQUIREMENTS`; Proposal `REQUIREMENTS_PENDING` |
| Aggregator (1SB) | **Integration Hub (#14)** routing to the **1SB Adapter (#15)** |
| Quote cache | Not implemented; provider metadata caches exist (masters, schemas) |

---

## 12. The five things worth taking back to the review as genuine gaps

Everything else above is either built, designed-and-deferred, or a naming difference. These five are not:

| # | Gap | Why it matters now |
|---|---|---|
| G-1 | **Quote caching has no design** — no key, TTL, invalidation rule or owning service | NFR-LAT-03 (p95 < 5 s) depends on it, and a wrong answer here creates a financial break via INV-PAY-03, not just a slow page |
| G-2 | **Telecaller / call-centre actor does not exist** in the role model, authority matrix or any journey | If this operating model is real, it changes WS-2's role design and adds a third-party DPDP processor question. Cheap now, expensive after identity is built |
| G-3 | **No document store, no requirement-fetch capability** | Blocks any LOB with medicals or inspection — i.e. everything after Term |
| G-4 | **Retention purge, legal hold, and the DPDP position on abandoned quotes** are unimplemented and partly undecided | We implement the keeping half of a two-sided obligation; the horizons themselves are still OPEN-I4 |
| G-5 | **LOB isolation is compile-time only** — no per-LOB kill switch, routing config or bulkhead | This is precisely the question the review asked; the answer needs the Integration Hub, which is W1 in the R0 build order and not yet built |

Each of these is a triage input, not a change. None of them was actioned in producing this document.

---

**Drafted:** 2026-08-18 · analysis in response to internal review questions on `docs/hdl.svg`
**Authority:** none — this document records what the repository already says, and where it says nothing.
