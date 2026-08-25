# WS-3 — Architecture Justification & Review Answers

**Persona:** Mahesh — Principal Insurance Platform Architect (Board 1)  
**Audience:** Architecture / Product / Engineering / Data / Security reviewers preparing to defend the platform shape  
**Status:** AI-DRAFTED · mandatory human Architecture signature outstanding  
**Companion artefacts:**
- Visual HLD (R0 slice): [`diagrams/WS3-R0-HLD.svg`](./diagrams/WS3-R0-HLD.svg) · PNG sibling  
- Visual HLD (full platform · release-coloured): [`diagrams/WS3-PLATFORM-HLD-RELEASE-MAP.svg`](./diagrams/WS3-PLATFORM-HLD-RELEASE-MAP.svg)  
- Normative R0 solution: [`03-solution-architecture-r0.md`](./03-solution-architecture-r0.md)  
- Target catalogue: [`../architecture-review/02-target-microservices-architecture.md`](../architecture-review/02-target-microservices-architecture.md)  
- Data engines: [`../architecture-review/05-data-architecture.md`](../architecture-review/05-data-architecture.md)  
- Integration roadmap: [`../../au-bank-insurance-platform/knowledge-base/08-integration-strategy.md`](../../au-bank-insurance-platform/knowledge-base/08-integration-strategy.md)  
- Product charter: [`../../governance/workstreams/WS-3-PLATFORM-CHARTER.md`](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md)

> **How to use this in a review.** Start with §1 (what we are building toward). Use §2 when asked
> "why so many services?". Use §3 for the per-service catalogue (functions / APIs / logic). Use §4
> for "why not merge X and Y?". Use §5–§7 for datastore, caching and direct-insurer questions.
> Use §8 and the release-map diagram when someone says "this only shows R0".

---

## 1. What we are achieving with this architecture

### 1.1 The business problem, restated as an architecture obligation

AU Bank holds IRDAI Composite Corporate Agent licence CA0515. Today, much of the sale leaves the
bank's visibility at redirect. The platform exists so the bank **retains control and evidence across
the whole sale**: lead → need analysis → suitability → consent → quote → proposal → payment on the
customer's device → issued & reconciled policy → audit trail.

That obligation forces architecture properties that a single "insurance microservice" cannot
satisfy:

| Obligation | Architecture property it forces |
|---|---|
| Suitability is a hard gate before quote | A Suitability context whose decision Quotation must verify — not a UI checkbox |
| Consent is evidenced, OTP-verified, immutable | A Consent context with append-only storage and its own retention/CMK posture |
| Payment never runs on an RM device | Payment owns link issuance; Notification delivers to the customer; no API path can issue a payment session into an RM session |
| "Sold" = issued + reconciled + auditable | Policy issues only against `RECONCILED` payment; Journey cannot reach `SOLD` until Audit confirms |
| 1SB is replaceable | Quotation/Proposal/Payment/Policy never call 1SB; they call the Integration Hub in bank-canonical language |
| Attribution is bank-owned | Hub injects `distributorId`; caller-supplied values are rejected |

If we collapse these into fewer deployables without preserving the **boundaries**, we keep the
microservice count down and lose the controls. The control is the product.

### 1.2 Three nested targets (do not confuse them)

| Horizon | What "done" means | Service count |
|---|---|---|
| **R0 (assisted Term slice)** | One RM sells one Term Life policy to one ETB customer from one Group A insurer, end to end, evidenced | ~12 services + Flutter RM app (+ WS-1 + WS-2 consumed) |
| **R1 (scale the proven journey)** | DIY + Customer BFF, Group B redirect, ULIP/Savings, richer notification/MIS/admin | Adds ~4–5 contexts and deepens existing ones |
| **Target platform (R2+)** | Full 19-context catalogue: renewals/servicing, NTB, non-life (when WS-1 Phase 5 unfreezes), direct insurer adapters, control tower | Full catalogue in [`02-target-microservices-architecture.md`](../architecture-review/02-target-microservices-architecture.md) |

R0 is a **release slice**, not a permanent architecture. The HLD you liked is the R0 cut. The
release-map diagram shows the same topology with R1 and R2+ coloured in so reviewers can see the
full platform without pretending we build it all now.

### 1.3 What success looks like architecturally

1. **Replaceability** — swap 1SB for a direct insurer without rewriting Quotation or Proposal.  
2. **Non-bypassable controls** — C1 (suitability), C2 (consent), C4 (customer-device payment) are
   enforced in services, not only in screens.  
3. **Evidence** — every regulated decision leaves an immutable audit trail.  
4. **Independent change** — a Payment reconciliation fix does not require redeploying Suitability.  
5. **Honest sequencing** — foundation (S08/S09) before Wave 1; Wave 1 before money path; money path
   before claiming "platform ready".

---

## 2. Why we need these services (the design rule)

### 2.1 The rule we actually use

> **One bounded context → one write model → one owning service.**  
> Split further only when lifecycle, scaling profile, team or release cadence genuinely diverge.  
> Merge only when two contexts share one lifecycle, one failure domain and one change cadence —
> **and** merging does not weaken a regulatory hard gate.

Source: [`02-target-microservices-architecture.md`](../architecture-review/02-target-microservices-architecture.md) § Design rule.

### 2.2 What a "service" is allowed to own

| Owns | Does not own |
|---|---|
| One canonical aggregate (or a tightly coupled pair under one SoR) | Another context's business decision |
| Its own datastore and migration history | A shared platform DB for all domains |
| Bank-canonical API | Raw 1SB / insurer wire format |
| Enforcement of its invariants | Journey stage of the whole sale |

Journey Orchestration is the deliberate exception: it owns **stage + references**, never the
authoritative quote/proposal/payment/policy decision (SC-W3-6 / INV-JRN-02).

### 2.3 Why "fewer services" is not automatically better here

Bancassurance failure modes are **cross-context**: payment authorised but issuance delayed;
suitability expired between quote and proposal; consent revoked; PG callback missing;
insurer returns `PARTIAL` quotes. Those failures need:

- clear ownership of each decision,
- compensating actions owned by Journey,
- independent deployability of the money path vs the advisory path,
- independent datastore failure domains (Consent must stay append-only even if Quotation is
  rebuilt).

A merged "Sales Service" that does suitability + consent + quote + proposal looks tidy on a slide
and fails the first compliance reconstruction: *"show me the immutable consent grant that
authorised this proposal, separate from the quote job that may have been retried three times."*

---

## 3. Service catalogue — functions, APIs, business logic

Legend for **Release**: solid R0 · deepen in R1 · R2+ only.  
API verbs are **platform-canonical** (not OpenAPI-final). Exact paths land with each service's
contract pack; this table is the review-facing inventory.

### 3.1 Edge & identity

| Service | Release | Functions / business logic | Representative APIs | Why separate |
|---|---|---|---|---|
| **#2 RM Workspace BFF** | R0 | Token-hiding session; aggregates RM screens; never holds domain decisions | `POST /session/login`, `GET /workspace/journey/{id}`, mutation proxies with `Idempotency-Key` | Flutter must not see OAuth tokens (ARCH-019); BFF is the only public app-facing surface |
| **#1 Customer BFF** | R1 | Same pattern for DIY customer app | Customer session + journey proxies | Different principal, different threat model, different app — do not overload RM BFF |
| **#3 Identity & Access (WS-2)** | R0 (workforce) / R1+ (retail) | Provider adapter + PDP (RBAC/ABAC), certification gates | `POST /auth/token-exchange` (adapter-internal); `POST /authorize` (PDP) | Fail-closed authz; IdP is never business SoR |

### 3.2 Sales & advisory

| Service | Release | Functions / business logic | Representative APIs | Why separate |
|---|---|---|---|---|
| **#4 Customer** | R0 | ETB CIF lookup, profile snapshot, prefill; never writes CBS | `GET /customers/by-cif`, `GET /customers/by-mobile`, `GET /customers/{id}/snapshot` | CBS facade + PII snapshot ownership; freshness rules differ from Lead |
| **#5 Lead** | R0 thin / R1 bulk-campaign | Create/update/assign/follow-up; duplicate & expiry rules | `POST /leads`, `PATCH /leads/{id}`, `POST /leads/{id}/assign` | Sales-management lifecycle; R0 can start from Customer lookup, Lead deepens later |
| **#6 Consent** | R0 | Versioned grants; customer-device OTP evidence; append-only | `POST /consents`, `POST /consents/{id}/otp/verify`, `GET /consents/{id}` | Regulatory evidence store; different retention/CMK; must fail closed for proposal |
| **#7 Suitability** | R0 | Need analysis; product eligibility; assessment id with TTL; PDF evidence | `POST /suitability/assessments`, `GET /suitability/assessments/{id}`, `GET …/eligible-products` | Hard gate C1 — must be independently enforceable and auditable |
| **#8 Product Catalogue** | R0 Term-matrix / R1 ULIP+Group B | Products, insurers, eligibility matrix, documents | `GET /products`, `GET /insurers`, `GET /eligibility` | Low-write / high-read; Redis read-through; Admin-owned content later |
| **#9 Journey Orchestration** | R0 | R0 state machine; saga / compensation; holds refs only | `POST /journeys`, `POST /journeys/{id}/advance`, `GET /journeys/{id}` | Without this, every BFF reinvents the sale spine (ARCH-005) |
| **#10 Quotation** | R0 | Create quote, poll offers, select offer, enforce suitability gate | `POST /quotes`, `GET /quotes/{id}`, `POST /quotes/{id}/select` | Bursty fan-out / poll workload; short-lived jobs — different lifecycle from Proposal |
| **#11 Proposal & UW** | R0 thin / R1 ACR depth | Prefill, submit, UW status, requirements tracking | `POST /proposals`, `GET /proposals/{id}`, `POST /proposals/{id}/submit` | Days/weeks-long case file; documents; relational integrity |

### 3.3 Fulfilment

| Service | Release | Functions / business logic | Representative APIs | Why separate |
|---|---|---|---|---|
| **#12 Payment** | R0 | Session create (customer device), callback ingest, reconciliation, `UNCERTAIN` handling | `POST /payments`, `POST /payments/callbacks/pg`, `POST /payments/reconcile` | Money path + RBI device isolation; dedicated CMK; no degraded mode |
| **#13 Policy & Issuance** | R0 visibility / R1 servicing views | Confirm issuance, store policy refs/PDFs, dispute states | `GET /policies/{id}`, `POST /policies/{id}/confirm`, document URLs | "Sold" definition lives here; must not issue against unreconciled payment |

### 3.4 Integration (supplier)

| Service | Release | Functions / business logic | Representative APIs | Why separate |
|---|---|---|---|---|
| **#14 Integration Hub** | R0 | Routing policy; inject attribution; per-provider bulkhead; canonical↔adapter handoff | Internal: `POST /provider/quotes`, `…/proposals`, `…/status` | Adding a provider must not touch Quotation/Proposal code (SC-W3-5) |
| **#15 1SB Adapter (WS-1)** | R0 (exists) | 1SB wire protocol, job store, raw payload archive | Existing integration APIs behind Hub | Provider vocabulary terminates in `adapter.onesb.*` |
| **Direct Insurer Adapter(s)** | R2+ (Phase B/C) | Per-insurer ACL; own caching where insurer has no middleware cache | Same Hub contract as 1SB | Proves replaceability; never called by domain services |

### 3.5 Cross-cutting

| Service | Release | Functions / business logic | Representative APIs | Why separate |
|---|---|---|---|---|
| **#16 Audit & Compliance** | R0 | Append-only evidence; reconstruct consent/suitability/attribution per txn | `POST /audit-events` (ingest), `GET /audit-events?journeyId=` | Library shapes events; service stores and queries them immutably |
| **#17 Notification** | R0 OTP+pay-link / R1 breadth | SMS/email delivery; never blocks journey | Consume events; delivery log API | Failure domain isolated from money/advisory path |
| **#18 Reporting & MIS** | R0 pilot funnel / R1 full MIS | Funnel metrics, sold KPI, later DWH | Read models / exports — never sync on sale path | Analytics must not back-pressure a customer journey |
| **#19 Administration & Config** | R0 as versioned artefacts / R1 admin UI | Rule packs, flags, catalogue ops | Config pull APIs + later UI | Config-first absorbs compliance answers without re-architecture (ARCH-010) |

---

## 4. Why we are not merging "a couple of services"

These are the merge proposals that come up in every review, with the rejection criteria.

| Proposed merge | Temptation | Why we reject it (for now) | Revisit only if |
|---|---|---|---|
| **Consent + Suitability** | "Both are pre-quote compliance" | Different legal artefacts, retention, OTP evidence vs rule evaluation; different CMK/access; one failing must not corrupt the other | Never for R0/R1 — controls C1 and C2 are independently reconstructable |
| **Quotation + Proposal** | "Both are sales" | Quote = short-lived bursty fan-out/poll; Proposal = long-lived UW case with documents. Different datastore fit, scaling, failure modes | Evidence that both share one lifecycle *and* one team for 12+ months |
| **Quotation + Integration Hub** | "Quote always goes to 1SB" | Hub is the replaceability seam; baking 1SB into Quotation re-creates the Beema redirect problem inside our own code | Never — violates SC-W3-5 |
| **Hub + 1SB Adapter** | "Only one provider today" | Phase B adds Direct adapters behind the same Hub contract; merge makes every new insurer a Quotation change | Second provider commitment arrives — still keep Hub; add adapter |
| **Payment + Policy** | "Both are fulfilment" | Payment reconciliation can be `UNCERTAIN` while policy must not issue; money vs contract lifecycle | Strong evidence of identical ops ownership *and* no reconciliation breaks |
| **Journey + BFF** | "Orchestration is just API glue" | Then every channel reimplements the saga; DIY/hybrid become N copies of state | Never — ARCH-005 |
| **Audit as library-only** | "We already have bank-common-audit" | Library defines shape; service provides durable immutable query store fed by outbox | Never for regulated reconstruction |
| **Customer + Lead** | "Both are CRM-ish" | Customer snapshot is CIF/CBS-sourced; Lead is sales workflow. Different SoR and retention | R2+ only with Product+Data joint ADR |
| **All R0 domains in one modular monolith** | "We are a small team" | Acceptable *deployment* tactic (one repo, few pipelines) is not the same as one *write model*. Modular monolith may be an interim packaging choice; boundaries and DBs must still exist | Packaging ADR with Mahesh+Amit+Shivanshi; does **not** erase context boundaries |

### 4.1 What we *are* willing to keep thin

Thin is not the same as merged:

- **Lead** can stay thin in R0 (journey may start from Customer lookup).  
- **Proposal** can stay thin (status tracking before full ACR depth).  
- **Admin** in R0 is versioned config artefacts, not a UI service.  
- **Notification** in R0 is OTP + payment link only.

Thin services still own their aggregate so R1 can deepen without a rewrite.

### 4.2 Modular-monolith packaging (honest option)

If delivery pressure argues for fewer **deployable units**, the honest architecture answer is:

> Keep **logical** bounded contexts, database-per-service (or schema-per-context with IAM walls),
> and ArchUnit boundaries — package multiple contexts into fewer Spring Boot processes only as an
> interim **ops** decision, with a recorded unpack plan.

That is a packaging ADR, not a domain merge. Do not present it as "we merged Consent into
Quotation."

---

## 5. Review question — why relational (and not MongoDB) for Quote and Proposal?

### 5.1 Short answer

We are **not** putting Quote and Proposal in the same engine, and we are **not** choosing MongoDB
as the default document store.

| Aggregate | Chosen engine | Why this, not MongoDB |
|---|---|---|
| **Quote / quote jobs / offers** | **DynamoDB** (+ Redis for idempotency keys) | Single-key access by `quoteId`/`jobId`, short TTL, high churn, poller-friendly — same pattern already proven in WS-1. MongoDB would add ops surface without a query pattern we need |
| **Proposal / UW case / requirements / documents metadata** | **Aurora PostgreSQL** | Multi-row relational integrity over days/weeks; foreign keys between proposal, requirements, document refs; reconciliation joins; ACID around status transitions. Document DB "flexibility" becomes a liability when UW requirements must remain consistent |

### 5.2 Why not MongoDB for Proposal specifically

1. **Integrity over flexibility.** UW cases are constraint-heavy (status machine, required docs,
   consent/suitability refs). Relational constraints catch illegal states at write time.  
2. **Bank ops familiarity.** Aurora/PostgreSQL is already the WS-1 job-store engine and the bank's
   default operational skill set; MongoDB would be a second operational class for one aggregate.  
3. **No document-tree access pattern that wins.** Proposal reads are by `proposalId`, status,
   customer/journey refs — classic relational filters + joins, not deep nested document search.  
4. **Binary documents stay in S3.** PDFs/videos are object storage regardless of DB choice; MongoDB
   GridFS is not an advantage here.  
5. **Compliance reconstruction** prefers clear tabular evidence trails with append-only siblings
   (consent/audit), not mutable nested documents.

### 5.3 Why Quote is DynamoDB (document-*ish*) but still not MongoDB

Quote jobs mirror `IntegrationJobEntity`: create → poll → complete/fail/timeout. That is a
**key-value state machine**, not an analytical document collection. DynamoDB gives TTL, single-digit
millisecond gets, and an ops model already adjacent to our AWS target. MongoDB would be a third
way to store short-lived jobs we already know how to run on DynamoDB/Aurora patterns.

### 5.4 Decision references

- Datastore table: [`05-data-architecture.md`](../architecture-review/05-data-architecture.md)  
- Database-per-service: **ARCH-004**  
- Quote/Proposal split rationale: [`02-target-microservices-architecture.md`](../architecture-review/02-target-microservices-architecture.md) "Why these boundaries"

> If a reviewer insists on MongoDB, the correct response is: **raise an ADR with Aarti** comparing
> Proposal-on-Mongo vs Aurora against integrity, ops skill, backup/PITR, and IRDAI reconstruction
> drills — do not silently swap engines in a slide.

---

## 6. Review question — quote caching (today and later)

### 6.1 Today (R0 / 1SB path)

| Layer | Caching? | Notes |
|---|---|---|
| **1SB middleware** | Yes (provider-side) | 1SB absorbs insurer fan-out latency; we benefit without owning insurer cache invalidation |
| **Quotation service** | **No response cache of insurer offers in R0** | Offers are suitability-gated, time-bounded, and commercially sensitive; serving a stale cached offer past suitability/consent TTL is a compliance defect |
| **Idempotency** | Yes (Redis or equivalent, 24h) | Prevents double-submit; **not** a quote-content cache |
| **Product Catalogue** | Yes (Redis read-through) | Low-write eligibility matrix — correct place to cache |
| **CBS customer snapshot** | Bounded freshness window only | Identity data: no unbounded stale fallback |

So the honest review line is:

> **R0 deliberately does not cache quote payloads in our platform.** We rely on 1SB's caching for
> speed, and we enforce freshness via suitability assessment TTL + quote job TTL + poll-to-recover.
> Caching quote *content* in R0 would optimize latency at the cost of selling on an expired
> suitability or consent basis.

### 6.2 Where caching logic will be added (planned seams)

| Seam | When | What may be cached | Invalidation / guard |
|---|---|---|---|
| Product Catalogue | R0 | Product & eligibility reads | Admin publish version; short TTL |
| Quotation idempotency | R0 | Request fingerprint → response snapshot | 24h; body mismatch → 409 |
| **Quote offer cache (platform-owned)** | **R1 candidate, only with Product+Compliance OK** | Normalized offers keyed by `(productId, ratingInputsHash, insurerId, assessmentId)` | Must include suitability id + expiry; never serve if assessment expired; never cross customers |
| **Direct insurer adapter cache** | **Phase B/C (direct integrations)** | Per-insurer rate/quote fragments where the insurer SLA requires it | Lives **inside the adapter**, not in Quotation; Hub contract stays canonical; TTL + insurer rate-card version |
| Journey read models | R1 | BFF-facing projections | Event-updated; not SoR |
| Reporting cubes | R1 | Analytical only | Never on the sale path |

### 6.3 Design rule for any future quote cache

1. Cache key **must** bind `suitabilityAssessmentId` and its expiry.  
2. Cache miss or expiry ⇒ **re-quote**, never silent serve.  
3. Cache lives behind Quotation or inside an adapter — **never** in the BFF.  
4. PII in cache follows Shailja's rule: prefer references over raw PAN/Aadhaar in shared Redis.  
5. Direct-insurer caching is an **adapter concern** so insurer-specific invalidation does not leak
   into domain services.

---

## 7. Future plans — direct insurer integrations (beyond 1SB)

### 7.1 Strategic stance (already ratified in product KB)

Integrations are **replaceable implementation details**. Domains and journeys stay stable.

```text
Quotation / Proposal / Payment / Policy
        │  bank-canonical only
        ▼
Integration Hub  ──►  Adapter: 1SB          (Phase A — now / R0)
                 ──►  Adapter: Direct-X     (Phase B — coexist)
                 ──►  Adapter: Direct-Y…    (Phase C — expand)
```

Source: [`08-integration-strategy.md`](../../au-bank-insurance-platform/knowledge-base/08-integration-strategy.md).

### 7.2 What changes when we go direct — and what must not

| Changes (adapter + Hub routing) | Must not change |
|---|---|
| New `adapter.<insurer>.*` module | Quotation/Proposal/Payment/Policy APIs |
| Hub `RoutingPolicy` rows (LOB/product → adapter) | Journey state machine meaning |
| Adapter-local caching, mTLS, insurer auth | Suitability/Consent hard gates |
| Raw payload archive per insurer in S3 | Bank-canonical offer/proposal model |
| Per-provider bulkheads & breakers | Attribution injection rules |

### 7.3 Why Hub-before-direct is mandatory in R0

Even though R0 only talks to 1SB, we still introduce **Integration Hub** in Wave 1 so that:

1. Domain services never compile against 1SB types (INV-ACL-01).  
2. Phase B is a routing + adapter change, not a platform rewrite.  
3. Per-provider isolation exists before a second dependency can take down all quotes.

Building "temporary" direct calls from Quotation to 1SB "until we need Hub" is how platforms become
permanently coupled to their first middleware.

### 7.4 Caching difference: 1SB vs direct

| Mode | Who caches insurer rates/quotes | Platform responsibility |
|---|---|---|
| **1SB (R0)** | 1SB | Idempotency + job state; optional later offer cache with suitability binding |
| **Direct (Phase B+)** | Often **us** (adapter-level) | Adapter cache + rate-card versioning + Hub timeouts/bulkheads; Quotation still speaks canonical |

### 7.5 Sequencing guardrails

- No direct adapter work until GATE-S08 and the S09 critical path are credible (foundation).  
- No second aggregator / multi-hub fantasy without commercial evidence (charter out-of-scope).  
- Direct adapters enter behind Hub only — never as a second parallel path from Flutter/BFF.

---

## 8. Full platform scope — R0 / R1 / R2+ (colour language)

Use this table with [`diagrams/WS3-PLATFORM-HLD-RELEASE-MAP.svg`](./diagrams/WS3-PLATFORM-HLD-RELEASE-MAP.svg).

| Colour / style on the release map | Meaning |
|---|---|
| **Solid indigo fill** | **R0** — required for the assisted Term pilot slice |
| **Solid amber fill** | **R1** — admitted after a real assisted sale; DIY, Group B, richer ops |
| **Dashed slate outline** | **R2+** — renewals, NTB, non-life, direct adapters, control tower |
| **Green identity band** | **WS-2 enabler** (workforce now; retail identity with DIY) |
| **Slate existing** | **WS-1 supplier** already in repo |

### 8.1 Context × release matrix

| # | Context | R0 | R1 | R2+ |
|---|---|---|---|---|
| 1 | Customer BFF | — | DIY surface | deepen |
| 2 | RM Workspace BFF | assisted UI | hybrid hand-off | — |
| 3 | Identity & Access | workforce (WS-2) | retail customer auth | partner depth |
| 4 | Customer | ETB CBS lookup | — | NTB / V-KYC hooks |
| 5 | Lead | thin create/resume | bulk, campaign | migrate-to-Sampath path |
| 6 | Consent | OTP grants | more templates / vernacular | — |
| 7 | Suitability | Term hard-gate | ULIP / Savings models | other LOBs |
| 8 | Product Catalogue | Life / Group A / Term | Group B + ULIP/Savings | non-life classes |
| 9 | Journey Orchestration | assisted state machine | DIY + hybrid modes | servicing journeys |
| 10 | Quotation | 1SB Term | richer compare/share; optional offer cache | multi-provider fan-out |
| 11 | Proposal & UW | thin submit/status | ACR / fraud depth | — |
| 12 | Payment | AU PG customer device | cheque / mandate breadth | — |
| 13 | Policy & Issuance | confirm + PDF visibility | servicing views | renewals |
| 14 | Integration Hub | route to 1SB | Group B redirect support | multi-adapter routing |
| 15 | 1SB Adapter | exists | harden | coexist with direct |
| — | Direct Insurer Adapter | — | spike possible | Phase B/C production |
| 16 | Audit & Compliance | append-only evidence | richer query/export | — |
| 17 | Notification | OTP + pay-link | welcome calling, breadth | — |
| 18 | Reporting & MIS | pilot funnel | full MIS / DWH push | executive control tower |
| 19 | Administration & Config | versioned artefacts | Admin UI | — |

### 8.2 What reviewers should *not* conclude from the R0 HLD

- That DIY is cancelled — it is **sequenced** to R1 after a proven assisted sale.  
- That Lead is abandoned — it is **thinned** for R0 honesty.  
- That direct integration is impossible — Hub exists so it is **inevitable without rewrite**.  
- That 19 contexts must be staffed now — building them before S08/S09 repeats the original error
  at larger scale (realignment plan §4).

---

## 9. One-screen answers for live review

**Q: Why so many services?**  
A: Each owns one regulatory or commercial decision with its own lifecycle and failure domain.
Fewer boxes without those boundaries deletes controls, not complexity.

**Q: Why not merge Quote and Proposal?**  
A: Burst short-lived fan-out vs weeks-long UW case — different data, scaling, and ops. See §4.

**Q: Why not MongoDB for quotes/proposals?**  
A: Quotes → DynamoDB jobs; Proposals → Aurora for relational integrity. MongoDB wins neither
access pattern. See §5.

**Q: Where is quote caching?**  
A: In R0, at 1SB — not in our Quotation payload store. Platform offer cache is an R1 candidate
behind suitability TTL; direct-insurer cache lives in the adapter in Phase B. See §6–§7.

**Q: How do we do direct insurer later?**  
A: New adapter behind Integration Hub; domain APIs unchanged. See §7.

**Q: Why does the diagram look R0-only?**  
A: Because R0 is the release we must make real first. The release-map diagram colours R1/R2+ on
the same topology. See §8.

**Q: What are we achieving?**  
A: A bank-owned, evidence-bearing, provider-replaceable distribution platform where one assisted
Term sale can complete lawfully — and the rest of the catalogue can grow without rewriting the
spine. See §1.

---

## 10. Authority and sign-off

| Role | Action |
|---|---|
| Mahesh (Architecture) | Owns this justification; human T4 signature outstanding |
| Aarti (Data) | Co-owns §5 datastore rationale; physical schema remains hers |
| Rajal (Product) | Owns R0/R1/R2 sequencing; this doc must not invent Product scope |
| Deepali (Security) | Owns trust-boundary consequences of BFF/Hub/adapter splits |
| Shivanshi (SRE) | Owns runtime implications of service count vs packaging ADR |
| Shailja (Compliance) | Owns permissibility of any future quote-content cache |

**Signed:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2)  
**signature_status:** `AI-DRAFTED — mandatory human signature outstanding`  
**Date:** 2026-08-18  
**Triage:** SUG-20260818-4c3
